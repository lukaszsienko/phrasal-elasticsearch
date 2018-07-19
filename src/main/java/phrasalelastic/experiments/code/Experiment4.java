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

public class Experiment4 {

    private static class ResultsIntersection {
        public int englishQueryResultsNum;
        public int englishTransQueryResultsNum;
        public int intersectionNum;

        public ResultsIntersection(int englishQueryResultsNum, int englishTransQueryResultsNum, int intersectionNum) {
            this.englishQueryResultsNum = englishQueryResultsNum;
            this.englishTransQueryResultsNum = englishTransQueryResultsNum;
            this.intersectionNum = intersectionNum;
        }

        @Override
        public String toString() {
            return englishQueryResultsNum + ","
                    + englishTransQueryResultsNum + ","
                    + intersectionNum;
        }
    }

    private static String offersDirectory;
    private static String offersEnglishGTDir;
    private static String offersEnglishTranslationJoshuaDir;
    private static Set<String> plOffersIds;
    private static String outputPath;

    private static MoreLikeThis moreLikeThisHelper;
    private static int requestedResultsNum;


    /**
     * W tym eksperymencie dane sa oferty dwujezyczne oraz oryginalne, tzn. nietłumaczone dokumenty CV ale tylko angielskie.
     * Bierzemy tłumaczenia polskich ofert Joshuą na język angielski oraz wersje angielskie ofert.
     * Wykonujemy 2 zapytania MLT. Pierwsze z nich to oferta w wer. angielskiej a drugie dotyczy tłumaczenenia.
     * Sprawdzamy część wspólną wyników.
     * Chcemy sprawdzić w jakim stopniu tłumaczenie realizuje niezależność wyszukiwania od języka.
     */
    public static void main(String[] args) throws FileNotFoundException {
        offersDirectory = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/oferty";
        offersEnglishGTDir = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/pl_eng_gt";
        offersEnglishTranslationJoshuaDir = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/pl_eng_joshua";
        plOffersIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/polish_offers_list");
        outputPath = "/home/lsienko/Pobrane/cv/jo_cv/experiments/3";

        moreLikeThisHelper = new MoreLikeThis();
        requestedResultsNum = 1000;

        List<ResultsIntersection> intersections = new ArrayList<>();

        for (String nextOfferFileName : plOffersIds) {
            //String polishOffer = readDocumentFromFile(offersDirectory+"/"+nextOfferFileName);
            String englishOffer = readDocumentFromFile(offersEnglishGTDir+"/"+nextOfferFileName);
            String englishTranslationOffer = readDocumentFromFile(offersEnglishTranslationJoshuaDir+"/"+nextOfferFileName);
            Set<String> returnedCVsEnglishOffer = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_equal", "cv", "cv_text", englishOffer, requestedResultsNum);
            Set<String> returnedCVsEnglishTranslationOffer = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_en_only", "cvbase_originals_en_only_type", "cv_text", englishTranslationOffer, requestedResultsNum);

            Set<String> intersectionBoth = new LinkedHashSet<>(returnedCVsEnglishOffer);
            intersectionBoth.retainAll(returnedCVsEnglishTranslationOffer);


            ResultsIntersection result = new ResultsIntersection(
                    returnedCVsEnglishOffer.size(),
                    returnedCVsEnglishTranslationOffer.size(),
                    intersectionBoth.size());

            intersections.add(result);
        }


        saveResultOnDisk(outputPath,"intersection_results_two_eng_queries", intersections);

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
