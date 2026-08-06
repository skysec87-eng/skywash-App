package com.skywash.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final boolean enabled;
  private final String from;

  public EmailService(
      JavaMailSender mailSender,
      @Value("${skywash.mail.enabled:true}") boolean enabled,
      @Value("${skywash.mail.from:skyWash <noreply@skywash.local>}") String from
  ) {
    this.mailSender = mailSender;
    this.enabled = enabled;
    this.from = from;
  }

  public void sendOtp(String toEmail, String code) {
    String subject = "Your skyWash login code";
    String body = """
        Your skyWash verification code is:

        %s

        It expires in 10 minutes. If you didn’t request this, you can ignore this email.
        """.formatted(code);

    if (!enabled) {
      log.info("Mail disabled — OTP for {} is {}", toEmail, code);
      return;
    }

    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setFrom(from);
    msg.setTo(toEmail);
    msg.setSubject(subject);
    msg.setText(body);
    mailSender.send(msg);
    log.info("OTP email sent to {}", toEmail);
  }
}
