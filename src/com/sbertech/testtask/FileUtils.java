package com.sbertech.testtask;

import java.io.File;

/**
 * Created by tatianamalyutina on 20/06/16.
 */
public class FileUtils {

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files != null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
