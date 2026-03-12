package com.servicehub.service;

import com.servicehub.service.impl.SlaServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private final SlaServiceImpl slaBreachService;

    @Scheduled(fixedRate = 1000)
    public void checkSlaBreaches() {
        slaBreachService.detectAndUpdateBreachStatus();
    }
}
