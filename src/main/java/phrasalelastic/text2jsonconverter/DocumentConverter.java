package phrasalelastic.text2jsonconverter;

import com.cybozu.labs.langdetect.LangDetectException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import phrasalelastic.text2jsonconverter.LanguageDetector.Language;

public class DocumentConverter {

    private final String pathToInputDocumentsDir;
    private final String pathToOutputDocumentsDir;
    private final LanguageDetector detector;
    private final Translator translator;
    private final ConvertedFilesLog convertedFilesLog;
    private final ConceptsDownloader conceptsDownloader;
    private int convertedDocsInThisRun = 0;

    public DocumentConverter(String pathToInputDocumentsFolder, String pathToOutputJSONdocumentsFolder, String translationSystemAddress, int translationSystemPortNumber) throws Exception {
        pathToInputDocumentsDir = getCanonicalPathOfSpecifiedDirectory(pathToInputDocumentsFolder);
        pathToOutputDocumentsDir = getCanonicalPathOfSpecifiedDirectory(pathToOutputJSONdocumentsFolder);
        detector = new LanguageDetector();
        translator = new Translator(translationSystemAddress, translationSystemPortNumber);
        convertedFilesLog = new ConvertedFilesLog(pathToInputDocumentsFolder);
        conceptsDownloader = new ConceptsDownloader();
    }

    private String getCanonicalPathOfSpecifiedDirectory(String pathToDirectory) throws IOException {
        File directory = new File(pathToDirectory.trim());
        if (!directory.exists()) {
            throw new FileNotFoundException("Cannot find directory at specified path: "+pathToDirectory.trim());
        }
        return directory.getCanonicalPath();
    }

    public void runConversionFromRawFilesToJSONTranslatedDocuments(final int maxNumDocumentsToConvert) {
        convertedDocsInThisRun = 0;
        try (Stream<Path> paths = Files.walk(Paths.get(pathToInputDocumentsDir))) {

            List<String> alreadyConvertedFiles = convertedFilesLog.readAlreadyConvertedFiles();
            paths.filter(Files::isRegularFile)
                    .forEach(rawTextFilePath -> {

                        String fileName = rawTextFilePath.getFileName().toString();
                        if (convertedDocsInThisRun <= maxNumDocumentsToConvert && !alreadyConvertedFiles.contains(fileName)) {

                            convertFile(rawTextFilePath, fileName);

                            convertedDocsInThisRun++;
                            convertedFilesLog.addNewConvertedFileToLog(fileName);
                        } else {
                            return;
                        }

                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void convertFile(Path rawTextFilePath, String fileName) {
        List<String> documentSentences = readDocument(rawTextFilePath);
        Language detectedLanguage = detectLanguage(documentSentences, fileName);
        List<String> detectedConcepts = conceptsDownloader.getConcepts(documentSentences, detectedLanguage);
        String jsonDocument =  generateJSONdocument(documentSentences, detectedLanguage, detectedConcepts);
        saveDocumentInOutputDirectory(fileName+".json", jsonDocument);
    }

    private List<String> readDocument(Path filePath) {
        List<String> document = new ArrayList<>();
        try {
            document = new ArrayList<>(Files.readAllLines(filePath, Charset.defaultCharset()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    private Language detectLanguage(List<String> documentSentences, String fileName) {
        if (documentSentences != null && !documentSentences.isEmpty()) {
            try {
                String input = documentSentences.stream().collect(Collectors.joining(" "));
                return detector.detectLanguage(input);
            } catch (LangDetectException e) {
                System.err.println("Cannot detect language of file: " + fileName);
                e.printStackTrace();
            }
        }
        return Language.OTHER;
    }

    private String generateJSONdocument(List<String> documentSentences, Language detectedLanguage, List<String> detectedConcepts) {
        String jsonDocument = null;
        if (detectedLanguage.equals(Language.POLISH)) {
            List<String> englishTranslation = translator.translateFromPolishToEnglish(documentSentences);
            if (!englishTranslation.isEmpty()) {
                jsonDocument = GeneratorOfDocumentsInJSON.generateJSON(detectedLanguage, documentSentences, englishTranslation, detectedConcepts, null);
            }
        } else if (detectedLanguage.equals(Language.ENGLISH)) {
            jsonDocument = GeneratorOfDocumentsInJSON.generateJSON(detectedLanguage, null, documentSentences, null, detectedConcepts);
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
