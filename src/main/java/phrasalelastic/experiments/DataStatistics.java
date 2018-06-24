package phrasalelastic.experiments;

import com.cybozu.labs.langdetect.LangDetectException;
import phrasalelastic.text2jsonconverter.LanguageDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DataStatistics {

    private static final String pathToDataFolder = "/home/lsienko/Pobrane/cv/jo_cv/new_cv";
    private static Map<String, LanguageDetector.Language> cvLanguage = new HashMap<>();

    public static Set<String> readFile(String path) throws IOException {
        Set<String> lista = new HashSet<>();
        BufferedReader br = new BufferedReader(new FileReader(path));
        try {
            String line = br.readLine();
            while (line != null) {
                lista.add(line);
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return lista;
    }

    public static void main(String[] args) throws Exception {
        Set<String> usuniete = readFile("/home/lsienko/Pobrane/cv/usuniete");

        File defaultOutputDirectory = new File(pathToDataFolder);
        File[] filesTable = defaultOutputDirectory.listFiles();


        int allCv = 0;
        int englishCvNum = 0;
        int polishCvNum = 0;
        int otherCvNum = 0;
        LanguageDetector detector = new LanguageDetector();
        for (File f: filesTable) {

            if (usuniete.contains(f.getName())) {
                System.out.println("Lista zawiera obecny w folderze plik: "+f.getName());
            }

            String text = readFileAsString(f.toPath());
            allCv++;
            LanguageDetector.Language language = null;
            try {
                language = detector.detectLanguage(text);
            } catch (LangDetectException e) {
                if (text.replaceAll("\\p{C}", "").trim().isEmpty()) {
                    System.out.println("Warning1: "+f.getName());
                    //f.delete();
                    continue;
                } else {
                    System.out.println("Warning2: "+f.getName());
                    //Files.move(f.toPath(), Paths.get("/home/lsienko/Pobrane/cv/moved/"+f.getName()));
                    continue;
                }
            }
            if (language.equals(LanguageDetector.Language.ENGLISH)) {
                englishCvNum++;
            } else if (language.equals(LanguageDetector.Language.POLISH)) {
                polishCvNum++;
            } else {
                //System.out.println(f.getName());
                otherCvNum++;
            }
            cvLanguage.put(f.getName(), language);
        }

        System.out.println("Wszystkich cv: "+allCv);
        System.out.println("  w tym cv angielskich: "+englishCvNum);
        System.out.println("  w tym cv polskich: "+polishCvNum);
        System.out.println("  w tym cv innych: "+otherCvNum);

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
