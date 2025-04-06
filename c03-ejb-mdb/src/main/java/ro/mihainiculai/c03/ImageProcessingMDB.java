package ro.mihainiculai.c03;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.mihainiculai.rmi.RmiHelper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/zoom"),
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        }
)
public class ImageProcessingMDB implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingMDB.class);
    private static final ExecutorService RMI_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String C06_IMAGE_UPLOAD_URL = System.getenv().getOrDefault(
            "C06_IMAGE_UPLOAD_URL",
            "http://0.0.0.0:3001/api/upload"
    );

    private final RmiHelper rmiServiceHelper;

    public ImageProcessingMDB() {
        String c04RmiHost = System.getenv().getOrDefault("C04_RMI_HOST", "0.0.0.0");
        String c04RmiPort = System.getenv().getOrDefault("C04_RMI_PORT", "10991");
        String c05RmiHost = System.getenv().getOrDefault("C05_RMI_HOST", "0.0.0.0");
        String c05RmiPort = System.getenv().getOrDefault("C05_RMI_PORT", "10992");

        this.rmiServiceHelper = new RmiHelper(c04RmiHost, c04RmiPort, c05RmiHost, c05RmiPort);
    }

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof TextMessage)) {
            logger.warn("Received unexpected message type. Ignoring.");
            return;
        }

        try {
            TextMessage textMessage = (TextMessage) message;
            String jsonPayload = textMessage.getText();
            JsonNode jsonNode = mapper.readTree(jsonPayload);

            String jobId = jsonNode.get("jobId").asText();
            double zoomLevel = jsonNode.get("zoomLevel").asDouble();
            String imageBase64 = jsonNode.get("imageBase64").asText();

            byte[] originalImageBytes = Base64.getDecoder().decode(imageBase64);
            BmpUtils.BmpDimensions dimensions = BmpUtils.extractDimensions(originalImageBytes);

            logger.info("Processing job {} ({}x{}, zoom {})",
                    jobId, dimensions.width(), dimensions.height(), zoomLevel);

            // Split image directly using byte manipulation
            byte[] topPart = BmpUtils.getTopHalf(originalImageBytes);
            byte[] bottomPart = BmpUtils.getBottomHalf(originalImageBytes);
            originalImageBytes = null; // Help GC

            // Process parts in parallel with bounded executor
            CompletableFuture<byte[]> topFuture = CompletableFuture.supplyAsync(
                    () -> rmiServiceHelper.callC04(topPart, zoomLevel), RMI_EXECUTOR);

            CompletableFuture<byte[]> bottomFuture = CompletableFuture.supplyAsync(
                    () -> rmiServiceHelper.callC05(bottomPart, zoomLevel), RMI_EXECUTOR);

            byte[] finalImage = topFuture.thenCombine(bottomFuture, BmpUtils::combineParts).join();

            String downloadUrl = uploadToNodeServer(finalImage, jobId);
            if (downloadUrl != null) {
                JobNotifier.notifyJobDone(jobId, downloadUrl);
            }
        } catch (Exception e) {
            logger.error("Processing failed: {}", e.getMessage(), e);
        }
    }

    private String uploadToNodeServer(byte[] imageData, String jobId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(C06_IMAGE_UPLOAD_URL))
                    .header("Content-Type", "application/octet-stream")
                    .header("jobId", jobId)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(imageData))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readTree(response.body()).get("downloadUrl").asText();
            } else {
                logger.error("Upload failed ({}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Upload error: {}", e.getMessage(), e);
        }
        return null;
    }
}
