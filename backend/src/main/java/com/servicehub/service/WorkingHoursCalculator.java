package com.servicehub.service;

import com.servicehub.config.BusinessHoursConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;

/**
 * Service for calculating business hours and SLA deadlines considering office hours.
 * Office hours: Monday-Friday, 07:30 - 16:30 (Ghana time)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkingHoursCalculator {

    private final BusinessHoursConfig config;

    /**
     * Check if the given datetime falls within office working hours
     *
     * @param dateTime the datetime to check
     * @return true if within working hours, false otherwise
     */
    public boolean isWorkingHours(OffsetDateTime dateTime) {
        if (!config.isEnabled()) {
            return true;
        }

        ZonedDateTime zonedDateTime = dateTime.atZoneSameInstant(config.getTimezone());
        DayOfWeek dayOfWeek = zonedDateTime.getDayOfWeek();
        LocalTime time = zonedDateTime.toLocalTime();

        if (!config.isWorkingDay(dayOfWeek)) {
            return false;
        }

        return !time.isBefore(config.getOfficeStart()) && time.isBefore(config.getOfficeEnd());
    }

    /**
     * Get the next working hours start time from the given datetime.
     * If already in working hours, returns the same time.
     * If outside working hours, returns the next office opening time.
     *
     * @param dateTime the starting datetime
     * @return the next working hours start time
     */
    public OffsetDateTime getNextWorkingHoursStart(OffsetDateTime dateTime) {
        if (!config.isEnabled()) {
            return dateTime;
        }

        ZonedDateTime zonedDateTime = dateTime.atZoneSameInstant(config.getTimezone());

        if (isWorkingHours(dateTime)) {
            return dateTime;
        }

        LocalDate date = zonedDateTime.toLocalDate();
        LocalTime time = zonedDateTime.toLocalTime();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (config.isWorkingDay(dayOfWeek) && time.isBefore(config.getOfficeStart())) {
            ZonedDateTime nextStart = ZonedDateTime.of(date, config.getOfficeStart(), config.getTimezone());
            return nextStart.toOffsetDateTime();
        }

  
        LocalDate nextWorkingDate = findNextWorkingDay(date);
        ZonedDateTime nextStart = ZonedDateTime.of(nextWorkingDate, config.getOfficeStart(), config.getTimezone());
        return nextStart.toOffsetDateTime();
    }

    /**
     * Calculate the number of business hours between two datetimes.
     * Only counts hours within office working hours.
     *
     * @param start the start datetime
     * @param end   the end datetime
     * @return the number of business hours
     */
    public long calculateBusinessHoursBetween(OffsetDateTime start, OffsetDateTime end) {
        if (!config.isEnabled()) {
            return Duration.between(start, end).toHours();
        }

        if (end.isBefore(start)) {
            return 0;
        }

        ZonedDateTime current = start.atZoneSameInstant(config.getTimezone());
        ZonedDateTime endZoned = end.atZoneSameInstant(config.getTimezone());

        long totalMinutes = 0;

        OffsetDateTime workingStart = getNextWorkingHoursStart(start);
        current = workingStart.atZoneSameInstant(config.getTimezone());

        while (current.isBefore(endZoned)) {
            LocalDate currentDate = current.toLocalDate();

            if (config.isWorkingDay(currentDate.getDayOfWeek())) {
                ZonedDateTime dayStart = ZonedDateTime.of(currentDate, config.getOfficeStart(), config.getTimezone());
                ZonedDateTime dayEnd = ZonedDateTime.of(currentDate, config.getOfficeEnd(), config.getTimezone());

                ZonedDateTime effectiveStart = current.isAfter(dayStart) ? current : dayStart;
                ZonedDateTime effectiveEnd = endZoned.isBefore(dayEnd) ? endZoned : dayEnd;

                if (effectiveStart.isBefore(effectiveEnd)) {
                    long minutes = Duration.between(effectiveStart, effectiveEnd).toMinutes();
                    totalMinutes += minutes;
                }
            }

            current = current.plusDays(1).with(config.getOfficeStart());
        }

        return totalMinutes / 60;
    }

    /**
     * Add business hours to a given datetime.
     * Skips non-working hours and days.
     *
     * @param start       the starting datetime
     * @param hoursToAdd  the number of business hours to add
     * @return the resulting datetime after adding business hours
     */
    public OffsetDateTime addBusinessHours(OffsetDateTime start, long hoursToAdd) {
        if (!config.isEnabled()) {
            return start.plusHours(hoursToAdd);
        }

        if (hoursToAdd <= 0) {
            return start;
        }

        ZonedDateTime current = getNextWorkingHoursStart(start).atZoneSameInstant(config.getTimezone());
        long minutesRemaining = hoursToAdd * 60;

        while (minutesRemaining > 0) {
            LocalDate currentDate = current.toLocalDate();
            LocalTime currentTime = current.toLocalTime();

            if (!config.isWorkingDay(currentDate.getDayOfWeek())) {
                current = ZonedDateTime.of(findNextWorkingDay(currentDate), config.getOfficeStart(), config.getTimezone());
                continue;
            }

            if (currentTime.isBefore(config.getOfficeStart())) {
                current = current.with(config.getOfficeStart());
                currentTime = current.toLocalTime();
            }

            if (!currentTime.isBefore(config.getOfficeEnd())) {
                current = ZonedDateTime.of(findNextWorkingDay(currentDate), config.getOfficeStart(), config.getTimezone());
                continue;
            }

            long minutesUntilEndOfDay = Duration.between(currentTime, config.getOfficeEnd()).toMinutes();

            if (minutesRemaining <= minutesUntilEndOfDay) {
                current = current.plusMinutes(minutesRemaining);
                minutesRemaining = 0;
            } else {
                minutesRemaining -= minutesUntilEndOfDay;
                current = ZonedDateTime.of(findNextWorkingDay(currentDate), config.getOfficeStart(), config.getTimezone());
            }
        }

        return current.toOffsetDateTime();
    }

    /**
     * Find the next working day from the given date.
     * If the given date is a working day and after office hours, returns the next working day.
     *
     * @param date the starting date
     * @return the next working day
     */
    private LocalDate findNextWorkingDay(LocalDate date) {
        LocalDate nextDate = date.plusDays(1);
        
        int iterations = 0;
        while (!config.isWorkingDay(nextDate.getDayOfWeek()) && iterations < 7) {
            nextDate = nextDate.plusDays(1);
            iterations++;
        }
        
        return nextDate;
    }
}
