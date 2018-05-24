package phrasalelastic.text2jsonconverter;

import com.cybozu.labs.langdetect.LangDetectException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import phrasalelastic.text2jsonconverter.LanguageDetector.Language;

public class DocumentConverter {

    private final String pathToInputDocumentsDir;
    private final String pathToOutputDocumentsDir;
    private final LanguageDetector detector;
    private final Translator translator;

    public DocumentConverter(String pathToInputDocumentsFolder, String pathToOutputJSONdocumentsFolder, String translationSystemAddress, int translationSystemPortNumber) throws Exception {
        pathToInputDocumentsDir = getCanonicalPathOfSpecifiedDirectory(pathToInputDocumentsFolder);
        pathToOutputDocumentsDir = getCanonicalPathOfSpecifiedDirectory(pathToOutputJSONdocumentsFolder);
        detector = new LanguageDetector();
        translator = new Translator(translationSystemAddress, translationSystemPortNumber);
    }

    private String getCanonicalPathOfSpecifiedDirectory(String pathToDirectory) throws IOException {
        File directory = new File(pathToDirectory.trim());
        if (!directory.exists()) {
            throw new FileNotFoundException("Cannot find directory at specified path: "+pathToDirectory.trim());
        }
        return directory.getCanonicalPath();
    }

    private void runConversionFromRawFilesToJSONTranslatedDocuments() {
        try (Stream<Path> paths = Files.walk(Paths.get(pathToInputDocumentsDir))) {
            paths.filter(Files::isRegularFile)
                    .forEach(rawTextFilePath -> {
                        String textDocument = readFileAsString(rawTextFilePath);
                        String fileName = rawTextFilePath.getFileName().toString();
                        Language detectedLanguage = detectLanguage(textDocument, fileName);
                        String jsonDocument =  generateJSONdocument(textDocument, detectedLanguage);
                        saveDocumentInOutputDirectory(fileName+".json", jsonDocument);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readFileAsString(Path filePath) {
        String fileContent = null;
        try {
            fileContent = Files.readAllLines(filePath, Charset.defaultCharset())
                    .stream()
                    .map(line -> line+"\n")
                    .collect(Collectors.joining());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent;
    }

    private Language detectLanguage(String input, String fileName) {
        if (input != null && !input.isEmpty()) {
            try {
                return detector.detectLanguage(input);
            } catch (LangDetectException e) {
                System.err.println("Cannot detect language of file: " + fileName);
                e.printStackTrace();
            }
        }
        return Language.OTHER;
    }

    private String generateJSONdocument(String textDocument, Language detectedLanguage) {
        String jsonDocument = null;
        if (detectedLanguage.equals(Language.POLISH)) {
            String englishTranslation = translator.translateFromPolishToEnglish(textDocument);
            if (!englishTranslation.isEmpty()) {
                jsonDocument = GeneratorOfDocumentsInJSON.generateJSON(textDocument, englishTranslation);
            }
        } else if (detectedLanguage.equals(Language.ENGLISH)) {
            jsonDocument = GeneratorOfDocumentsInJSON.generateJSON(null, textDocument);
        }
        return jsonDocument;
    }

    private void saveDocumentInOutputDirectory(String filename, String jsonDocument) {
        if (jsonDocument != null) {
            try {
                PrintWriter out = new PrintWriter(pathToOutputDocumentsDir + "/" + filename);
                out.write(jsonDocument);
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Warning! File: "+filename+" had to be skipped during conversion.");
        }
    }

}
