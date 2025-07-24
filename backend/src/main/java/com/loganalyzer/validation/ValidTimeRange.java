package com.loganalyzer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure that start time is before end time
 * and the time range is not too large for performance reasons.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeRangeValidator.class)
@Documented
public @interface ValidTimeRange {
    
    String message() default "Invalid time range: start time must be before end time and range cannot exceed 30 days";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Maximum allowed time range in days
     */
    int maxDays() default 30;
    
    /**
     * Field name for start time
     */
    String startTimeField() default "startTime";
    
    /**
     * Field name for end time
     */
    String endTimeField() default "endTime";
}
