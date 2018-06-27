package phrasalelastic.text2jsonconverter;

public class Run {

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Nalezy podac 4 parametry programu: pathToInputDocumentsFolder, pathToOutputJSONdocumentsFolder, translationSystemAddress, translationSystemPortNumber");
            System.exit(0);
        }
        String pathToInputDocumentsFolder = args[0];
        String pathToOutputJSONdocumentsFolder = args[1];
        String translationSystemAddress = args[2];
        int translationSystemPortNumber = Integer.valueOf(args[3]);
        int maxNumDocumentsToConvert = 10;

        DocumentConverter dc = new DocumentConverter(pathToInputDocumentsFolder, pathToOutputJSONdocumentsFolder, translationSystemAddress, translationSystemPortNumber);
        dc.runConversionFromRawFilesToJSONTranslatedDocuments(maxNumDocumentsToConvert);
    }
}
