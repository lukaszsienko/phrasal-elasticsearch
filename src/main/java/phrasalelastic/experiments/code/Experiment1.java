package phrasalelastic.experiments.code;

import phrasalelastic.experiments.helpers.MoreLikeThis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Experiment1 {

    private static class ResultLangDistr implements Comparable<ResultLangDistr> {
        public Integer allResultCV;
        public Integer polishResultCV;
        public Integer englishResultCV;

        public ResultLangDistr(int allResultCV, int polishResultCV, int englishResultCV) {
            this.allResultCV = allResultCV;
            this.polishResultCV = polishResultCV;
            this.englishResultCV = englishResultCV;
        }

        @Override
        public int compareTo(ResultLangDistr resultLangDistr) {
            return allResultCV.compareTo(resultLangDistr.allResultCV);
        }
    }

    private static String offersDirectory;
    private static Set<String> plOffersIds;
    private static Set<String> enOffersIds;
    private static Set<String> plCvIds;
    private static Set<String> enCvIds;
    private static String outputPath;
    private static MoreLikeThis moreLikeThisHelper;
    private static int requestedResultsNum;



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
        offersDirectory = "/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/oferty";
        plOffersIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/polish_offers_list");
        enOffersIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/pracuj_pl_it_polska/wersja_1_formatowanie_zachowane/english_offers_list");
        plCvIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_polish_cv_cut");
        enCvIds = readSetFromFile("/home/lsienko/Pobrane/cv/jo_cv/list_english_cv");
        outputPath = "/home/lsienko/Pobrane/cv/jo_cv/experiments/1";

        moreLikeThisHelper = new MoreLikeThis();
        requestedResultsNum = 1000;

        List<ResultLangDistr> polishOffersLangDistr = runExperimentLangDistrForOffers(plOffersIds);
        Collections.sort(polishOffersLangDistr);
        saveResultOnDisk(outputPath, "polishOffersLangDistr", polishOffersLangDistr);

        List<ResultLangDistr> englishOffersLangDistr = runExperimentLangDistrForOffers(enOffersIds);
        Collections.sort(englishOffersLangDistr);
        saveResultOnDisk(outputPath, "englishOffersLangDistr", englishOffersLangDistr);

        moreLikeThisHelper.closeConnection();
    }

    private static List<ResultLangDistr> runExperimentLangDistrForOffers(Set<String> offersIds) {
        List<ResultLangDistr> offersLangDistr = new ArrayList<>();
        for (String nextOfferId : offersIds) {
            String nextOfferText = readDocumentFromFile(offersDirectory+"/"+nextOfferId);
            Set<String> returnedCVs = moreLikeThisHelper.doMoreLikeThisSearch("cvbase_originals_equal", "cv", "cv_text", nextOfferText, requestedResultsNum);
            if (!returnedCVs.isEmpty()) {
                ResultLangDistr resultLangDistr = analizeResults(returnedCVs);
                offersLangDistr.add(resultLangDistr);
            }
        }
        return offersLangDistr;
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

    private static ResultLangDistr analizeResults(Set<String> returnedCVs) {
        int plNum = 0;
        int engNum = 0;
        for (String cvFileName : returnedCVs) {
            if (plCvIds.contains(cvFileName)) {
                plNum++;
            } else if (enCvIds.contains(cvFileName)) {
                engNum++;
            }
        }
        return new ResultLangDistr(returnedCVs.size(), plNum, engNum);
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

    private static void saveResultOnDisk(String dir, String filename, List<ResultLangDistr> results) throws FileNotFoundException {
        String text = results.stream()
                .map(resultObject -> resultObject.allResultCV + "," + resultObject.polishResultCV + "," + resultObject.englishResultCV)
                .collect(Collectors.joining("\n"));
        PrintWriter out = new PrintWriter(dir + "/" + filename);
        out.write(text);
        out.close();
    }
}
