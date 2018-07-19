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

public class Experiment3 {

    private static class ResultsIntersection {
        public int allPolishQueryNum;
        public int allEnglishQueryNum;
        public int allBothQueryNum;
        public int intersectionPolishEnglishNum;
        public int sumPolishEnglishNum;
        public int intersectionPolEngAndBothNum;

        public ResultsIntersection(int allPolishQueryNum, int allEnglishQueryNum, int allBothQueryNum, int intersectionPolishEnglishNum, int sumPolishEnglishNum, int intersectionPolEngAndBothNum) {
            this.allPolishQueryNum = allPolishQueryNum;
            this.allEnglishQueryNum = allEnglishQueryNum;
            this.allBothQueryNum = allBothQueryNum;
            this.intersectionPolishEnglishNum = intersectionPolishEnglishNum;
            this.sumPolishEnglishNum = sumPolishEnglishNum;
            this.intersectionPolEngAndBothNum = intersectionPolEngAndBothNum;
        }

        @Override
        public String toString() {
            return allPolishQueryNum + ","
                    + allEnglishQueryNum + ","
                    + allBothQueryNum + ","
                    + intersectionPolishEnglishNum + ","
                    + sumPolishEnglishNum + ","
                    + intersectionPolEngAndBothNum;
        }
    }

    private static String offersDirectory;
    private static String offersEnglishGTDir;
    private static String offersPolishGTDir;
    private static Set<String> plOffersIds;
    private static Set<String> enOffersIds;
    private static String outputPath;

    private static MoreLikeThis moreLikeThisHelper;
    private static int requestedResultsNum;


    /**
     * W tym eksperymencie dane sa oferty dwujezyczne oraz oryginalne, tzn. nietłumaczone dokumenty CV.
     * Dodatkowo tworzymy ofertę, która jest połączeniem oferty polskiej i angielskiej.
     * Dla każdej oferty występującej w dwoch językach odpytujemy najpierw o CV dla wersji oferty polskiej,
     * a następnie wersji angielskiej. Następnie pytamy o CV dla oferty składającej się z połaczonej oferty.
     * Sprawdzamy ile zostanie znalezionych tych samych CV dla zapytania PL oraz EN. Następnie sumujemy
     * wynikowe CV z zapytania PL oraz EN (suma zbiorów). Szukamy CV dla oferty połączonej i sprawdzamy
     * ile CV dla oferty połączonej to te CV, które zostały zwrócone w zapytaniu PL o EN.
     * Chcemy sprawdzić czy połączenie oferty PL i EN da w wyniku oferty zawacane przez zapytania osobno PL + osobno EN.
     */
    public static void main(String[] args) throws FileNotFoundException {
        offersDirectory = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/oferty";
        offersEnglishGTDir = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/pl_eng_gt";
        offersPolishGTDir = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/eng_pl_gt";
        plOffersIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/polish_offers_list");
        enOffersIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/english_offers_list");
        outputPath = "/home/lsienko/Pobrane/cv/jo_cv/experiments/3";

        moreLikeThisHelper = new MoreLikeThis();
        requestedResultsNum = 1000;

        List<ResultsIntersection> intersections = new ArrayList<>();

        List<ResultsIntersection> intersectionPart1 = runExperimentQueryPlEngIntersection(plOffersIds, true);
        intersections.addAll(intersectionPart1);

        List<ResultsIntersection> intersectionPart2 = runExperimentQueryPlEngIntersection(enOffersIds, false);
        intersections.addAll(intersectionPart2);

        saveResultOnDisk(outputPath,"intersection_results", intersections);

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

    private static List<ResultsIntersection> runExperimentQueryPlEngIntersection(Set<String> offersToQuery, boolean polishOffers) {
        List<ResultsIntersection> intersections = new ArrayList<>();

        for (String nextOffer : offersToQuery) {
            String polishOffer = readDocumentFromFile(polishOffers ? (offersDirectory+"/"+nextOffer) : (offersPolishGTDir+"/"+nextOffer));
            String englishOffer = readDocumentFromFile((polishOffers ? (offersEnglishGTDir+"/"+nextOffer) : offersDirectory+"/"+nextOffer));
            String bothOffers = polishOffer + "\n" + englishOffer;
            Set<String> returnedCVsPolishOffer = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_equal", "cv", "cv_text", polishOffer, requestedResultsNum);
            Set<String> returnedCVsEnglishOffer = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_equal", "cv", "cv_text", englishOffer, requestedResultsNum);
            Set<String> returnedCVsBothOffer = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_equal", "cv", "cv_text", bothOffers, 2*requestedResultsNum);

            Set<String> intersectionPolEng = new LinkedHashSet<>(returnedCVsEnglishOffer);
            intersectionPolEng.retainAll(returnedCVsPolishOffer);

            Set<String> sumPolEng = new LinkedHashSet<>(returnedCVsPolishOffer);
            sumPolEng.addAll(returnedCVsEnglishOffer);

            Set<String> intersectionPolEngBoth = new LinkedHashSet<>(sumPolEng);
            intersectionPolEng.retainAll(returnedCVsBothOffer);

            ResultsIntersection result = new ResultsIntersection(returnedCVsPolishOffer.size(),
                    returnedCVsEnglishOffer.size(),
                    returnedCVsBothOffer.size(),
                    intersectionPolEng.size(),
                    sumPolEng.size(),
                    intersectionPolEngBoth.size());

            intersections.add(result);
        }

        return intersections;
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
