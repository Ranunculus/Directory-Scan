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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

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
         * ==================================================== new
         */
        long start = System.currentTimeMillis();
        int maxThreads = 20;
        RecursiveWalk recursiveWalk = new RecursiveWalk(Paths.get(params.getIncludedFolders().get(0)), params);
        ForkJoinPool forkJoinPool = new ForkJoinPool(maxThreads);

        forkJoinPool.invoke(recursiveWalk);

        long end = System.currentTimeMillis();

        System.out.println("Time : " + (end - start) + " in millis");

        /**
         * ==================================================== old
         */
//        /**
//         * Создаём файловый процессор с входящими параметрами и методами для обработки встречающихся нам файлов.
//         */
//        FileVisitor<Path> fileProcessor = new ProcessFiles(params);
//        /**
//         * Сканируем все запрашиваемые папки
//         */
//        for (String includedFolder : params.getIncludedFolders()){
//            Files.walkFileTree(Paths.get(includedFolder), fileProcessor);
//        }
//        result.addAll(((ProcessFiles)fileProcessor).getCurrentResult());
//
        /**
         * Записываем результат в файл.
         * Пришлось писать самостоятельно, так как метод Files.write насильно добавляет newLine в конце каждого элемента коллекции.
         */
        Path resultFile = Paths.get("scan-result.txt");
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        OutputStream out = Files.newOutputStream(resultFile);
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

    public static class RecursiveWalk extends RecursiveTask<TreeSet<String>> {
        private final Path currentDirectory;
        private InputParameters params;
        private List<RecursiveWalk> walks = new ArrayList<>();

        private TreeSet<String> currentResult = new TreeSet<>();

        public List<RecursiveWalk> getWalks() {
            return walks;
        }

        public void setWalks(List<RecursiveWalk> walks) {
            this.walks = walks;
        }

        public RecursiveWalk(Path currentDirectory, InputParameters params) {
            this.currentDirectory = currentDirectory;
            this.params = params;
        }

        @Override
        protected TreeSet<String> compute(){
            /**
             * Создаём файловый процессор с входящими параметрами и методами для обработки встречающихся нам файлов.
             */
            FileVisitor<Path> fileProcessor = new ProcessFiles(params);
            ((ProcessFiles)fileProcessor).setCurrentDirectory(currentDirectory);

            try{
                Files.walkFileTree(currentDirectory, new SimpleFileVisitor<Path>() {
                    /**
                     * Метод создаёт из метаданных файла строку нужного формата и записывает в коллекцию result.
                     */
                    @Override
                    public FileVisitResult visitFile(Path aFile, BasicFileAttributes aAttrs) throws IOException {
//                        System.out.println(aFile + " \t" + Thread.currentThread().getId());
//                        System.out.println(Thread.activeCount());
                        StringBuilder sb = new StringBuilder();
                        sb.append("[\nfile = ")
                                .append(aFile)
                                .append("\ndate = ")
                                .append(new SimpleDateFormat("yyyy.MM.dd").format(new Date(aFile.toFile().lastModified())))
                                .append("\nsize = ")
                                .append(aFile.toFile().length())
                                .append("]");
                        currentResult.add(sb.toString());
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
                        Main.filesFailedToScan++;
                        return FileVisitResult.CONTINUE;
                    }

                    /**
                     * Метод проверяет, можем ли мы сканировать текущую папку. Если да - продолжаем. Если нет - пропускаем все её вложенные файлы и папки.
                     */
                    @Override
                    public FileVisitResult preVisitDirectory(Path aDir, BasicFileAttributes aAttrs) throws IOException {

                        if (params.getExcludedFolders() != null && params.getExcludedFolders().contains(aDir.toString())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        if (!aDir.equals(RecursiveWalk.this.currentDirectory) && Thread.activeCount() < 20) {
//                            System.out.println("Skipping " + aDir + " with \t" + Thread.currentThread().getId());
                            System.out.println(aDir);
                            RecursiveWalk walk = new RecursiveWalk(aDir, params);
                            walk.fork();
                            walks.add(walk);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
//                        System.out.println(aDir + " \t" + Thread.currentThread());

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if(dir.equals(RecursiveWalk.this.currentDirectory)) {
//                            System.out.println(Thread.currentThread().getState());
                        }
                        Objects.requireNonNull(dir);
                        if (exc != null)
                            throw exc;
                        return FileVisitResult.CONTINUE;
                    }
                });

//                for (String includedFolder : params.getIncludedFolders()){
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            walks.forEach(ForkJoinTask::join);
            System.out.println("currentResult.size() = " + currentResult.size());
            return currentResult;

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
