package com.servicehub.service.impl;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.service.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements Notification {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;


    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Async
    @Override
    public void sendSlaBreachNotification(String to, ServiceRequest serviceRequest) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("⚠ SLA Breached: " + serviceRequest.getTitle());

        Context context = new Context();
        context.setVariable("title", serviceRequest.getTitle());
        context.setVariable("status", serviceRequest.getStatus());
        context.setVariable("priority", serviceRequest.getPriority());
        context.setVariable("slaDeadline", serviceRequest.getSlaDeadline());

        String html = templateEngine.process("email/sla-breach", context);;

        helper.setText(html, true);

        mailSender.send(message);
    }


    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Async
    @Override
    public void sendStatusUpdate(String to, String title, RequestStatus status) {
        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Service Request Status Update");

            Context context = new Context();
            context.setVariable("title", title);
            context.setVariable("status", status);

            String html = templateEngine.process("email/status-update", context);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Email sending failed", e);
        }
    }
}
