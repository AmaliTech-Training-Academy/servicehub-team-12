package com.servicehub.service;

import com.servicehub.service.impl.SlaServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/**
 * Schedules periodic checks for SLA breaches.
 */

@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private final SlaServiceImpl slaBreachService;

    @Scheduled(fixedRate = 120000)
    public void checkSlaBreaches() {
        slaBreachService.detectAndUpdateBreachStatus();
    }
}
