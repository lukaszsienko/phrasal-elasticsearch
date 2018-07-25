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

public class Experiment5 {

    private static class ResultsIntersection {
        public int plQueryResultsNum;
        public int enQueryResultsNum;
        public int intersectionNum;

        public ResultsIntersection(int plQueryResultsNum, int englishTransQueryResultsNum, int intersectionNum) {
            this.plQueryResultsNum = plQueryResultsNum;
            this.enQueryResultsNum = englishTransQueryResultsNum;
            this.intersectionNum = intersectionNum;
        }

        @Override
        public String toString() {
            return plQueryResultsNum + ","
                    + enQueryResultsNum + ","
                    + intersectionNum;
        }
    }

    private static String offersDirectory;
    private static Set<String> plOffersIds;
    private static String offersEnglishGTDir;
    private static String outputPath;

    private static MoreLikeThis moreLikeThisHelper;
    private static int requestedResultsNum;


    /**
     * W tym eksperymencie dane sa oferty dwujezyczne oraz polskie, oryginalne dokumenty CV.
     * Dodatkowo dla każdego polskiego CV posiadamy tłumaczenie PL2EN otrzymane z systemu Joshua.
     * Wykonujemy 2 zapytania MLT. Pierwsze z nich to oferta w wer. polskiej na zbiorze polskich CV.
     * Drugie to oferta angielska na zbiorze tłumaczeń dokumentów. Sprawdzamy część wspólną wyników.
     */
    public static void main(String[] args) throws FileNotFoundException {
        offersDirectory = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/oferty";
        plOffersIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/polish_offers_list");
        offersEnglishGTDir = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/pl_eng_gt";
        outputPath = "/home/lsienko/Pobrane/cv/jo_cv/experiments/5";

        moreLikeThisHelper = new MoreLikeThis();
        requestedResultsNum = 1000;

        List<ResultsIntersection> intersections = new ArrayList<>();

        for (String nextOfferFileName : plOffersIds) {
            String polishOffer = readDocumentFromFile(offersDirectory+"/"+nextOfferFileName);
            String englishOffer = readDocumentFromFile(offersEnglishGTDir+"/"+nextOfferFileName);
            Set<String> returnedCVplQuery = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_pl_cut_only", "cvbase_originals_pl_cut_only_type", "cv_text", polishOffer, requestedResultsNum);
            Set<String> returnedCVenQuery = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_translation_joshua_plcut2en", "cvbase_translation_joshua_plcut2en_type", "cv_text", englishOffer, requestedResultsNum);

            Set<String> intersectionBoth = new LinkedHashSet<>(returnedCVplQuery);
            intersectionBoth.retainAll(returnedCVenQuery);


            ResultsIntersection result = new ResultsIntersection(
                    returnedCVplQuery.size(),
                    returnedCVenQuery.size(),
                    intersectionBoth.size());

            intersections.add(result);
        }


        saveResultOnDisk(outputPath,"intersection_exper_5", intersections);

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

    private static String readDocumentFromFile(String path) {
        List<String> fileLines;
        try {
            fileLines = Files.readAllLines(Paths.get(path), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return fileLines.stream().collect(Collectors.joining("\n"));
    }

    private static void saveResultOnDisk(String dir, String filename, List<ResultsIntersection> results) throws FileNotFoundException {
        String text = results.stream()
                .map(ResultsIntersection::toString)
                .collect(Collectors.joining("\n"));
        PrintWriter out = new PrintWriter(dir + "/" + filename);
        out.write(text);
        out.close();
    }

}
