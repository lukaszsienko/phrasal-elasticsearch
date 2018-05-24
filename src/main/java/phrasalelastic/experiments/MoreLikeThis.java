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
import java.util.Map;

public class MoreLikeThis {

    private RestHighLevelClient client;

    public MoreLikeThis() {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    public void doMoreLikeThisSearch(String queryText) {
        MoreLikeThisQueryBuilder moreLikeThisQueryBuilder =
                QueryBuilders.moreLikeThisQuery(
                        new String[] {"cv_pl", "cv_en"},
                        new String[] {queryText},
                        new MoreLikeThisQueryBuilder.Item[]{});
        moreLikeThisQueryBuilder.minTermFreq(1);

        SearchRequest request = new SearchRequest("cvbase");
        request.types("cv");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(moreLikeThisQueryBuilder);
        request.source(searchSourceBuilder);


        try {
            SearchResponse searchResponse = client.search(request);
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            int nr = 1;
            for (SearchHit hit : searchHits) {
                System.out.println("\n\n\n\n\n\rPosition nr "+nr++);
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String cv_pl = (String) sourceAsMap.get("cv_pl");
                System.out.println("CV_PL:");
                System.out.println(cv_pl);
                String cv_en = (String) sourceAsMap.get("cv_en");
                System.out.println("CV_EN:");
                System.out.println(cv_en);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
