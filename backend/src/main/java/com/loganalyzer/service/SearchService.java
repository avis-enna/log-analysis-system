package com.loganalyzer.service;

import com.loganalyzer.dto.SearchQuery;
import com.loganalyzer.dto.SearchResult;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryRepository;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for handling log search operations.
 * Provides comprehensive search capabilities similar to Splunk.
 */
@Service
public class SearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    
    /**
     * Performs a comprehensive search based on the provided query.
     * Supports full-text search, filtering, aggregations, and highlighting.
     */
    public SearchResult search(SearchQuery query) {
        long startTime = System.currentTimeMillis();
        
        try {
            SearchRequest searchRequest = buildSearchRequest(query);
            SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
            
            SearchResult result = processSearchResponse(response, query);
            result.setSearchTimeMs(System.currentTimeMillis() - startTime);
            result.setSearchId(UUID.randomUUID().toString());
            
            logger.info("Search completed: query='{}', hits={}, time={}ms", 
                       query.getQuery(), result.getTotalHits(), result.getSearchTimeMs());
            
            return result;
            
        } catch (IOException e) {
            logger.error("Search failed for query: {}", query.getQuery(), e);
            throw new RuntimeException("Search operation failed", e);
        }
    }
    
    /**
     * Performs a quick search using Spring Data Elasticsearch.
     * Suitable for simple queries without complex aggregations.
     */
    @Cacheable(value = "searchResults", key = "#query.toString()")
    public SearchResult quickSearch(SearchQuery query) {
        long startTime = System.currentTimeMillis();
        
        Pageable pageable = createPageable(query);
        
        try {
            var page = logEntryRepository.findByMessageContaining(query.getQuery(), pageable);
            
            SearchResult result = new SearchResult(
                page.getContent(),
                page.getTotalElements(),
                query.getPage(),
                query.getSize()
            );
            
            result.setSearchTimeMs(System.currentTimeMillis() - startTime);
            result.setSearchId(UUID.randomUUID().toString());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Quick search failed for query: {}", query.getQuery(), e);
            throw new RuntimeException("Quick search operation failed", e);
        }
    }
    
    /**
     * Searches for error logs within a time range.
     */
    public SearchResult searchErrors(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        SearchQuery query = new SearchQuery("level:ERROR OR level:FATAL");
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(page);
        query.setSize(size);
        query.setIncludeStackTrace(true);
        
        return search(query);
    }
    
    /**
     * Searches for logs by application and environment.
     */
    public SearchResult searchByApplication(String application, String environment, 
                                          LocalDateTime startTime, LocalDateTime endTime, 
                                          int page, int size) {
        SearchQuery query = new SearchQuery("*");
        query.setApplications(Arrays.asList(application));
        query.setEnvironments(Arrays.asList(environment));
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(page);
        query.setSize(size);
        
        return search(query);
    }
    
    /**
     * Performs pattern-based search using wildcards or regex.
     */
    public SearchResult searchByPattern(String pattern, SearchQuery.SearchMode mode, 
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      int page, int size) {
        SearchQuery query = new SearchQuery(pattern);
        query.setSearchMode(mode);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(page);
        query.setSize(size);
        query.setHighlightMatches(true);
        
        return search(query);
    }
    
    /**
     * Gets aggregated statistics for the specified time range.
     */
    public Map<String, Object> getLogStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        SearchQuery query = new SearchQuery("*");
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setSize(0); // Only aggregations, no documents
        
        // Add aggregations
        List<SearchQuery.AggregationRequest> aggregations = Arrays.asList(
            new SearchQuery.AggregationRequest("levels", SearchQuery.AggregationRequest.AggregationType.TERMS, "level"),
            new SearchQuery.AggregationRequest("sources", SearchQuery.AggregationRequest.AggregationType.TERMS, "source"),
            new SearchQuery.AggregationRequest("hosts", SearchQuery.AggregationRequest.AggregationType.TERMS, "host"),
            new SearchQuery.AggregationRequest("timeline", SearchQuery.AggregationRequest.AggregationType.DATE_HISTOGRAM, "timestamp")
        );
        query.setAggregations(aggregations);
        
        SearchResult result = search(query);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalLogs", result.getTotalHits());
        statistics.put("timeRange", Map.of("start", startTime, "end", endTime));
        statistics.put("aggregations", result.getAggregations());
        statistics.put("searchTime", result.getSearchTimeMs());
        
        return statistics;
    }
    
    /**
     * Builds an Elasticsearch SearchRequest from a SearchQuery.
     */
    private SearchRequest buildSearchRequest(SearchQuery query) {
        SearchRequest searchRequest = new SearchRequest("logs");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        
        // Build the main query
        QueryBuilder mainQuery = buildQuery(query);
        sourceBuilder.query(mainQuery);
        
        // Set pagination
        sourceBuilder.from(query.getOffset());
        sourceBuilder.size(query.getSize());
        
        // Set timeout
        sourceBuilder.timeout(new TimeValue(30, TimeUnit.SECONDS));
        
        // Add sorting
        if (query.hasSorting()) {
            for (SearchQuery.SortField sortField : query.getSortFields()) {
                SortOrder order = sortField.getDirection() == SearchQuery.SortDirection.ASC ? 
                                SortOrder.ASC : SortOrder.DESC;
                sourceBuilder.sort(sortField.getField(), order);
            }
        } else {
            // Default sort by timestamp descending
            sourceBuilder.sort("timestamp", SortOrder.DESC);
        }
        
        // Add highlighting
        if (query.isHighlightMatches()) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("message");
            highlightBuilder.field("stackTrace");
            sourceBuilder.highlighter(highlightBuilder);
        }
        
        // Add aggregations
        if (query.hasAggregations()) {
            for (SearchQuery.AggregationRequest aggReq : query.getAggregations()) {
                AggregationBuilder aggBuilder = buildAggregation(aggReq);
                if (aggBuilder != null) {
                    sourceBuilder.aggregation(aggBuilder);
                }
            }
        }
        
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }
    
    /**
     * Builds the main query from SearchQuery parameters.
     */
    private QueryBuilder buildQuery(SearchQuery query) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Main search query
        if (query.getQuery() != null && !query.getQuery().trim().isEmpty()) {
            QueryBuilder textQuery = buildTextQuery(query);
            boolQuery.must(textQuery);
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        
        // Time range filter
        if (query.hasTimeRange()) {
            boolQuery.filter(QueryBuilders.rangeQuery("timestamp")
                .gte(query.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .lte(query.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
        
        // Level filters
        if (query.getLevels() != null && !query.getLevels().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("level.keyword", query.getLevels()));
        }
        
        // Source filters
        if (query.getSources() != null && !query.getSources().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("source.keyword", query.getSources()));
        }
        
        // Host filters
        if (query.getHosts() != null && !query.getHosts().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("host.keyword", query.getHosts()));
        }
        
        // Application filters
        if (query.getApplications() != null && !query.getApplications().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("application.keyword", query.getApplications()));
        }
        
        // Environment filters
        if (query.getEnvironments() != null && !query.getEnvironments().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("environment.keyword", query.getEnvironments()));
        }
        
        // Custom filters
        if (query.getFilters() != null) {
            for (Map.Entry<String, String> filter : query.getFilters().entrySet()) {
                boolQuery.filter(QueryBuilders.termQuery(filter.getKey() + ".keyword", filter.getValue()));
            }
        }
        
        return boolQuery;
    }
    
    /**
     * Builds the text query based on search mode.
     */
    private QueryBuilder buildTextQuery(SearchQuery query) {
        String queryText = query.getQuery();
        
        switch (query.getSearchMode()) {
            case EXACT_MATCH:
                return QueryBuilders.termQuery("message.keyword", queryText);
            
            case WILDCARD:
                return QueryBuilders.wildcardQuery("message.keyword", queryText);
            
            case REGEX:
                return QueryBuilders.regexpQuery("message.keyword", queryText);
            
            case FUZZY:
                return QueryBuilders.fuzzyQuery("message", queryText);
            
            case FULL_TEXT:
            default:
                return QueryBuilders.multiMatchQuery(queryText)
                    .field("message", 2.0f)
                    .field("logger", 1.0f)
                    .field("thread", 1.0f)
                    .field("stackTrace", 1.5f);
        }
    }
    
    /**
     * Builds aggregation from AggregationRequest.
     */
    private AggregationBuilder buildAggregation(SearchQuery.AggregationRequest aggReq) {
        switch (aggReq.getType()) {
            case TERMS:
                return AggregationBuilders.terms(aggReq.getName())
                    .field(aggReq.getField() + ".keyword")
                    .size(100);
            
            case DATE_HISTOGRAM:
                DateHistogramAggregationBuilder dateHistogram = AggregationBuilders.dateHistogram(aggReq.getName())
                    .field(aggReq.getField())
                    .calendarInterval(DateHistogramInterval.HOUR);
                return dateHistogram;
            
            case HISTOGRAM:
                return AggregationBuilders.histogram(aggReq.getName())
                    .field(aggReq.getField())
                    .interval(1);
            
            case STATS:
                return AggregationBuilders.stats(aggReq.getName())
                    .field(aggReq.getField());
            
            case CARDINALITY:
                return AggregationBuilders.cardinality(aggReq.getName())
                    .field(aggReq.getField() + ".keyword");
            
            case PERCENTILES:
                return AggregationBuilders.percentiles(aggReq.getName())
                    .field(aggReq.getField());
            
            default:
                return null;
        }
    }
    
    /**
     * Processes Elasticsearch SearchResponse into SearchResult.
     */
    private SearchResult processSearchResponse(SearchResponse response, SearchQuery query) {
        List<LogEntry> logs = Arrays.stream(response.getHits().getHits())
            .map(this::mapSearchHitToLogEntry)
            .collect(Collectors.toList());
        
        SearchResult result = new SearchResult(
            logs,
            response.getHits().getTotalHits().value,
            query.getPage(),
            query.getSize()
        );
        
        // Process aggregations
        if (response.getAggregations() != null) {
            Map<String, SearchResult.AggregationResult> aggregations = new HashMap<>();
            response.getAggregations().asMap().forEach((name, agg) -> {
                SearchResult.AggregationResult aggResult = processAggregation(name, agg);
                if (aggResult != null) {
                    aggregations.put(name, aggResult);
                }
            });
            result.setAggregations(aggregations);
        }
        
        // Process highlights
        if (query.isHighlightMatches()) {
            Map<String, List<String>> highlights = new HashMap<>();
            for (SearchHit hit : response.getHits().getHits()) {
                if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                    hit.getHighlightFields().forEach((field, highlightField) -> {
                        List<String> fragments = Arrays.stream(highlightField.getFragments())
                            .map(Object::toString)
                            .collect(Collectors.toList());
                        highlights.put(hit.getId() + "." + field, fragments);
                    });
                }
            }
            result.setHighlights(highlights);
        }
        
        // Set metadata
        SearchResult.SearchMetadata metadata = new SearchResult.SearchMetadata();
        metadata.setQuery(query.getQuery());
        metadata.setStartTime(query.getStartTime());
        metadata.setEndTime(query.getEndTime());
        metadata.setTimedOut(response.isTimedOut());
        metadata.setShardsTotal(response.getTotalShards());
        metadata.setShardsSuccessful(response.getSuccessfulShards());
        metadata.setShardsFailed(response.getFailedShards());
        result.setMetadata(metadata);
        
        return result;
    }
    
    /**
     * Maps Elasticsearch SearchHit to LogEntry.
     */
    private LogEntry mapSearchHitToLogEntry(SearchHit hit) {
        // This would typically use ObjectMapper or similar
        // For now, returning a basic mapping
        LogEntry logEntry = new LogEntry();
        logEntry.setId(hit.getId());
        
        Map<String, Object> source = hit.getSourceAsMap();
        if (source != null) {
            logEntry.setMessage((String) source.get("message"));
            logEntry.setLevel((String) source.get("level"));
            logEntry.setSource((String) source.get("source"));
            logEntry.setHost((String) source.get("host"));
            logEntry.setApplication((String) source.get("application"));
            logEntry.setEnvironment((String) source.get("environment"));
            // ... map other fields
        }
        
        return logEntry;
    }
    
    /**
     * Processes Elasticsearch aggregation results.
     */
    private SearchResult.AggregationResult processAggregation(String name, org.elasticsearch.search.aggregations.Aggregation agg) {
        SearchResult.AggregationResult result = new SearchResult.AggregationResult(name, agg.getType());
        
        if (agg instanceof Terms) {
            Terms terms = (Terms) agg;
            List<SearchResult.AggregationResult.Bucket> buckets = terms.getBuckets().stream()
                .map(bucket -> new SearchResult.AggregationResult.Bucket(bucket.getKeyAsString(), bucket.getDocCount()))
                .collect(Collectors.toList());
            result.setBuckets(buckets);
        }
        
        return result;
    }
    
    /**
     * Creates Pageable from SearchQuery.
     */
    private Pageable createPageable(SearchQuery query) {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

        if (query.hasSorting()) {
            List<Sort.Order> orders = query.getSortFields().stream()
                .map(sf -> new Sort.Order(
                    sf.getDirection() == SearchQuery.SortDirection.ASC ?
                        Sort.Direction.ASC : Sort.Direction.DESC,
                    sf.getField()))
                .collect(Collectors.toList());
            sort = Sort.by(orders);
        }

        return PageRequest.of(query.getPage() - 1, query.getSize(), sort);
    }

    /**
     * Gets field suggestions for auto-completion.
     */
    public List<String> getFieldSuggestions(String fieldName, String prefix, int limit) {
        try {
            SearchRequest searchRequest = new SearchRequest("logs");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.aggregation(
                AggregationBuilders.terms("suggestions")
                    .field(fieldName + ".keyword")
                    .includeExclude(new org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude(prefix + ".*", null))
                    .size(limit)
            );
            sourceBuilder.size(0);

            SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

            if (response.getAggregations() != null) {
                Terms terms = response.getAggregations().get("suggestions");
                return terms.getBuckets().stream()
                    .map(bucket -> bucket.getKeyAsString())
                    .collect(Collectors.toList());
            }

        } catch (IOException e) {
            logger.error("Failed to get field suggestions for field: {}", fieldName, e);
        }

        return Collections.emptyList();
    }

    /**
     * Gets available field names from the index mapping.
     */
    public List<String> getAvailableFields() {
        // This would typically query the index mapping
        // For now, returning common fields
        return Arrays.asList(
            "timestamp", "level", "message", "source", "host",
            "application", "environment", "logger", "thread",
            "stackTrace", "userId", "sessionId", "requestId",
            "httpMethod", "httpUrl", "httpStatus", "responseTime"
        );
    }
}
