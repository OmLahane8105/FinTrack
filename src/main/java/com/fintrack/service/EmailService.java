package com.fintrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String fromEmail;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail.from}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.fromEmail = fromEmail;
    }

    public void sendVerificationEmail(
            String email,
            String name,
            String token
    ) {

        String verificationUrl =
                frontendUrl + "/verify-email?token=" + token;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Verify your FinTrack account");

        message.setText(
                "Hello " + name + ",\n\n" +
                        "Welcome to FinTrack!\n\n" +
                        "Please verify your email address by clicking the link below:\n\n" +
                        verificationUrl + "\n\n" +
                        "This verification link expires in 24 hours.\n\n" +
                        "If you did not create a FinTrack account, you can ignore this email.\n\n" +
                        "Regards,\n" +
                        "FinTrack"
        );

        mailSender.send(message);
    }
}