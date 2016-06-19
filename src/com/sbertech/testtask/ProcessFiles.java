package com.sbertech.testtask;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeSet;

/**
 * Created by tatianamalyutina on 19/06/16.
 */
public class ProcessFiles extends SimpleFileVisitor<Path> implements Runnable {
    private InputParameters inputParameters;
    private TreeSet<String> currentResult = new TreeSet<>();
    //        private ThreadPoolExecutor currentExecutor;
//
    private Path currentPath;

    public ProcessFiles(InputParameters parameters) {
        this.inputParameters = parameters;
    }


    public void setCurrentPath(Path currentPath) {
        this.currentPath = currentPath;
    }

    /**
     * Метод создаёт из метаданных файла строку нужного формата и записывает в коллекцию result.
     */
    @Override
    public FileVisitResult visitFile(Path aFile, BasicFileAttributes aAttrs) throws IOException {
        System.out.println("File: " + aFile);

        StringBuilder sb = new StringBuilder();
        sb.append("[\nfile = ")
                .append(aFile)
                .append("\ndate = ")
                .append(new SimpleDateFormat("yyyy.MM.dd").format(new Date(aFile.toFile().lastModified())))
                .append("\nsize = ")
                .append(aFile.toFile().length())
                .append("]");
        this.currentResult.add(sb.toString());
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
        //todo: threadsafe
        Main.filesFailedToScan++;
        return FileVisitResult.CONTINUE;
    }

    /**
     * Метод проверяет, можем ли мы сканировать текущую папку. Если да - продолжаем. Если нет - пропускаем все её вложенные файлы и папки.
     */
    @Override
    public FileVisitResult preVisitDirectory(Path aDir, BasicFileAttributes aAttrs) throws IOException {
        System.out.println("Dir: " + aDir.toString());
        File currentDirectory = new File(aDir.toString());
        if(currentDirectory.list().length <= 0) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        if (inputParameters.getExcludedFolders() != null && inputParameters.getExcludedFolders().contains(aDir.toString())) {
            return FileVisitResult.SKIP_SUBTREE;
        }
//        //todo: create new thread if there is a space
//
        if(aDir.getParent() != null && !Main.includedRootFolders.contains(aDir.getParent().toString()) && aDir.getParent().getParent() != null && !Main.includedRootFolders.contains(aDir.getParent().getParent().toString())) {
            return FileVisitResult.CONTINUE;
        }
        if(Main.mainExecutor.getActiveCount() < Main.mainExecutor.getMaximumPoolSize() && !Main.currentlyProceededFolders.contains(aDir.toString())) {
            InputParameters folderInputParameters = new InputParameters(aDir.toString(), inputParameters.getExcludedFolders());

            FileVisitor<Path> newProcessFile = new ProcessFiles(folderInputParameters);
            ((ProcessFiles)newProcessFile).setCurrentPath(aDir);
            Main.currentlyProceededFolders.add(aDir.toString());
            Main.mainExecutor.submit((Runnable) newProcessFile);
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        System.out.println("POST : " + dir);
        System.out.println(Main.currentlyProceededFolders.size());
        Main.currentlyProceededFolders.removeIf(folder -> folder.equals(dir.toString()));
        File currentDirectory = new File(dir.toString());
        System.out.println("active : " + Main.mainExecutor.getActiveCount());
        System.out.println("max size : " + Main.mainExecutor.getMaximumPoolSize());
        System.out.println("task count : " + Main.mainExecutor.getTaskCount());
        if(currentDirectory.list().length > 0) {
            /**
             * Записываем результат в файл.
             * Пришлось писать самостоятельно, так как метод Files.write насильно добавляет newLine в конце каждого элемента коллекции.
             */
//        Path file = Paths.get("scan-result-for-" + this.currentPath + ".txt");
//        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
//        OutputStream out = Files.newOutputStream(file);
//        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
//            for (String line: this.currentResult) {
//                writer.append(line);
//            }
//        }
            File tempDir = new File("temp-scan-dir/"+this.currentPath.toString());
            tempDir.mkdirs();
            Path file = Paths.get("temp-scan-dir/"+this.currentPath.toString()+"/scan-result.txt");
            Files.write(file, this.currentResult, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            this.currentResult = null;
            if (Main.currentlyProceededFolders.isEmpty()) {
                Main.mainExecutor.shutdown();
                System.out.println("getLargestPoolSize = " + Main.mainExecutor.getLargestPoolSize());

            }
        }

        return super.postVisitDirectory(dir, exc);
    }

    @Override
    public void run() {
        try {
            Main.currentlyProceededFolders.add(inputParameters.getIncludedFolders().get(0));
            Files.walkFileTree(Paths.get(inputParameters.getIncludedFolders().get(0)), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
