package com.sbertech.testtask;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class Main {

    private static int filesFailedToScan = 0;
    private static int scannedFiles = 0;

    public static void main(String[] args) throws IOException {
        /**
         * Парсим входящие параметры на те папки, которые надо просканировать, и те, которые надо исключить из сканирования.
         */
        InputParameters params = new InputParameters(args);
        /**
         * Добавляем временную директорию, в которую будут сложены промежуточные результаты сканирования.
         */
        File file = new File("temp-scan-dir");
        file.mkdir();

        /**
         * Добавляем временную директорию в исключения, чтобы не зациклить поиск на новых файлов.
         */
        params.getExcludedFolders().add(file.getAbsolutePath());
        /**
         * Создаём таймер, который будет каждые 6 секунд выводить ".", каждую минуту "|".
         */
        Timer timer = new Timer();
        timer.schedule(new TimerForConsole('s'), 0, 6000);
        timer.schedule(new TimerForConsole('m'), 60000, 60000);

        /**
         * Для каждой включённой в поиск папки запускаем процесс поиска.
         */
        long start = System.currentTimeMillis();
        int maxThreads = 20;
        for (String includedFolder : params.getIncludedFolders()) {
            RecursiveWalk recursiveWalk = new RecursiveWalk(Paths.get(includedFolder), params);
            ForkJoinPool forkJoinPool = new ForkJoinPool(maxThreads);
            forkJoinPool.invoke(recursiveWalk);
        }
        long end = System.currentTimeMillis();



        /**
         * Создаём файловый процессор с входящими параметрами и методами для обработки встречающихся нам файлов.
         */
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        FileWriter fw = new FileWriter("scan-result-" + format.format(new Date()) + ".txt");
        FileVisitor<Path> fileProcessor = new ProcessTempFiles();

        /**
         * Запускаем ещё один процесс поиска, на этот раз - по созданным папкам.
         */
        ((ProcessTempFiles)fileProcessor).setMainFileWriter(fw);

        /**
         * Сканируем все запрашиваемые папки.
         */
        Files.walkFileTree(Paths.get("temp-scan-dir"), fileProcessor);

        /**
         * Закрываем таймер.
         */
        timer.cancel();
        System.out.println("Execution time  : " + (end - start) + " in milliseconds");

        /**
         * Выдаём пользователю информацию о количестве просканенных файлов и о файлах, которые не удалось просканировать.
         */
        System.out.println("Scanned files: " + scannedFiles);
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
            try{
                Files.walkFileTree(currentDirectory, new SimpleFileVisitor<Path>() {
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
                        currentResult.add(sb.toString());
                        Main.scannedFiles++;
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

                        if (!aDir.equals(RecursiveWalk.this.currentDirectory) && Thread.activeCount() < 50) {
                            RecursiveWalk walk = new RecursiveWalk(aDir, params);
                            walk.fork();
                            walks.add(walk);
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    /**
                     * После завершения сканирования директории создаём копию её верхней структуры во временной папке.
                     */
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if(dir.equals(RecursiveWalk.this.currentDirectory) && currentResult.size() != 0) {
                            File tempDir = new File("temp-scan-dir" + File.separator + dir.toString());
                            tempDir.mkdirs();
                            Path file = Paths.get("temp-scan-dir" + File.separator + dir.toString() + File.separator + "scan-result.txt");
                            Files.write(file, currentResult, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                            /**
                             * Освобождаем коллекцию для GC.
                             */
                            currentResult = null;
                        }
                        Objects.requireNonNull(dir);
                        if (exc != null)
                            throw exc;
                        return FileVisitResult.CONTINUE;
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }

            walks.forEach(ForkJoinTask::join);
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
