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

public class Experiment6 {

    private static class Results {
        public int queryToOriginalsResultsNum;
        public int queryToOriginalsResultsNumPolish;
        public int queryToOriginalsResultsNumEnglish;
        public int queryToOriginalsPlusTranslResultsNum;
        public int queryToOriginalsPlusTranslResultsNumPolish;
        public int queryToOriginalsPlusTranslResultsNumEnglish;

        public Results(int queryToOriginalsResultsNum, int queryToOriginalsResultsNumPolish, int queryToOriginalsResultsNumEnglish,
                       int queryToOriginalsPlusTranslResultsNum, int queryToOriginalsPlusTranslResultsNumPolish, int queryToOriginalsPlusTranslResultsNumEnglish) {
            this.queryToOriginalsResultsNum = queryToOriginalsResultsNum;
            this.queryToOriginalsResultsNumPolish = queryToOriginalsResultsNumPolish;
            this.queryToOriginalsResultsNumEnglish = queryToOriginalsResultsNumEnglish;
            this.queryToOriginalsPlusTranslResultsNum = queryToOriginalsPlusTranslResultsNum;
            this.queryToOriginalsPlusTranslResultsNumPolish = queryToOriginalsPlusTranslResultsNumPolish;
            this.queryToOriginalsPlusTranslResultsNumEnglish = queryToOriginalsPlusTranslResultsNumEnglish;
        }

        @Override
        public String toString() {
            return queryToOriginalsResultsNum + ","
                    + queryToOriginalsResultsNumPolish + ","
                    + queryToOriginalsResultsNumEnglish + ","
                    + queryToOriginalsPlusTranslResultsNum + ","
                    + queryToOriginalsPlusTranslResultsNumPolish + ","
                    + queryToOriginalsPlusTranslResultsNumEnglish;
        }
    }

    private static String offersDirectory;
    private static Set<String> enOffersIds;
    private static Set<String> plCvIds;
    private static Set<String> enCvIds;
    private static String outputPath;

    private static MoreLikeThis moreLikeThisHelper;
    private static int requestedResultsNum;


    /**
     * W tym eksperymencie dana jest oferta angielska. Zbiór CV dla pierwszego zapytania to całość CV oryginalnych (angielskie lub polskie).
     * Dodatkowo dla każdego polskiego CV posiadamy tłumaczenie PL2EN otrzymane z systemu Joshua. W drugim zapytaniu do CV oryginalnych dodajemy tłumaczenia PL2EN joshuą.
     * Analizujemy czy liczba polskich dokumentów zwiększy się.
     */
    public static void main(String[] args) throws FileNotFoundException {
        offersDirectory = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/oferty";
        enOffersIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/english_offers_list");
        plCvIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_polish_cv_cut");
        enCvIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_english_cv");
        outputPath = "/home/lsienko/Pobrane/cv/jo_cv/experiments/6";

        moreLikeThisHelper = new MoreLikeThis();
        requestedResultsNum = 1000;

        List<Results> results = new ArrayList<>();

        for (String nextEnglishOfferFileName : enOffersIds) {
            String englishOffer = readDocumentFromFile(offersDirectory+"/"+nextEnglishOfferFileName);

            //FIRST QUERY
            Set<String> returnedCVQueryOriginals = moreLikeThisHelper.doMoreLikeThisSearch(
                    "cvbase_originals_equal",
                    "cv",
                    "cv_text",
                    englishOffer,
                    requestedResultsNum);
            int queryOrgPolish = 0;
            int queryOrgEnglish = 0;
            for (String resultFileName : returnedCVQueryOriginals) {
                if (plCvIds.contains(resultFileName)) {
                    queryOrgPolish++;
                } else if (enCvIds.contains(resultFileName)) {
                    queryOrgEnglish++;
                } else {
                    System.out.println("ERROR 1");
                }
            }

            // SECOND QUERY
            Set<String> returnedCVQueryOriginalsPlusTransl = moreLikeThisHelper.doMoreLikeThisSearch(
                    new String [] {"cvbase_originals_equal", "cvbase_translation_joshua_plcut2en"},
                    new String [] {"cv", "cvbase_translation_joshua_plcut2en_type"},
                    "cv_text",
                    englishOffer,
                    requestedResultsNum);
            int queryTranslPolish = 0;
            int queryTranslEnglish = 0;
            for (String resultFileName : returnedCVQueryOriginalsPlusTransl) {
                if (plCvIds.contains(resultFileName)) {
                    queryTranslPolish++;
                } else if (enCvIds.contains(resultFileName)) {
                    queryTranslEnglish++;
                } else {
                    System.out.println("ERROR 2");
                }
            }

            Results result = new Results(
                    returnedCVQueryOriginals.size(),
                    queryOrgPolish,
                    queryOrgEnglish,
                    returnedCVQueryOriginalsPlusTransl.size(),
                    queryTranslPolish,
                    queryTranslEnglish);

            results.add(result);
        }


        saveResultOnDisk(outputPath,"exper_6_lang_distr", results);

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

    private static void saveResultOnDisk(String dir, String filename, List<Results> results) throws FileNotFoundException {
        String text = results.stream()
                .map(Results::toString)
                .collect(Collectors.joining("\n"));
        PrintWriter out = new PrintWriter(dir + "/" + filename);
        out.write(text);
        out.close();
    }

}
