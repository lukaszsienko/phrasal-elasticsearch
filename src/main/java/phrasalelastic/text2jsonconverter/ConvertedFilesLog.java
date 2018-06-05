package phrasalelastic.text2jsonconverter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConvertedFilesLog {

    private final File alreadyConvertedFilesLog;
    private final PrintWriter logWriter;

    public ConvertedFilesLog(String pathToInputDocumentsDir) throws IOException{
        alreadyConvertedFilesLog = getLogFile(pathToInputDocumentsDir);
        logWriter = new PrintWriter(new FileOutputStream(alreadyConvertedFilesLog, true));
    }

    private File getLogFile(String pathToInputDocumentsDir) throws IOException {
        String dirName = "converted_files";
        String dirCanonicalPath = pathToInputDocumentsDir+"/"+dirName;
        File dir = new File(dirCanonicalPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String logFileName = "log_converted_files";
        String logFilePath = dir.getCanonicalPath() + "/" + logFileName;
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
        return logFile;
    }

    public List<String> readAlreadyConvertedFiles() throws IOException {
        List<String> namesOfConvertedFiles = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(alreadyConvertedFilesLog.getCanonicalPath()))) {
            String fileName;
            while ((fileName = in.readLine()) != null) {
                namesOfConvertedFiles.add(fileName);
            }
        }
        return namesOfConvertedFiles;
    }

    public void addNewConvertedFileToLog(String convertedFileName) {
        logWriter.println(convertedFileName);
        logWriter.flush();
    }
}
