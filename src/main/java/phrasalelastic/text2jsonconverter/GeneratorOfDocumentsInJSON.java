package phrasalelastic.text2jsonconverter;

import org.json.simple.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

public class GeneratorOfDocumentsInJSON {

    public static String generateJSON(List<String> polishText, List<String> englishText) {
        String pl = (polishText == null) ? "" : polishText.stream().collect(Collectors.joining("\n"));
        String en = (englishText == null) ? "" : englishText.stream().collect(Collectors.joining("\n"));

        JSONObject jsonDoc = new JSONObject();
        jsonDoc.put("cv_pl", pl);
        jsonDoc.put("cv_en", en);


        return jsonDoc.toJSONString();
    }

}
