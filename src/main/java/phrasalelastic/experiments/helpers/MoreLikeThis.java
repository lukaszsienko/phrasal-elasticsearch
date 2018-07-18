package phrasalelastic.experiments.helpers;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;

public class MoreLikeThis {

    private RestHighLevelClient client;

    public MoreLikeThis() {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    public Set<String> doMoreLikeThisSearch(String searchIndex, String searchType, String fieldName, String queryText, int resultsNum) {
        MoreLikeThisQueryBuilder moreLikeThisQueryBuilder = QueryBuilders.moreLikeThisQuery(
                new String[] {fieldName},
                new String[] {queryText},
                new MoreLikeThisQueryBuilder.Item[]{});
        moreLikeThisQueryBuilder.minTermFreq(1);
        moreLikeThisQueryBuilder.maxQueryTerms(300);
        moreLikeThisQueryBuilder.minDocFreq(2);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(moreLikeThisQueryBuilder);
        searchSourceBuilder.size(resultsNum);

        SearchRequest request = new SearchRequest(searchIndex);
        request.types(searchType);
        request.source(searchSourceBuilder);

        Set<String> results = new LinkedHashSet<>();
        try {
            SearchResponse searchResponse = client.search(request);
            SearchHits hitBlock = searchResponse.getHits();
            List<SearchHit> hitsList = Arrays.asList(hitBlock.getHits());
            for (SearchHit hit : hitsList) {
                if (!hit.getIndex().equals(searchIndex) ||
                        !hit.getType().equals(searchType)) {
                    throw new RuntimeException("Elasticsearch returns documents from different indices or types than requested.");
                }
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String cvFileName = (String) sourceAsMap.get("cv_file_name");
                results.add(cvFileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }

    public void closeConnection() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
