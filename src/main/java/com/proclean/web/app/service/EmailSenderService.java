package com.proclean.web.app.service;

import com.proclean.web.app.model.Usuario;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class EmailSenderService {

    private final UserService userService;
    private final AESUtil aesUtil;


    @Value("${mail.smtp.host}")
    private String smtpHost;

    @Value("${mail.smtp.port}")
    private int smtpPort;

    @Value("${mail.smtp.auth}")
    private boolean smtpAuth;

    @Value("${mail.smtp.starttls.enable}")
    private boolean smtpStartTls;

    @Value("${mail.smtp.debug}")
    private boolean smtpDebug;

    public EmailSenderService(UserService userService, AESUtil aesUtil) {
        this.userService = userService;
        this.aesUtil = aesUtil;
    }

    public void sendEmail(String username, String to, String subject, String body) throws Exception {

        Usuario usuario = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


        String email = usuario.getEmail();
        String emailPassword = aesUtil.decrypt(usuario.getEmailPassword());


        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setUsername(email);
        mailSender.setPassword(emailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTls));
        props.put("mail.debug", String.valueOf(smtpDebug));


        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(email);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);

        mailSender.send(message);
    }
}
