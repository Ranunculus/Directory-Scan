package com.sbertech.testtask;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static TreeSet<String> result = new TreeSet<>();
    public static int filesFailedToScan = 0;

    public static void main(String[] args) throws IOException {
        /**
         * Парсим входящие параметры на те папки, которые надо просканировать, и те, которые надо исключить из сканирования.
         */
        InputParameters params = new InputParameters(args);
        /**
         * Создаём таймер, который будет каждые 6 секунд выводить ".", каждую минуту "|".
         */
        Timer timer = new Timer();
        timer.schedule(new TimerForConsole('s'), 0, 6000);
        timer.schedule(new TimerForConsole('m'), 60000, 60000);

        /**
         * Создаём файловый процессор с входящими параметрами и методами для обработки встречающихся нам файлов.
         */
        FileVisitor<Path> fileProcessor = new ProcessFiles(params);
        /**
         * Сканируем все запрашиваемые папки
         */
        for (String includedFolder : params.getIncludedFolders()){
            Files.walkFileTree(Paths.get(includedFolder), fileProcessor);
        }
        result.addAll(((ProcessFiles)fileProcessor).getCurrentResult());

        /**
         * Записываем результат в файл.
         * Пришлось писать самостоятельно, так как метод Files.write насильно добавляет newLine в конце каждого элемента коллекции.
         */
        Path file = Paths.get("scan-result.txt");
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        OutputStream out = Files.newOutputStream(file);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
            for (String line : result) {
                writer.append(line);
            }
        }
        /**
         * Закрываем таймер.
         */
        timer.cancel();
        /**
         * Выдаём пользователю информацию о количестве просканенных файлов и о файлах, которые не удалось простакировать.
         */
        System.out.println("Scanned files: " + result.size());
        if (filesFailedToScan > 0) {
            System.out.println("Files failed to scan: " + filesFailedToScan);
            System.out.println("You can find all failed files and causes in scan-error-log.txt");
        }

    }

    public static class TimerForConsole extends TimerTask {

        char infoType;

        public TimerForConsole(char infoType) {
            this.infoType = infoType;
        }

        public void run() {
            switch(infoType){
                case 'm':
                    System.out.print("|");
                case 's':
                    System.out.print(".");
            }
        }
    }


}
