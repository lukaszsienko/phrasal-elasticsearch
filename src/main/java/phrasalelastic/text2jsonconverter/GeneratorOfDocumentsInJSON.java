package phrasalelastic.text2jsonconverter;

public class GeneratorOfDocumentsInJSON {

    public static String generateJSON(String polishText, String englishText) {
        return "{\n" +
                "\r\"cv_pl\" : \""+polishText+"\",\n" +
                "\r\"cv_en\" : \""+englishText+"\"\n" +
                "}";
    }

}
