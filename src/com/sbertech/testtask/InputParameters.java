package com.sbertech.testtask;

import java.util.ArrayList;

/**
 * Created by tatianamalyutina on 17/06/16.
 */
public class InputParameters {

    private final static String EXCLUSION_KEY = "-";

    private ArrayList<String> excludedFolders;
    private ArrayList<String> includedFolders = new ArrayList<>();

    public InputParameters() {}

    public InputParameters(String[] consoleArguments) {
        boolean inclusion = true;
        boolean exlusion = false;
        for (String arg : consoleArguments) {
            if(arg.equals(EXCLUSION_KEY)){
                excludedFolders = new ArrayList<>();
                exlusion = true;
                inclusion = false;
            } else {
                if(inclusion) {
                    includedFolders.add(arg);
                }
                /**
                 * Добавляем папки для ислючения из поиска.
                 * Если папка была указана как во включении, так и в исключении - исключаем.
                 */
                if(exlusion) {
                    excludedFolders.add(arg);
                    includedFolders.removeIf(incl -> incl.equals(arg));
                }
            }
        }

    }

    public InputParameters(String s, ArrayList<String> excludedFolders) {
        this.includedFolders.add(s);
        this.excludedFolders = excludedFolders;
    }

    public ArrayList<String> getExcludedFolders() {
        return excludedFolders;
    }

    public void setExcludedFolders(ArrayList<String> excludedFolders) {
        this.excludedFolders = excludedFolders;
    }

    public ArrayList<String> getIncludedFolders() {
        return includedFolders;
    }

    public void setIncludedFolders(ArrayList<String> includedFolders) {
        this.includedFolders = includedFolders;
    }
}
