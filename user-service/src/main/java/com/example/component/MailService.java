package com.example.component;

//component放组件，和业务没关系，只是提供组件的功能
public interface MailService {

    void sendSimpleMail(String to, String subject, String content);

}
