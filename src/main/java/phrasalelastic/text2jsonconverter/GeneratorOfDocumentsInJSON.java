package phrasalelastic.text2jsonconverter;

public class GeneratorOfDocumentsInJSON {

    public static String generateJSON(String polishText, String englishText) {
        String plTxt = (polishText == null) ? "" : polishText;
        String enTxt = (englishText == null) ? "" : englishText;
        return "{\n" +
                "\r\"cv_pl\" : \""+plTxt+"\",\n" +
                "\r\"cv_en\" : \""+enTxt+"\"\n" +
                "}";
    }

}
