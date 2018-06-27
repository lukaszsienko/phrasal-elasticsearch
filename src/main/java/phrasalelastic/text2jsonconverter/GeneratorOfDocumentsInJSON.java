package phrasalelastic.text2jsonconverter;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import phrasalelastic.text2jsonconverter.LanguageDetector.Language;

public class GeneratorOfDocumentsInJSON {

    public static String generateJSON(String cvID, Language detectedLanguage, List<String> polishText, List<String> englishText, List<String> polishTextConcepts, List<String> englishTextConcepts) {
        String pl = (polishText == null) ? "" : polishText.stream().collect(Collectors.joining("\n"));
        String en = (englishText == null) ? "" : englishText.stream().collect(Collectors.joining("\n"));
        List<String> plConcepts = (polishTextConcepts == null) ? new ArrayList<>() : polishTextConcepts;
        List<String> enConcepts = (englishTextConcepts == null) ? new ArrayList<>() : englishTextConcepts;

        JSONObject jsonDoc = new JSONObject();
        jsonDoc.put("cv_id", cvID);
        jsonDoc.put("cv_lang_original", detectedLanguage.getLanguageCode());
        jsonDoc.put("cv_pl", pl);
        jsonDoc.put("cv_en", en);
        jsonDoc.put("cv_pl_concepts", plConcepts);
        jsonDoc.put("cv_en_concepts", enConcepts);

        return jsonDoc.toJSONString();
    }

}
