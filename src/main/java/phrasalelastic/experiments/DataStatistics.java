package phrasalelastic.experiments;

import com.cybozu.labs.langdetect.LangDetectException;
import phrasalelastic.text2jsonconverter.LanguageDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DataStatistics {

    private static final String pathToDataFolder = "/home/lsienko/Pobrane/cv/jo_cv/test_set/full_test_set_extended";
    private static Map<String, LanguageDetector.Language> cvLanguage = new HashMap<>();


    public static void main(String[] args) throws Exception {
        File defaultOutputDirectory = new File(pathToDataFolder);
        File[] dirTextSets = defaultOutputDirectory.listFiles();

        LanguageDetector detector = new LanguageDetector();
        for (File f: dirTextSets) {
            int allCv = 0;
            int englishCvNum = 0;
            int polishCvNum = 0;
            int otherCvNum = 0;
            File[] filesInside = f.listFiles();
            for (File ff: filesInside) {
                if (!ff.isDirectory() && !ff.getName().equals("main_JO.txt")) {
                    String text = readFileAsString(ff.toPath());
                    allCv++;
                    LanguageDetector.Language language = null;
                    try {
                        language = detector.detectLanguage(text);
                    } catch (LangDetectException e) {
                        if (text.replaceAll("\\p{C}", "").trim().isEmpty()) {
                            otherCvNum++;
                            continue;
                        } else {
                            otherCvNum++;
                            continue;
                        }
                    }
                    if (language.equals(LanguageDetector.Language.ENGLISH)) {
                        englishCvNum++;
                    } else if (language.equals(LanguageDetector.Language.POLISH)) {
                        polishCvNum++;
                    } else {
                        otherCvNum++;
                    }
                }
                if (ff.getName().equals("main_JO.txt")) {
                    String text = readFileAsString(ff.toPath());
                    LanguageDetector.Language language = null;
                    try {
                        language = detector.detectLanguage(text);
                        System.out.println(f.getName()+" main_JO.txt language is "+language.toString());
                    } catch (LangDetectException e) {

                    }
                }
            }
            System.out.println("Test set name: "+f.getName());
            System.out.println("ALL = "+allCv);
            System.out.println("ENG = "+englishCvNum);
            System.out.println("PL = "+polishCvNum);
            System.out.println("other = "+otherCvNum);
            System.out.println("\n\n\n");
        }
    }

    private static String readFileAsString(Path filePath) {
        String fileContent = null;
        try (BufferedReader r = Files.newBufferedReader(filePath, Charset.defaultCharset())) {
            fileContent = r.lines()
                    .map(line -> line+"\n")
                    .collect(Collectors.joining());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent;
    }
}
