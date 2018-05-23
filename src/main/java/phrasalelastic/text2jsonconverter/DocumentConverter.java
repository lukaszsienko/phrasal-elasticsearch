package phrasalelastic.text2jsonconverter;

import com.cybozu.labs.langdetect.LangDetectException;

import java.io.*;
import java.util.Arrays;
import java.util.List;

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
        List<File> filesInDocFolder = getListOfRawDocFiles();

        for (int i = 0; i < filesInDocFolder.size(); i++) {
            if (filesInDocFolder.get(i).isFile()) {
                File rawTextFile = filesInDocFolder.get(i);
                String textDocument = readFileAsString(rawTextFile);
                Language detectedLanguage = detectLanguage(textDocument, rawTextFile.getName());
                String jsonDocument =  generateJSONdocument(textDocument, detectedLanguage);
                saveDocumentInOutputDirectory(rawTextFile.getName()+".json", jsonDocument);
            }
        }
    }

    private List<File> getListOfRawDocFiles() {
        File documentsFolder = new File(pathToInputDocumentsDir);
        return Arrays.asList(documentsFolder.listFiles());
    }

    private String readFileAsString(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String nextLine;
            while ((nextLine = br.readLine()) != null) {
                sb.append(nextLine+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
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
