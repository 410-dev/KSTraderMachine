package me.hysong.atlas.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class LanguageKit {
    private static HashMap<String, HashMap<String, String>> languageLoaded = new HashMap<>();
    private static ArrayList<String> languagePriority = new ArrayList<>();
    private static String currentLanguage = "ko-kr";

    public static void setLocale(String languageCode) {
        currentLanguage = languageCode;
    }

    public static boolean loadLanguageFromFile(String filePath) {
        String content = MFS1.readString(MFS1.virtualPath(filePath));
        if (content == null) {
            throw new RuntimeException("Unable to load language file: " + filePath);
        }
        return loadLanguageFromString(content);
    }

    public static boolean loadLanguageFromString(String content) {

        // Language file format
        // ---------------------
        // LanguageFile_Format1
        // en-us
        // CURRENT_LANGUAGE=en-us
        // // This is a comment
        // ABOUT_BUTTON_TEXT=About

        String[] lines = content.split("\n");
        if (lines.length < 2) {
            throw new RuntimeException("Invalid language data. Too short content (less than 2 lines)");
        }

        if (!lines[0].equals("LanguageFile_Format1")) {
            throw new RuntimeException("Invalid language data. Expected header \"LanguageFile_Format1\", got \"" + lines[0] + "\".");
        }

        String languageCode = lines[1];
        for (int i = 2; i < lines.length; i++) {
            if (lines[i].trim().startsWith("//") || lines[i].trim().isEmpty()) {
                continue;
            }
            String key = "";
            String value = null;
            try {
                key = lines[i].split("=")[0];
                value = lines[i].split("=", 2)[1];
            }catch (Exception e) {
                System.out.println("WARNING: Potentially empty localization: " + key + "@" + languageCode + "=" + value);
            }
            putTranslation(languageCode, key, value);
        }
        return true;
    }

    public static String getValue(String key) {
        if (languageLoaded.containsKey(currentLanguage) && languageLoaded.get(currentLanguage).containsKey(key)) {
            return languageLoaded.get(currentLanguage).get(key);
        }
        for (String languageCode : languagePriority) {
            if (languageLoaded.containsKey(languageCode) && languageLoaded.get(languageCode).containsKey(key)) {
                return languageLoaded.get(languageCode).get(key);
            }
        }
        System.err.println("WARNING: Key not found: " + key);
        return key;
    }

    public static String getValue(String key, Object ... value) {
        String result = getValue(key);
        return String.format(result, value);
    }

    public static void putTranslation(String languageCode, String key, String value) {
        if (!languageLoaded.containsKey(languageCode)) {
            languageLoaded.put(languageCode, new HashMap<>());
        }

        languageLoaded.get(languageCode).put(key, value);

        if (!languagePriority.contains(languageCode)) {
            languagePriority.add(languageCode);
        }
    }
}
