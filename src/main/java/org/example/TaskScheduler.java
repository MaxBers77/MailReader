package org.example;

import org.slf4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    MailReader mailReader;
    //дата начала работы
    String startDate;
    //дата окончания работы
    String endDate;
    //время первой ежедневной попытки прочесть почту
    String readingTime;
    //пауза перед повторной попыткой прочесть почту при неудаче (в часах)
    String repeatAfter;
    //количество повторных попыток прочесть почту при неудаче ( за текущие сутки)
    String numberOfAttempts;

    Date sD;
    Date eD;
    int nOA;
    Date actualDate;
    int countOfAttempts=0;
    int rA;

    Logger logger = org.slf4j.LoggerFactory.getLogger(TaskScheduler.class);

    public TaskScheduler(MailReader mailReader, String startDate, String endDate, String readingTime, String repeatAfter, String numberOfAttempts) {
        this.mailReader = mailReader;
        this.startDate = startDate;
        this.endDate = endDate;
        this.readingTime = readingTime;
        this.repeatAfter = repeatAfter;
        this.numberOfAttempts = numberOfAttempts;
    }

    public void startWork() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            sD = df.parse(startDate + " " + readingTime);
            actualDate=sD;
        } catch (ParseException e) {
            logger.error("Неверный формат даты начала работы или времени чтения!");
            throw new RuntimeException(e);
        }
        try {
            eD = df.parse(endDate + " " + readingTime);
        } catch (ParseException e) {
            logger.error("Неверный формат даты окончания работы или времени чтения!");
            throw new RuntimeException(e);
        }

        if (eD.before(sD)) {
            logger.error("Дата начала работы позже даты окончания!");
            throw new RuntimeException("Дата начала работы позже даты окончания!");
        }

        //Проверка корректности заданных периодов повторных попыток чтения почты
        nOA=Integer.parseInt(numberOfAttempts);
        rA=Integer.parseInt(repeatAfter);
        if (nOA*rA>=24){
            logger.error("Количество пвторных попыток чтения почты и их время превышает 24 часа!");
            throw new RuntimeException("Количество пвторных попыток чтения почты и их время превышает 24 часа!");
        }

        //Усыпляем поток на время ожидания первого срабатывания
        waitingForReading();

        Date currentActualDate=actualDate;

        //Запускаем цикл проверок почты с парметрами времени из переменных
        while (true) {
            //Пытаемся прочесть почту и сохранить вложения

            boolean isReading=mailReader.readMail();

            //Если все нормально прочлось и распаковалось
            // и период работы программы не завершен - засыпаем до readingTime следующих суток
            if (isReading && new Date().before(eD)) {
                if (countOfAttempts==0) {
                    actualDate = new Date(actualDate.getTime() + TimeUnit.DAYS.toMillis(1));
                } else {
                    actualDate=new Date(currentActualDate.getTime()+TimeUnit.DAYS.toMillis(1));
                }
                if (actualDate.after(eD)){
                    break;
                }
                currentActualDate=actualDate;
                countOfAttempts=0;
                waitingForReading();
                continue;
            }
            //Если все нормально прочлось и распаковалось
            // и период работы программы завершен - прерываем цикл ожиданий и завершаем метод
            if (isReading && (new Date().after(eD))){
                break;
            }

            //Если прочесть почту не удалось и количество попыток прочесть почту меньше numberOfAttempts
            if (!isReading && countOfAttempts<nOA){
                //Время следующей попытки через repeatAfter
                actualDate=new Date(actualDate.getTime()+TimeUnit.HOURS.toMillis(rA));
                countOfAttempts++;
                waitingForReading();
                continue;
            }
            //Если прочесть почту не удалось и количество попыток прочесть почту больше или равно numberOfAttempts
            if (!isReading && countOfAttempts>=nOA){
                //Время следующей попытки - штатное время следующих суток
                actualDate=currentActualDate;
                actualDate=new Date(actualDate.getTime()+ TimeUnit.DAYS.toMillis(1));
                countOfAttempts=0;
                //Проверяем, не закончился ли период работы программы
                if (actualDate.after(eD)){
                    break;
                }
                waitingForReading();
            }

        }

    }

    private void waitingForReading(){
        Date currentDate = new Date();
        System.out.println("работает метод ожидания, ожидаем до: "+actualDate);
        try {
            Thread.sleep(actualDate.getTime() - currentDate.getTime());
        } catch (InterruptedException e) {
            logger.error("Ошибка усыпления потока при ожидании работы");
            throw new RuntimeException(e);
        }

    }
}
