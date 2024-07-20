package org.example;


import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static void main(String[] args)  {
        Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);


        //Забираем настройки пользователя из файла C:\MailReader\mailReader.properties
        File file=new File("C:\\MailReader\\mailReader.properties");
        Properties props=new Properties();
        try {
            props.load(new FileReader(file));
        } catch (IOException e) {
            logger.error("Ошибка доступа к файлу настроек mailReader.properties");
            throw new RuntimeException("Ошибка доступа к файлу настроек mailReader.properties");
        }

        String IMAP_AUTH_PWD= props.getProperty("IMAP_AUTH_PWD");
        String IMAP_AUTH_EMAIL=props.getProperty("IMAP_AUTH_EMAIL");
        String IMAP_Server=props.getProperty("IMAP_Server");
        String IMAP_Port= props.getProperty("IMAP_Port");
        String downloadDirectory= props.getProperty("downloadDirectory");
        String mail_imap_ssl_enable= props.getProperty("mail_imap_ssl_enable");
        String mail_imap_ssl_protocols=props.getProperty("mail_imap_ssl_protocols");
        String startDate=props.getProperty("startDate");
        String endDate=props.getProperty("endDate");
        String readingTime=props.getProperty("readingTime");
        String repeatAfter=props.getProperty("repeatAfter");
        String numberOfAttempts=props.getProperty("numberOfAttempts");


        //Создаем экземпляры классов и начинаем работу
        MailReader mailReader=new MailReader(IMAP_AUTH_EMAIL,IMAP_AUTH_PWD,IMAP_Server,IMAP_Port,downloadDirectory,mail_imap_ssl_enable,mail_imap_ssl_protocols);
        TaskScheduler taskScheduler=new TaskScheduler(mailReader,startDate,endDate,readingTime,repeatAfter,numberOfAttempts);
        taskScheduler.startWork();
        logger.info("Работа программы успешно завершена");
    }


}