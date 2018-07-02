package phrasalelastic.text2jsonconverter;

public class Run {

    public static void main(String[] args) throws Exception {
        if (args.length != 7) {
            System.out.println("Params: maxNumDocumentsToConvert, pathToInputDocumentsFolder, pathToPL2EngTranslations, pathToEng2PLTranslations, pathToOutputJSONdocumentsFolder, Pl2EngTranslationPortNumber, Eng2PLTranslationPortNumber");
            System.exit(0);
        }

        int maxNumDocumentsToConvert = Integer.valueOf(args[0]);
        String pathToInputDocumentsFolder = args[1];
        String pathToPL2EngTranslations = args[2];
        String pathToEng2PLTranslations = args[3];
        String pathToOutputJSONdocumentsFolder = args[4];
        int Pl2EngTranslationPortNumber = Integer.valueOf(args[5]);//5674;
        int Eng2PLTranslationPortNumber = Integer.valueOf(args[6]);//5556;

        Translator joshua = new Translator("localhost", Pl2EngTranslationPortNumber);
        Translator phrasal = new Translator("localhost", Eng2PLTranslationPortNumber);

        DocumentConverter dc = new DocumentConverter(pathToInputDocumentsFolder, pathToPL2EngTranslations, pathToEng2PLTranslations, pathToOutputJSONdocumentsFolder, joshua, phrasal);
        dc.runConversionFromRawFilesToJSONTranslatedDocuments(maxNumDocumentsToConvert);
    }
}
