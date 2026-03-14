package com.servicehub.service;

import com.servicehub.config.BusinessHoursConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.*;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
/**
 * Tests business-hour and deadline calculations.
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkingHoursCalculatorTest {

    @Mock
    private BusinessHoursConfig config;

    private WorkingHoursCalculator calculator;

    @BeforeEach
    void setUp() {
        when(config.isEnabled()).thenReturn(true);
        when(config.getOfficeStart()).thenReturn(LocalTime.of(7, 30));
        when(config.getOfficeEnd()).thenReturn(LocalTime.of(16, 30));
        when(config.getTimezone()).thenReturn(ZoneId.of("Africa/Accra"));
        when(config.getWorkingDays()).thenReturn(Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        ));
        when(config.isWorkingDay(any())).thenAnswer(invocation -> {
            DayOfWeek day = invocation.getArgument(0);
            return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        });

        calculator = new WorkingHoursCalculator(config);
    }

    @Test
    @DisplayName("Should return true for time during office hours")
    void testIsWorkingHours_DuringOfficeHours() {
        OffsetDateTime monday10am = OffsetDateTime.of(
            LocalDate.of(2026, 3, 16),
            LocalTime.of(10, 0),
            ZoneOffset.UTC
        );

        assertTrue(calculator.isWorkingHours(monday10am));
    }

    @Test
    @DisplayName("Should return false for time before office hours")
    void testIsWorkingHours_BeforeOfficeHours() {
        OffsetDateTime monday6am = OffsetDateTime.of(
            LocalDate.of(2026, 3, 16),
            LocalTime.of(6, 0),
            ZoneOffset.UTC
        );

        assertFalse(calculator.isWorkingHours(monday6am));
    }

    @Test
    @DisplayName("Should return false for time after office hours")
    void testIsWorkingHours_AfterOfficeHours() {
        OffsetDateTime monday5pm = OffsetDateTime.of(
            LocalDate.of(2026, 3, 16), // Monday
            LocalTime.of(17, 0),
            ZoneOffset.UTC
        );

        assertFalse(calculator.isWorkingHours(monday5pm));
    }

    @Test
    @DisplayName("Should return false for weekend")
    void testIsWorkingHours_Weekend() {
        OffsetDateTime saturday10am = OffsetDateTime.of(
            LocalDate.of(2026, 3, 21),
            LocalTime.of(10, 0),
            ZoneOffset.UTC
        );

        assertFalse(calculator.isWorkingHours(saturday10am));
    }

    @Test
    @DisplayName("Should return same time if already in working hours")
    void testGetNextWorkingHoursStart_AlreadyInWorkingHours() {
        OffsetDateTime monday10am = OffsetDateTime.of(
            LocalDate.of(2026, 3, 16),
            LocalTime.of(10, 0),
            ZoneOffset.UTC
        );

        OffsetDateTime result = calculator.getNextWorkingHoursStart(monday10am);
        assertEquals(monday10am, result);
    }

    @Test
    @DisplayName("Should return office start time if before office hours same day")
    void testGetNextWorkingHoursStart_BeforeOfficeHours() {
        OffsetDateTime monday6am = OffsetDateTime.of(
            LocalDate.of(2026, 3, 16),
            LocalTime.of(6, 0),
            ZoneOffset.UTC
        );

        OffsetDateTime result = calculator.getNextWorkingHoursStart(monday6am);
        
        assertEquals(LocalTime.of(7, 30), result.atZoneSameInstant(ZoneId.of("Africa/Accra")).toLocalTime());
        assertEquals(DayOfWeek.MONDAY, result.getDayOfWeek());
    }
}
