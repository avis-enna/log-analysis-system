package com.loganalyzer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Validator for ValidTimeRange annotation.
 * Validates that start time is before end time and the range is within limits.
 */
public class TimeRangeValidator implements ConstraintValidator<ValidTimeRange, Object> {
    
    private int maxDays;
    private String startTimeField;
    private String endTimeField;
    
    @Override
    public void initialize(ValidTimeRange constraintAnnotation) {
        this.maxDays = constraintAnnotation.maxDays();
        this.startTimeField = constraintAnnotation.startTimeField();
        this.endTimeField = constraintAnnotation.endTimeField();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        try {
            LocalDateTime startTime = getFieldValue(value, startTimeField);
            LocalDateTime endTime = getFieldValue(value, endTimeField);
            
            // If either is null, validation passes (let other validators handle nulls)
            if (startTime == null || endTime == null) {
                return true;
            }
            
            // Check if start time is before end time
            if (!startTime.isBefore(endTime)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Start time must be before end time"
                ).addConstraintViolation();
                return false;
            }
            
            // Check if time range is within limits
            long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);
            if (daysBetween > maxDays) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("Time range cannot exceed %d days (current: %d days)", maxDays, daysBetween)
                ).addConstraintViolation();
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            // If reflection fails, assume invalid
            return false;
        }
    }
    
    private LocalDateTime getFieldValue(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (LocalDateTime) field.get(object);
    }
}
