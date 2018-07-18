package phrasalelastic.experiments.code;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Experiment1 {

    private class ResultStats {
        public int allResultCV;
        public int polishResultCV;
        public int englishResultCV;

        public ResultStats(int allResultCV, int polishResultCV, int englishResultCV) {
            this.allResultCV = allResultCV;
            this.polishResultCV = polishResultCV;
            this.englishResultCV = englishResultCV;
        }
    }

    /**
     * W indeksie o nazwie "cvbase_originals_equal" znajduje się 53286 dokumentów CV,
     * w tym połowa (26643) polskich i połowa angielskich. Indeks nie zawiera żadnych tłumaczeń.
     * Pola indeksu "cvbase_originals_equal":
     * - cv_file_name
     * - cv_lang (wartość "pl" lub "en")
     * - cv_text
     * W tym eksperymencie pobieramy kolejno oferty pracy pracujpl, najpierw polskie,
     * pozniej angielskie. W tych dwóch grupach dla kazdej oferty pobieramy morelikethis
     * i sprawdzamy ile jest polskich a ile angielskich odpowiedzi.
     */
    public static void main(String[] args) {
        Set<String> plOffers = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/polish_offers_list");
        Set<String> enOffers = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/english_offers_list");
        Set<String> plCv = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_polish_cv_cut");
        Set<String> enCv = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_english_cv");

        List<String> langDistPolishOffer = new ArrayList<>();
        for (String nextPolishOffer : plOffers) {
            //DO query
            //call method analysing result
            //add result to result list
        }

        List<String> langDistEnglishOffer = new ArrayList<>();
        for (String nextEnglishOffer : enOffers) {
            //DO query
            //call method analysing result
            //add result to result list
        }

    }

    private static Set<String> readSetFromFile(String path) {
        Set<String> set = new LinkedHashSet<>();
        try {
            set = new LinkedHashSet<>(Files.readAllLines(Paths.get(path), Charset.defaultCharset()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return set;
    }


}
