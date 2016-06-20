package com.sbertech.testtask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
public class ProcessTempFiles extends SimpleFileVisitor<Path> {

    private FileWriter mainFileWriter;

    /**
     * Каждый файл записывает в хвост главного файла с результатами.
     */
    @Override
    public FileVisitResult visitFile(Path aFile, BasicFileAttributes aAttrs) throws IOException {
        FileReader fr = new FileReader(aFile.toFile());
        int c = fr.read();
        while(c != -1) {
            mainFileWriter.write(c);
            c = fr.read();
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * После записи данных в файл удаляет папку вместе с временными файлами.
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        FileUtils.deleteFolder(dir.toFile());
        return super.postVisitDirectory(dir, exc);
    }

    public FileWriter getMainFileWriter() {
        return mainFileWriter;
    }

    public void setMainFileWriter(FileWriter mainFileWriter) {
        this.mainFileWriter = mainFileWriter;
    }


}
