package ro.mihainiculai.c03;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JobNotifier {
    private static final Logger logger = LoggerFactory.getLogger(JobNotifier.class);

    private static final String C01_NOTIFY_URL = System.getenv().getOrDefault(
            "C01_NOTIFY_URL",
            "http://0.0.0.0:8081/api/notifyJobDone"
    );
    private static final String C06_DOWNLOAD_URL_PREFIX = System.getenv().getOrDefault(
            "C06_DOWNLOAD_URL_PREFIX",
            "http://0.0.0.0:8081/api/notifyJobDone/"
    );

    public static void notifyJobDone(String jobId, String downloadUrl)
            throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        String jsonBody = String.format(
                "{\"jobId\":\"%s\",\"downloadUrl\":\"%s\"}",
                jobId,
                C06_DOWNLOAD_URL_PREFIX + downloadUrl
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(C01_NOTIFY_URL))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> httpResponse = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (httpResponse.statusCode() == 200) {
            logger.info("Notified C01 that jobId={} is done!", jobId);
        } else {
            logger.error(
                    "Failed to notify C01. Status={}, body={}",
                    httpResponse.statusCode(),
                    httpResponse.body()
            );
        }
    }
}
