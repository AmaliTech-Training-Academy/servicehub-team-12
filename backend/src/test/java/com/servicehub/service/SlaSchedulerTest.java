package com.servicehub.service;

import com.servicehub.service.impl.SlaServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SlaSchedulerTest {

    @Test
    void testSchedulerCallsService() {
        SlaServiceImpl mockService = Mockito.mock(SlaServiceImpl.class);
        SlaScheduler scheduler = new SlaScheduler(mockService);

        scheduler.checkSlaBreaches();

        Mockito.verify(mockService, Mockito.times(1)).detectAndUpdateBreachStatus();
    }
}