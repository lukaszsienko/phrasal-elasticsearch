package phrasalelastic.experiments;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private RestHighLevelClient client;

    public Main() {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    public void closeApp() throws IOException {
        client.close();
    }

    public static String dir = "/home/lsienko/Pobrane/cv/jo_cv/oferty_pracy";
    public static String[] tests = new String[] {
            "18_4CF337AA-E224-4A92-9D58-2EDAFE72B914_scrum",
            "21_260BC571-8A82-46EC-A225-5BC50DF9C58F_java",
            "32_3DD2D0A2-3D72-4AA1-9DE4-67426B2972EF_plsql",
            "40_555C2E6A-513C-41B1-86D2-1906EF080618_web_dev",
            "sii_senior_java",
            "sollers_java_developer"};

    public static void main(String[] args) throws IOException {
        MoreLikeThis moreLikeThis = new MoreLikeThis();
        for (String testFolder: tests) {
            String canonicalPath = dir + "/" + testFolder;
            String engPath = canonicalPath + "/" + "en";
            String polPath = canonicalPath + "/" + "pl";
            File en = new File(engPath);
            File pl = new File(polPath);
            List<String> enSentences = readDocument(en.toPath());
            List<String> plSentences = readDocument(pl.toPath());

            String englishJobOrder = enSentences.stream().collect(Collectors.joining(" \n "));
            String polishJobOrder = plSentences.stream().collect(Collectors.joining(" \n "));

            System.out.println("\nTest name: "+testFolder);
            Set<String> eng_result = moreLikeThis.doMoreLikeThisSearch(englishJobOrder, "cv_en");
            Set<String> pol_result = moreLikeThis.doMoreLikeThisSearch(polishJobOrder, "cv_pl");

            Set<String> intersection = new LinkedHashSet<>(eng_result);
            intersection.retainAll(pol_result);

            System.out.println("CV dla ENG query: "+eng_result.size());
            System.out.println("CV dla POL query: "+pol_result.size());
            System.out.println("Intersection: "+intersection.size()+" out of "+ Math.min(eng_result.size(), pol_result.size()));
            System.out.println("Intersection rate: "+((float)intersection.size() / Math.min(eng_result.size(), pol_result.size()))*100+"\n");
        }
        moreLikeThis.closeConnection();

        /*DocumentsImporter documentsImporter = new DocumentsImporter("/home/lsienko/Pobrane/cv/jo_cv/json_cv");
        documentsImporter.importToElastic();
        documentsImporter.closeConnection();*/
        //Main main = new Main();
        //main.deleteIndex();
        //main.createIndex();
        //main.getAllDocuments();
        //main.closeApp();
    }

    private static List<String> readDocument(Path filePath) {
        List<String> document = new ArrayList<>();
        try {
            document = new ArrayList<>(Files.readAllLines(filePath, Charset.defaultCharset()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    private void deleteIndex() {
        DeleteIndexRequest request = new DeleteIndexRequest("cvbase");
        try {
            DeleteIndexResponse deleteIndexResponse = client.indices().delete(request);
            boolean acknowledged = deleteIndexResponse.isAcknowledged();
            System.out.println("acknowledged = "+acknowledged);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("cvbase");
        request.mapping("cv",
                "  {\n" +
                        "    \"cv\": {\n" +
                        "      \"properties\": {\n" +
                        "        \"cv_id\": {\n" +
                        "          \"type\": \"keyword\"\n" +
                        "        },\n" +
                        "        \"cv_lang_original\": {\n" +
                        "          \"type\": \"keyword\"\n" +
                        "        },\n" +
                        "        \"cv_pl\": {\n" +
                        "          \"type\": \"text\"\n" +
                        "        },\n" +
                        "        \"cv_en\": {\n" +
                        "          \"type\": \"text\"\n" +
                        "        },\n" +
                        "        \"cv_pl_concepts\": {\n" +
                        "          \"type\": \"keyword\"\n" +
                        "        },\n" +
                        "        \"cv_en_concepts\": {\n" +
                        "          \"type\": \"keyword\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }",
                XContentType.JSON);
        CreateIndexResponse createIndexResponse = client.indices().create(request);
        System.out.println(createIndexResponse.index());
        System.out.println(createIndexResponse.isAcknowledged());
    }

    private void getAllDocuments() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        /*
        SearchRequest searchRequest = new SearchRequest("posts");
        searchRequest.types("doc");
         */

        SearchResponse searchResponse = client.search(searchRequest);

        RestStatus status = searchResponse.status();
        TimeValue took = searchResponse.getTook();
        Boolean terminatedEarly = searchResponse.isTerminatedEarly();
        boolean timedOut = searchResponse.isTimedOut();

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            // do something with the SearchHit
            String sourceAsString = hit.getSourceAsString();
            System.out.println(sourceAsString);
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            System.out.println(sourceAsMap.get("cv_pl"));
            System.out.println(sourceAsMap.get("cv_en"));
        }

    }

    private void getSpecifiedDocument() throws IOException {
        //Get documents
        GetRequest getRequest = new GetRequest("cvbase","cv", "1");
        GetResponse getResponse = client.get(getRequest);

        String index = getResponse.getIndex();
        System.out.println(index);
        String type = getResponse.getType();
        System.out.println(type);
        String id = getResponse.getId();
        System.out.println(id);
        if (getResponse.isExists()) {
            long version = getResponse.getVersion();
            String sourceAsString = getResponse.getSourceAsString();
            Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
            byte[] sourceAsBytes = getResponse.getSourceAsBytes();
        } else {

        }
    }

    private void addNewDocument() throws IOException {
        /*Map<String, Object> jsonDocument = new HashMap<String, Object>();
        jsonDocument.put("cv_pl", "Oto treść polskiego CV ąśćżźó i takie tam.");
        jsonDocument.put("cv_en", "And that's english translation of Polish cv.");

        IndexRequest request = new IndexRequest("cvbase","cv");
        request.source(jsonDocument);*/

        IndexRequest request2 = new IndexRequest("cvbase","cv");
        request2.source("cv_pl", "Oto treść polskiego CV ąśćżźó i takie tam.", "cv_en", "And that's english translation of Polish cv.");

        IndexResponse indexResponse = null;
        try {
            indexResponse = client.index(request2);
        } catch(ElasticsearchException e) {
            e.printStackTrace();
            if (e.status() == RestStatus.CONFLICT) {
                System.out.println("e.status() == RestStatus.CONFLICT");
            }
        }


        String index = indexResponse.getIndex();
        String type = indexResponse.getType();
        String id = indexResponse.getId();
        long version = indexResponse.getVersion();
        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
            System.out.println("Handle (if needed) the case where the document was created for the first time ");
        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            System.out.println("Handle (if needed) the case where the document was rewritten as it was already existing");
        }
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            System.out.println("Handle the situation where number of successful shards is less than total shards");
        }
        if (shardInfo.getFailed() > 0) {
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                String reason = failure.reason();
                System.out.println("Handle the potential failures, reason: "+reason);
            }
        }
    }
}