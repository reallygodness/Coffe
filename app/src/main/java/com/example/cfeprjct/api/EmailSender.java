package com.example.cfeprjct.api;


import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class EmailSender {
    // TODO: заполните своими данными SMTP
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME  = "gneyasov63@gmail.com";
    private static final String PASSWORD  = "vsws ptvv czkt unti";

    public static void send(String toEmail, String subject, String body) {
        AsyncTask.execute(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(USERNAME, PASSWORD);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(USERNAME));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject(subject);
                msg.setText(body);

                Transport.send(msg);
                Log.d("EmailSender", "Email sent to " + toEmail);
            } catch (Exception e) {
                Log.e("EmailSender", "Failed to send email", e);
            }
        });
    }
}