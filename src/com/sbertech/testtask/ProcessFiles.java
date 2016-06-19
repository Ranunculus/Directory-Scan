package com.sbertech.testtask;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeSet;

/**
 * Created by tatianamalyutina on 19/06/16.
 */
public class ProcessFiles extends SimpleFileVisitor<Path> {
    private InputParameters inputParameters;
    private TreeSet<String> currentResult = new TreeSet<>();

    public ProcessFiles(InputParameters parameters) {
        this.inputParameters = parameters;
    }

    public TreeSet<String> getCurrentResult() {
        return currentResult;
    }

    public void setCurrentResult(TreeSet<String> currentResult) {
        this.currentResult = currentResult;
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
        if (inputParameters.getExcludedFolders() != null && inputParameters.getExcludedFolders().contains(aDir.toString())) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }
}
