package phrasalelastic.text2jsonconverter;

import com.cybozu.labs.langdetect.LangDetectException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import phrasalelastic.text2jsonconverter.LanguageDetector.Language;

public class DocumentConverter {

    private final String pathToInputDocumentsDir;
    private final String pathToPL2EngTranslations;
    private final String pathToEng2PLTranslations;
    private final String pathToConceptsDir;
    private final String pathToOutputDocumentsDir;
    private final LanguageDetector detector;
    private final Translator joshua;
    private final Translator phrasal;
    private final ConvertedFilesLog convertedFilesLog;
    private int convertedDocsInThisRun = 0;

    public DocumentConverter(String pathToInputDocumentsFolder, String pathToPL2EngTranslations, String pathToEng2PLTranslations, String pathToConceptsFolder, String pathToOutputJSONdocumentsFolder, Translator joshua, Translator phrasal) throws Exception {
        this.pathToInputDocumentsDir = getCanonicalPathOfSpecifiedDirectory(pathToInputDocumentsFolder);
        this.pathToPL2EngTranslations = getCanonicalPathOfSpecifiedDirectory(pathToPL2EngTranslations);
        this.pathToEng2PLTranslations = getCanonicalPathOfSpecifiedDirectory(pathToEng2PLTranslations);
        this.pathToConceptsDir = getCanonicalPathOfSpecifiedDirectory(pathToConceptsFolder);
        this.pathToOutputDocumentsDir = getCanonicalPathOfSpecifiedDirectory(pathToOutputJSONdocumentsFolder);
        this.detector = new LanguageDetector();
        this.joshua = joshua;
        this.phrasal = phrasal;
        this.convertedFilesLog = new ConvertedFilesLog(pathToInputDocumentsFolder);
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

            Set<String> alreadyConvertedFiles = convertedFilesLog.readAlreadyConvertedFiles();
            paths.filter(Files::isRegularFile)
                    .forEach(rawTextFilePath -> {

                        String fileName = rawTextFilePath.getFileName().toString();
                        if (!fileName.equals("log_converted_files") && convertedDocsInThisRun <= maxNumDocumentsToConvert && !alreadyConvertedFiles.contains(fileName)) {

                            convertFile(rawTextFilePath, fileName);

                            convertedDocsInThisRun++;
                            System.out.println(convertedDocsInThisRun);
                            convertedFilesLog.addNewConvertedFileToLog(fileName);
                        }

                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished conversion. ConvertedDocsInThisRun = "+convertedDocsInThisRun);
    }

    private void convertFile(Path rawTextFilePath, String fileName) {
        String docID = FilenameUtils.removeExtension(fileName);
        List<String> documentSentences = readDocument(rawTextFilePath);
        Language detectedLanguage = detectLanguage(documentSentences, fileName);
        String jsonDocument =  generateJSONDocument(fileName, docID, documentSentences, detectedLanguage);
        saveDocumentInOutputDirectory(fileName, jsonDocument);
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
                Language language = detector.detectLanguage(input);
                if (language.equals(Language.OTHER)) {
                    //lang detector sometimes goes crazy when doc is uppercase only
                    String lowercased = input.toLowerCase();
                    language = detector.detectLanguage(lowercased);
                    if (language.equals(Language.OTHER)) {
                        language = Language.POLISH;
                    }
                }
                return language;
            } catch (LangDetectException e) {
                System.err.println("Cannot detect language of file: " + fileName);
                e.printStackTrace();
            }
        }
        return Language.OTHER;
    }

    private String generateJSONDocument(String fileName, String cvID, List<String> documentSentences, Language detectedLanguage) {
        List<String> detectedConcepts = getConcepts(fileName);

        String jsonDocument = null;
        if (detectedLanguage.equals(Language.POLISH)) {
            File englishTranslationOfPolishDocument = new File(pathToPL2EngTranslations+"/"+fileName);
            List<String> englishTranslation = null;
            if (englishTranslationOfPolishDocument.exists()) {
                englishTranslation = readDocument(englishTranslationOfPolishDocument.toPath());
            } else {
                System.out.println("Calling joshua for english translation... file="+fileName);
                englishTranslation = joshua.translate(documentSentences);
            }

            if (englishTranslation != null && !englishTranslation.isEmpty()) {

                jsonDocument = GeneratorOfDocumentsInJSON.generateJSON(cvID, detectedLanguage, documentSentences, englishTranslation, detectedConcepts);
            } else {
                System.out.println("joshua english translation is null or empty, filename = "+fileName);
            }

        } else if (detectedLanguage.equals(Language.ENGLISH)) {
            File polishTranslationOfEnglishDocument = new File(pathToEng2PLTranslations+"/"+fileName);
            List<String> polishTranslation = null;
            if (polishTranslationOfEnglishDocument.exists()) {
                polishTranslation = readDocument(polishTranslationOfEnglishDocument.toPath());
            } else {
                System.out.println("Calling phrasal for polish translation... file="+fileName);
                polishTranslation = phrasal.translate(documentSentences);
            }

            if (polishTranslation != null && !polishTranslation.isEmpty()) {
                jsonDocument = GeneratorOfDocumentsInJSON.generateJSON(cvID, detectedLanguage, polishTranslation, documentSentences, detectedConcepts);
            } else {
                System.out.println("phrasal polish translation is null or empty, filename = "+fileName);
            }
        }
        return jsonDocument;
    }

    private List<String> getConcepts(String filename) {
        File conceptListFile = new File(pathToConceptsDir+"/"+filename);
        List<String> concepts = readDocument(conceptListFile.toPath());
        List<String> cleanList = concepts.stream().map(String::trim).filter(concept -> !concept.isEmpty()).collect(Collectors.toList());
        return cleanList;
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
