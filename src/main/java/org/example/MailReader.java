package org.example;


import org.slf4j.Logger;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.FlagTerm;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class MailReader {
    String IMAP_AUTH_EMAIL;
    String IMAP_AUTH_PWD ;
    String IMAP_Server;
    String IMAP_Port;
    String downloadDirectory;
    String mail_imap_ssl_enable;
    String mail_imap_ssl_protocols;
    Logger logger = org.slf4j.LoggerFactory.getLogger(MailReader.class);
    boolean hasAttachment=false;
    boolean hasZip=false;


    public MailReader(String IMAP_AUTH_EMAIL, String IMAP_AUTH_PWD, String IMAP_Server, String IMAP_Port, String downloadDirectory, String mail_imap_ssl_enable, String mail_imap_ssl_protocols) {
        this.IMAP_AUTH_EMAIL = IMAP_AUTH_EMAIL;
        this.IMAP_AUTH_PWD = IMAP_AUTH_PWD;
        this.IMAP_Server = IMAP_Server;
        this.IMAP_Port = IMAP_Port;
        this.downloadDirectory = downloadDirectory;
        this.mail_imap_ssl_enable=mail_imap_ssl_enable;
        this.mail_imap_ssl_protocols=mail_imap_ssl_protocols;
    }

    public  boolean readMail(){
        Properties props=new Properties();
        props.put("mail.debug","false");
        props.put("mail.host",IMAP_Server);
        props.put("mail.store.protocol","imap");
        props.put("mail.user",IMAP_AUTH_EMAIL);
        if (!IMAP_Port.equals("")) {
            props.put("mail.imap.port", IMAP_Port);
        }
        props.put("mail.imap.ssl.enable",mail_imap_ssl_enable);
        //Если протокол задан в файле настроек, зададим его
        if (!Objects.equals(mail_imap_ssl_protocols, "")) {
            props.put("mail.imap.ssl.protocols", mail_imap_ssl_protocols);
        }






        Session session=Session.getDefaultInstance(props,null);
        Store store;

        try {
            store = session.getStore();

        } catch (NoSuchProviderException e) {
            logger.error("Неверные настройки почтового сервера! Проверте файл пропертиз.");
            throw new RuntimeException("Неверные настройки почтового сервера! Проверте файл пропертиз.");
        }
        try {
            store.connect(IMAP_AUTH_EMAIL,IMAP_AUTH_PWD);
        } catch (MessagingException e) {
            logger.error("Неверный адрес почты или пароль! Проверте файл пропертиз.");
            throw new RuntimeException("Неверный адрес почты или пароль! Проверте файл пропертиз.");
        }
        Folder folder;
        try {
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);



            //Выбираем только непрочитанные письма
            Message[]messages=folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            logger.info("В обработке "+String.valueOf(messages.length)+" непрочитанных писем");
            if (messages.length==0){
                logger.info("Нет непрочитанных писем");
                return false;
            }
            //Читаем их и распаковываем в целевую папку
            for (Message message : messages){
                try {
                    saveAttachment(message);
                } catch (IOException e) {
                    logger.error("Ошибка при попытке сохранения вложения! Ошибка при работе с файловой системой! Аварийное завершение работы!");
                    throw new RuntimeException(e);
                }
                //Выставляем флаг прочитанного письма
                message.setFlag(Flags.Flag.SEEN, true);
                if (!hasAttachment){
                    logger.info("Обрабатываемое письмо не имеет вложений!!!");
                    folder.close(false);
                    store.close();
                    return false;
                }
                if (!hasZip){
                    folder.close(false);
                    store.close();
                    return false;
                }
                logger.info("Архив из вложения распакован");

            }

            folder.close(false);
        } catch (MessagingException e) {
            logger.error("Неверные настройки почтового сервера! Проверте файл пропертиз.");
            throw new RuntimeException("Неверные настройки почтового сервера! Проверте файл пропертиз.");
        }
        try {
            store.close();
        } catch (MessagingException e) {
            logger.error("Неверные настройки почтового сервера! Проверте файл пропертиз.");

            throw new RuntimeException("Неверные настройки почтового сервера! Проверте файл пропертиз.");
        }

        hasZip=false;



        return true;
    }

    public  void saveAttachment(Message message) throws MessagingException, IOException {
        Multipart multipart=(Multipart) message.getContent();
         hasAttachment=false;
        int numberOfParts=multipart.getCount();
        for (int partCount=0;partCount<numberOfParts;partCount++){

            MimeBodyPart part=(MimeBodyPart) multipart.getBodyPart(partCount);
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())){

                hasAttachment=true;

                part.saveFile(downloadDirectory+ File.separator+partCount+".zip");

                Path path= Paths.get(downloadDirectory+ File.separator+partCount+".zip");

                ZipFile zf;
                try {
                    zf = new ZipFile(downloadDirectory+ File.separator+partCount+".zip");
                } catch (IOException e) {
                    logger.error("Вложение не является ZIP-файлом!");
                    Files.delete(path);
                    return;
                }
                hasZip=true;
                var entries=zf.entries();
                while (entries.hasMoreElements()){
                    var entry=entries.nextElement();
                    if (entry.isDirectory()){
                        processDirectory(entry);
                    } else {
                        processFile(zf,entry);
                    }
                }
                zf.close();
                Files.delete(path);
            }

        }
    }
    private  void processDirectory(ZipEntry entry) {
        var newDirectory = downloadDirectory + entry.getName();
        System.out.println("Creating Directory: " + newDirectory);
        var directory = new File(newDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
    private  void processFile(ZipFile file,  ZipEntry entry) throws IOException {
        try (
                var is = file.getInputStream(entry);
                var bis = new BufferedInputStream(is)
        ) {
            var uncompressedFileName = downloadDirectory + "\\"+entry.getName();
            try (
                    var os = new FileOutputStream(uncompressedFileName);
                    var bos = new BufferedOutputStream(os)
            ) {
                while (bis.available() > 0) {
                    bos.write(bis.read());
                }
            }
        }
    }



}
