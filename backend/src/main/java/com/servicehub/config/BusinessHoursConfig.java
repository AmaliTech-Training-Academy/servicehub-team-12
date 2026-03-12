package com.servicehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Configuration for business hours used in SLA calculations.
 * Office hours: Monday-Friday, 07:30 - 16:30
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "servicehub.business-hours")
@Validated
public class BusinessHoursConfig {

    /**
     * Office opening time (default: 07:30)
     */
    @NotNull
    private LocalTime officeStart = LocalTime.of(7, 30);

    /**
     * Office closing time (default: 16:30)
     */
    @NotNull
    private LocalTime officeEnd = LocalTime.of(16, 30);

    /**
     * Working days (default: Monday-Friday)
     */
    @NotNull
    private Set<DayOfWeek> workingDays = Set.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    );

    /**
     * Timezone for office hours (default: System default)
     */
    @NotNull
    private ZoneId timezone = ZoneId.systemDefault();

    /**
     * Whether to use business hours for SLA calculations
     * Set to false to use simple 24/7 hour calculation (for testing/migration)
     */
    private boolean enabled = true;

    /**
     * Get the number of working hours per day
     */
    public long getWorkingHoursPerDay() {
        return java.time.Duration.between(officeStart, officeEnd).toHours();
    }

    /**
     * Check if a given day of week is a working day
     */
    public boolean isWorkingDay(DayOfWeek dayOfWeek) {
        return workingDays.contains(dayOfWeek);
    }
}
