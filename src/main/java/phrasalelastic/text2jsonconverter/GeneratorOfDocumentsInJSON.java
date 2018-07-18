package phrasalelastic.text2jsonconverter;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import phrasalelastic.text2jsonconverter.LanguageDetector.Language;

public class GeneratorOfDocumentsInJSON {

    public static String generateJSON(String cvID, Language detectedLanguage, List<String> polishText, List<String> englishText, List<String> concepts) {
        String pl = (polishText == null) ? "" : polishText.stream().collect(Collectors.joining("\n"));
        String en = (englishText == null) ? "" : englishText.stream().collect(Collectors.joining("\n"));
        List<String> conceptsList = (concepts == null) ? new ArrayList<>() : concepts;

        JSONObject jsonDoc = new JSONObject();
        jsonDoc.put("cv_id", cvID);
        jsonDoc.put("cv_lang_original", detectedLanguage.getLanguageCode());
        jsonDoc.put("cv_pl", pl);
        jsonDoc.put("cv_en", en);
        jsonDoc.put("cv_concepts", conceptsList);

        return jsonDoc.toJSONString();
    }

}
