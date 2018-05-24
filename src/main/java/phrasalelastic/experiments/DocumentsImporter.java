package phrasalelastic.experiments;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentsImporter {

    private final String pathToJSONfilesDirectory;
    private final int numerOfDocumentsInOneImportPackage = 5000;

    private RestHighLevelClient client;
    private BulkRequest indexDocumentsReq = new BulkRequest();
    private int collectedDocuments = 0;

    public DocumentsImporter(String pathToJSONfilesDirectory) {
        this.pathToJSONfilesDirectory = pathToJSONfilesDirectory;
        client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    }

    public static void main(String[] args) {
        DocumentsImporter importer = new DocumentsImporter(args[0]);
        importer.importToElastic();
    }

    public void importToElastic() {
        try (Stream<Path> docsToImportPaths = Files.walk(Paths.get(pathToJSONfilesDirectory))) {
            docsToImportPaths.filter(Files::isRegularFile)
                    .forEach(rawTextFilePath -> {
                        String jsonDoc = readFileAsString(rawTextFilePath);

                        IndexRequest request = new IndexRequest("cvbase","cv");
                        request.source(jsonDoc, XContentType.JSON);

                        indexDocumentsReq.add(request);
                        collectedDocuments = collectedDocuments + 1;
                        if (collectedDocuments >= numerOfDocumentsInOneImportPackage) {
                            indexDocuments();
                            indexDocumentsReq = new BulkRequest();
                            collectedDocuments = 0;
                        }
                    });
            if (collectedDocuments > 0) {
                indexDocuments();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void indexDocuments() {
        BulkResponse bulkResponse = null;
        try {
            bulkResponse = client.bulk(indexDocumentsReq);
            if (bulkResponse.hasFailures()) {
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                        System.err.println("\nIndex failure for doc: "+failure.getId());
                        System.err.println(failure.getMessage());
                        failure.getCause().printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readFileAsString(Path filePath) {
        String fileContent = null;
        try (BufferedReader r = Files.newBufferedReader(filePath, Charset.defaultCharset())) {
            fileContent = r.lines()
                    .map(line -> line+"\n")
                    .collect(Collectors.joining());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent;
    }

}
