package com.lap.hacom.order.service;

public interface EmailService {
    boolean sendEmail(String to, String subject, String body);
}
