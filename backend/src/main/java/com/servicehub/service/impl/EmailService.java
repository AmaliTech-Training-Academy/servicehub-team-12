package com.servicehub.service.impl;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.service.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService implements Notification {

    private final JavaMailSender mailSender;

    @Override
    public void sendSlaBreachNotification(String to, ServiceRequest serviceRequest) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("⚠ SLA Breached: " + serviceRequest.getTitle());

        String html = """
        <html>
        <body style="font-family: Arial, sans-serif;">
            <h2 style="color:#d9534f;">SLA Breach Notification</h2>
            <p>Hello,</p>

            <p>The following service request has <b>breached its SLA deadline</b>.</p>

            <table border="1" cellpadding="8" cellspacing="0">
                <tr>
                    <td><b>Title</b></td>
                    <td>%s</td>
                </tr>
                <tr>
                    <td><b>Status</b></td>
                    <td>%s</td>
                </tr>
                <tr>
                    <td><b>Priority</b></td>
                    <td>%s</td>
                </tr>
                <tr>
                    <td><b>SLA Deadline</b></td>
                    <td>%s</td>
                </tr>
            </table>

            <p>Please take action as soon as possible.</p>

            <p>Regards,<br>
            <b>ServiceHub System</b></p>
        </body>
        </html>
        """.formatted(
                serviceRequest.getTitle(),
                serviceRequest.getStatus(),
                serviceRequest.getPriority(),
                serviceRequest.getSlaDeadline()
        );

        helper.setText(html, true);

        mailSender.send(message);
    }

    @Override
    public void sendStatusUpdate(String to, String title, RequestStatus newStatus) {
        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Service Request Status Update");

            String html = """
                <html>
                <body style="font-family: Arial, sans-serif;">
                
                    <h2 style="color:#2c3e50;">Service Request Update</h2>
                
                    <p>Hello,</p>
                
                    <p>The status of your service request has been updated.</p>
                    
                    <table style="border-collapse: collapse;">
                        <tr>
                            <td style="padding:8px;"><b>Request Title:</b></td>
                            <td style="padding:8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding:8px;"><b>New Status:</b></td>
                            <td style="padding:8px;">%s</td>
                        </tr>
                    </table>
                    
                    <p style="margin-top:20px;">
                        You can log in to the system to view more details.
                    </p>
                    
                    <p>
                        Regards,<br>
                        <b>ServiceHub System</b>
                    </p>
                    
                </body>
                </html>
                """.formatted(title, newStatus);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send status update email", e);
        }
    }
}
