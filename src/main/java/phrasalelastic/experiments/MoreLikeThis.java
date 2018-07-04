package phrasalelastic.experiments;

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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class MoreLikeThis {

    private RestHighLevelClient client;

    public MoreLikeThis() {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    public Set<String> doMoreLikeThisSearch(String queryText, String lang) {
        MoreLikeThisQueryBuilder moreLikeThisQueryBuilder =
                QueryBuilders.moreLikeThisQuery(
                        new String[] {lang},/*{"cv_pl", "cv_en"}*/
                        new String[] {queryText},
                        new MoreLikeThisQueryBuilder.Item[]{});
        //moreLikeThisQueryBuilder.minTermFreq(1);

        SearchRequest request = new SearchRequest("cvbase");
        request.types("cv");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(moreLikeThisQueryBuilder);
        searchSourceBuilder.size(1000);
        request.source(searchSourceBuilder);

        Set<String> results = new LinkedHashSet<>();
        try {
            SearchResponse searchResponse = client.search(request);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String cv_id = (String) sourceAsMap.get("cv_id");
                results.add(cv_id);
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
