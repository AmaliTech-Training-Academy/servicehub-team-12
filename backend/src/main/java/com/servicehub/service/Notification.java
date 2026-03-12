package com.servicehub.service;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import jakarta.mail.MessagingException;

public interface Notification {

    void sendSlaBreachNotification(String to, ServiceRequest serviceRequest) throws MessagingException;

    void sendStatusUpdate(String to, String title, RequestStatus newStatus);
}
