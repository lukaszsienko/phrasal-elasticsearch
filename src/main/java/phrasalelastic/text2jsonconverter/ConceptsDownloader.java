package phrasalelastic.text2jsonconverter;

import it.uniroma1.lcl.babelfy.commons.annotation.SemanticAnnotation;
import it.uniroma1.lcl.babelfy.core.Babelfy;
import java.util.List;
import java.util.stream.Collectors;
import phrasalelastic.text2jsonconverter.LanguageDetector.Language;

public class ConceptsDownloader {

    private Babelfy bfy;

    public ConceptsDownloader() {
        bfy = new Babelfy();
    }

    public List<String> getConcepts(List<String> sentences, Language language) throws RuntimeException {
        String inputText = sentences.stream().collect(Collectors.joining(" "));
        List<SemanticAnnotation> bfyAnnotations = bfy.babelfy(inputText, language.getBabelfyLanguage());
        List<String> concepts = bfyAnnotations.stream()
                .map(annotation -> annotation.getBabelSynsetID())
                .collect(Collectors.toList());
        return concepts;
    }
}
