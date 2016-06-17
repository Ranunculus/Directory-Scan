package com.sbertech.testtask;


import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

public class Main {


    public static void main(String[] args) throws IOException {
        InputParameters params = new InputParameters(args);

//        File dir = new File(args[0]);
        FileVisitor<Path> fileProcessor = new ProcessFile(params);
        for (String includedFolder : params.getIncludedFolders()){
            Files.walkFileTree(Paths.get(includedFolder), fileProcessor);
        }

    }

    private static final class ProcessFile extends SimpleFileVisitor<Path> {
        private InputParameters inputParameters;

        public ProcessFile(InputParameters parameters) {
            this.inputParameters = parameters;
        }

        @Override
        public FileVisitResult visitFile(Path aFile, BasicFileAttributes aAttrs) throws IOException {
            System.out.println("Processing file " + aFile + " : size = " + aFile.toFile().length() + ", last modified at " + new Date(aFile.toFile().lastModified()));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path aDir, BasicFileAttributes aAttrs) throws IOException {
            System.out.println("Processing directory:" + aDir);
            if (inputParameters.getExcludedFolders().contains(aDir.toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
