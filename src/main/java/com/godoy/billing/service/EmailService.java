package com.godoy.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final MailSender mailSender;

    @Value("${notification.mail.from}")
    private String from;

    public void sendEmail(String to, String subject, String text) {
            log.info("Enviando email para {}", to);

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

            log.info("Email enviado para {}", to);
    }
}
