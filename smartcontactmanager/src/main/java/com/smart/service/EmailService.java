package com.smart.service;

import java.util.Properties;

import org.springframework.stereotype.Service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	public boolean sendEmail(String subject,String text,String to) {
		boolean flag=false;
		String from="davuluruushaswini@gmail.com";
		
		Properties properties=new Properties();
		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port","465");
		properties.put("mail.smtp.starttls.enable","true");
		properties.put("mail.smtp.auth","true");
		properties.put("mail.smtp.ssl.enable", true);
		
		
		String username="davuluruushaswini";
		String password="your password";
		
		Session session=Session.getInstance(properties,new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				// TODO Auto-generated method stub
				return new PasswordAuthentication(username,password);
			}
			
		});
		
		try {
			Message message=new MimeMessage(session);
			
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			
			message.setFrom(new InternetAddress(from));
			
			message.setSubject(subject);
			
			//message.setText(text);
			message.setContent(text, "text/html");
			
			
			Transport.send(message);
			
			flag=true;
			
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return flag;
	}
}
