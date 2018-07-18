package phrasalelastic.experiments.code;

import phrasalelastic.experiments.helpers.MoreLikeThis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Experiment1 {

    private static class ResultStats {
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
    public static void main(String[] args) throws FileNotFoundException {
        Set<String> plOffers = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/polish_offers_list");
        Set<String> enOffers = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/english_offers_list");
        Set<String> plCv = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_polish_cv_cut");
        Set<String> enCv = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_english_cv");
        String outputPath = "/home/lsienko/Pobrane/cv/jo_cv/experiments/1";

        MoreLikeThis moreLikeThisHelper = new MoreLikeThis();
        final int requestedResultsNum = 1000;

        List<ResultStats> langDistPolishOffer = new ArrayList<>();
        for (String nextPolishOffer : plOffers) {
            Set<String> returnedCVs = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_equal", "cv", "cv_text", nextPolishOffer, requestedResultsNum);
            ResultStats resultStats = analizeResults(returnedCVs, plCv, enCv);
            langDistPolishOffer.add(resultStats);
        }
        saveResultOnDisk(outputPath, "polishOffersLangDistr", langDistPolishOffer);

        List<ResultStats> langDistEnglishOffer = new ArrayList<>();
        for (String nextEnglishOffer : enOffers) {
            Set<String> returnedCVs = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_equal", "cv", "cv_text", nextEnglishOffer, requestedResultsNum);
            ResultStats resultStats = analizeResults(returnedCVs, plCv, enCv);
            langDistEnglishOffer.add(resultStats);
        }
        saveResultOnDisk(outputPath, "englishOffersLangDistr", langDistEnglishOffer);

        moreLikeThisHelper.closeConnection();
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

    private static ResultStats analizeResults(Set<String> returnedCVs, Set<String> plCv, Set<String> enCv) {
        int plNum = 0;
        int engNum = 0;
        for (String cvFileName : returnedCVs) {
            if (plCv.contains(cvFileName)) {
                plNum++;
            } else if (enCv.contains(cvFileName)) {
                engNum++;
            }
        }
        return new ResultStats(returnedCVs.size(), plNum, engNum);
    }

    private static void saveResultOnDisk(String dir, String filename, List<ResultStats> results) throws FileNotFoundException {
        String text = results.stream()
                .map(resultObject -> resultObject.allResultCV + "," + resultObject.polishResultCV + "," + resultObject.englishResultCV)
                .collect(Collectors.joining("\n"));
        PrintWriter out = new PrintWriter(dir + "/" + filename);
        out.write(text);
        out.close();
    }


}
