package com.sbertech.testtask;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    //todo: decide, what is better: HashMap and the following sorting or TreeMap
    public static TreeSet<String> result = new TreeSet<>();//321255 ms + with file written:
//    public static HashSet<String> result = new HashSet<>();//204303 ms
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
        FileVisitor<Path> fileProcessor = new ProcessFile(params);
        /**
         * Сканируем все запрашиваемые папки
         */
        for (String includedFolder : params.getIncludedFolders()){
            Files.walkFileTree(Paths.get(includedFolder), fileProcessor);
        }

        /**
         * Записываем результат в файл.
         * Пришлось писать самостоятельно, так как метод Files.write насильно добавляет newLine в конце каждого элемента коллекции.
         */
        Path file = Paths.get("scan-result.txt");
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        OutputStream out = Files.newOutputStream(file);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
            for (String line: result) {
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

    private static final class ProcessFile extends SimpleFileVisitor<Path> {
        private InputParameters inputParameters;

        public ProcessFile(InputParameters parameters) {
            this.inputParameters = parameters;
        }

        /**
         * Метод создаёт из метаданных файла строку нужного формата и записывает в коллекцию result.
         */
        @Override
        public FileVisitResult visitFile(Path aFile, BasicFileAttributes aAttrs) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("[\nfile = ")
                .append(aFile)
                .append("\ndate = ")
                .append(new SimpleDateFormat("yyyy.MM.dd").format(new Date(aFile.toFile().lastModified())))
                .append("\nsize = ")
                .append(aFile.toFile().length())
                .append("]");
            result.add(sb.toString());
            return FileVisitResult.CONTINUE;
        }

        /**
         * Метод инкрементирует количество файлов, которые не удалось просканировать и добавляет их в executor, чтобы тот мог записать лог ошибок.
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e)
                throws IOException {
            Path errorLog = Paths.get("scan-error-log.txt");
            Files.write(errorLog, (e.toString() + "\n").getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            System.out.println(e.toString());
            filesFailedToScan++;
            return FileVisitResult.CONTINUE;
        }

        /**
         * Метод проверяет, можем ли мы сканировать текущую папку. Если да - продолжаем. Если нет - пропускаем все её вложенные файлы и папки.
         */
        @Override
        public FileVisitResult preVisitDirectory(Path aDir, BasicFileAttributes aAttrs) throws IOException {
            if (inputParameters.getExcludedFolders() != null && inputParameters.getExcludedFolders().contains(aDir.toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
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
