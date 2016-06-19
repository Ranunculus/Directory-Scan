package com.sbertech.testtask;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    //todo: decide, what is better: HashMap and the following sorting or TreeMap
    public static TreeSet<String> result = new TreeSet<>();//321255 ms + with file written:
//    public static HashSet<String> result = new HashSet<>();//204303 ms
    public static int filesFailedToScan = 0;
    public static BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(500);
    public static ThreadPoolExecutor mainExecutor = new ThreadPoolExecutor(1, 20, 1, TimeUnit.MILLISECONDS, blockingQueue);
    public static ArrayList<String> includedRootFolders = new ArrayList<>();
    public static Set<String> currentlyProceededFolders = new HashSet<>();

    public static void main(String[] args) throws IOException {
        /**
         * Парсим входящие параметры на те папки, которые надо просканировать, и те, которые надо исключить из сканирования.
         */
        InputParameters params = new InputParameters(args);

        /**
         * Добавляем корневые папки.
         */

        includedRootFolders = params.getIncludedFolders();

        /**
         * Добавляем временную директорию, в которую будут сложены промежуточные результаты сканирования.
         */
        File file = new File("temp-scan-dir");
        file.mkdir();

        /**
         * Добавляем временную директорию в исключения, чтобы не зациклить поиск новых файлов.
         */
        params.getExcludedFolders().add(file.getAbsolutePath());
        /**
         * Создаём таймер, который будет каждые 6 секунд выводить ".", каждую минуту "|".
         */
        Timer timer = new Timer();
        timer.schedule(new TimerForConsole('s'), 0, 6000);
        timer.schedule(new TimerForConsole('m'), 60000, 60000);

        /**
         * Создаём файловый процессор с входящими параметрами и методами для обработки встречающихся нам файлов.
         */
        /**
         * Сканируем все запрашиваемые папки
         */
        for (String includedFolder : params.getIncludedFolders()){
            InputParameters folderInputParameters = new InputParameters(includedFolder, params.getExcludedFolders());

            FileVisitor<Path> fileProcessor = new ProcessFiles(folderInputParameters);

            mainExecutor.submit((Runnable) fileProcessor);
//            mainExecutor.submit(Files.walkFileTree(Paths.get(includedFolder), fileProcessor))
//            ;
        }

//        /**
//         * Записываем результат в файл.
//         * Пришлось писать самостоятельно, так как метод Files.write насильно добавляет newLine в конце каждого элемента коллекции.
//         */
//        Path file = Paths.get("scan-result.txt");
//        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
//        OutputStream out = Files.newOutputStream(file);
//        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
//            for (String line: result) {
//                writer.append(line);
//            }
//        }
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
