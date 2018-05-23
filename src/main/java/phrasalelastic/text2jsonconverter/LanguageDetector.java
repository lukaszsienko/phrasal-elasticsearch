package phrasalelastic.text2jsonconverter;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LanguageDetector {

    private String profilesDirectoryPath;
    private Detector detector;

    public LanguageDetector() throws Exception {
        profilesDirectoryPath = getDirectoryForProfiles();
        DetectorFactory.loadProfile(profilesDirectoryPath);
        detector = DetectorFactory.create();
    }

    private String getDirectoryForProfiles() throws Exception {
        String tempDir = getTempDirectory();
        extractDirectoryFromJarToLocalDisc("/language_profiles", tempDir);
        return tempDir + "/language_profiles";
    }

    private String getTempDirectory() throws Exception {
        Path directory = Files.createTempDirectory("phrasal-elastic");
        File f = directory.toFile();
        f.deleteOnExit();
        return f.getCanonicalPath();
    }

    public enum Language {
        POLISH("pl"),
        ENGLISH("en"),
        OTHER("x");

        private String languageCode;

        Language(String languageCode) {
            this.languageCode = languageCode;
        }

        public static Language getLanguageByLangCode(String langCode) {
            List<Language> allLanguages = Arrays.asList(Language.values());
            for (Language lang : allLanguages) {
                if (lang.languageCode.equals(langCode)) {
                    return lang;
                }
            }
            return Language.OTHER;
        }
    }

    public Language detectLanguage(String input) throws LangDetectException {
        detector.append(input);
        String codeOfDetectedLanguage = detector.detect();
        return Language.getLanguageByLangCode(codeOfDetectedLanguage);
    }

    private void extractDirectoryFromJarToLocalDisc(String folderPath, String outputPath) throws IOException {
        String directoryName = folderPath.startsWith("/") ? folderPath.substring(1, folderPath.length()) : folderPath;

        String pathToJar = LanguageDetector.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPathToJar = URLDecoder.decode(pathToJar, "UTF-8");
        JarFile jarFile = new JarFile(decodedPathToJar);

        URI uriToJar = URI.create("jar:file:"+decodedPathToJar);
        Map<String, String> env = new HashMap<>();
        FileSystem fs;
        try {
            fs = FileSystems.newFileSystem(uriToJar, env);
        } catch (FileSystemAlreadyExistsException exp) {
            fs = FileSystems.getFileSystem(uriToJar);
        }

        JarEntry entry;
        for (Enumeration<JarEntry> enumEntry = jarFile.entries(); enumEntry.hasMoreElements(); ) {
            entry = enumEntry.nextElement();
            if (entry.getName().startsWith(directoryName)) {
                Path pathToResourceInsideJar = fs.getPath("/"+entry.getName());

                File outputFile = new File(outputPath, entry.getName());
                if (!outputFile.exists()) {
                    outputFile.getParentFile().mkdirs();
                    outputFile = new File(outputPath, entry.getName());
                }

                if (entry.isDirectory()) {
                    continue;
                }

                Files.copy(pathToResourceInsideJar, outputFile.toPath());
            }
        }
    }
}
