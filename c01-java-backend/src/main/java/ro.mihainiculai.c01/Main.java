package ro.mihainiculai.c01;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String JMS_HOST = System.getenv().getOrDefault("JMS_BROKER_HOST", "0.0.0.0");
    private static final String JMS_PORT = System.getenv().getOrDefault("JMS_BROKER_PORT", "61616");
    
    private static final String JMS_TOPIC = "jms/topic/zoom";
    private static final String BMP_CONTENT_TYPE = "image/bmp";
    private static final int SERVER_PORT = 8081;

    private static JmsMessageProducer jmsProducer;

    public static void main(String[] args) {
        initializeJmsProducer();

        Javalin app = createJavalinApp();
        logger.info("Server started on port {}", SERVER_PORT);

        setupRoutes(app);
        setupWebSocket(app);
        addShutdownHook(app);
    }

    private static void initializeJmsProducer() {
        try {
            jmsProducer = new JmsMessageProducer(JMS_HOST, JMS_PORT, JMS_TOPIC);
        } catch (Exception e) {
            logger.error("Failed to initialize JMS Producer: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static Javalin createJavalinApp() {
        return Javalin.create(config ->
                config.bundledPlugins.enableCors(cors ->
                        cors.addRule(CorsPluginConfig.CorsRule::anyHost)
                )
        ).start(SERVER_PORT);
    }

    private static void setupRoutes(Javalin app) {
        app.get("/", ctx -> ctx.result("Hello from C01!"));

        app.post("/api/upload", Main::handleFileUpload);

        app.post("/api/notifyJobDone", Main::handleJobCompletionNotification);
    }

    private static void setupWebSocket(Javalin app) {
        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                WebSocketHandler.registerSession(ctx);
                logger.info("Client connected to WebSocket: {}", ctx.sessionId());
            });

            ws.onMessage(ctx -> logger.info("WebSocket message from client: {}", ctx.message()));

            ws.onClose(ctx -> {
                WebSocketHandler.removeSession(ctx);
                logger.info("WebSocket closed: {}", ctx.sessionId());
            });
        });
    }

    private static void addShutdownHook(Javalin app) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
            if (jmsProducer != null) {
                jmsProducer.close();
            }
            app.stop();
            logger.info("Application stopped.");
        }));
    }

    private static void handleFileUpload(io.javalin.http.Context ctx) {
        UploadedFile uploadedFile = ctx.uploadedFile("image");
        if (uploadedFile == null) {
            logger.warn("No file uploaded in the request.");
            ctx.status(400).json(Map.of("message", "No file uploaded."));
            return;
        }

        if (!isValidContentType(uploadedFile.contentType())) {
            logger.warn("Invalid file type uploaded: {}", uploadedFile.contentType());
            ctx.status(400).json(Map.of("message", "File must be a BMP image."));
            return;
        }

        String zoomLevelParam = ctx.formParam("zoomLevel");
        if (zoomLevelParam == null) {
            logger.warn("Missing zoomLevel parameter.");
            ctx.status(400).json(Map.of("message", "zoomLevel is missing."));
            return;
        }

        double zoomLevel;
        try {
            zoomLevel = Double.parseDouble(zoomLevelParam);
        } catch (NumberFormatException e) {
            logger.warn("Invalid zoomLevel format: {}", zoomLevelParam);
            ctx.status(400).json(Map.of("message", "Invalid zoomLevel format."));
            return;
        }

        String jobId = generateJobId();
        logger.info("Received BMP file, jobId={}, zoomLevel={}", jobId, zoomLevel);

        try {
            byte[] fileBytes = readFileBytes(uploadedFile.content());
            jmsProducer.sendMessage(jobId, zoomLevel, fileBytes);
        } catch (Exception e) {
            logger.error("Failed to send message to JMS for jobId={}: {}", jobId, e.getMessage(), e);
            ctx.status(500).json(Map.of("message", "Failed to send message to JMS."));
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("message", "Image received. Processing...");
        ctx.json(response);
    }

    private static boolean isValidContentType(String contentType) {
        return contentType != null && contentType.equalsIgnoreCase(BMP_CONTENT_TYPE);
    }

    private static String generateJobId() {
        return "job-" + UUID.randomUUID();
    }

    private static byte[] readFileBytes(InputStream inputStream) throws Exception {
        return inputStream.readAllBytes();
    }

    private static void handleJobCompletionNotification(io.javalin.http.Context ctx) {
        Map<String, String> requestBody;
        try {
            requestBody = ctx.bodyAsClass(new JavalinJackson().getMapper().getTypeFactory()
                    .constructMapType(HashMap.class, String.class, String.class));
        } catch (Exception e) {
            logger.warn("Invalid request body format.");
            ctx.status(400).json(Map.of("message", "Invalid request body format."));
            return;
        }

        String jobId = requestBody.get("jobId");
        String downloadUrl = requestBody.get("downloadUrl");

        if (jobId == null || downloadUrl == null) {
            logger.warn("Missing jobId or downloadUrl in the request.");
            ctx.status(400).json(Map.of("message", "jobId and downloadUrl are required."));
            return;
        }

        logger.info("Job completed, jobId={}. Notifying WebSocket clients...", jobId);

        try {
            WebSocketHandler.sendJobCompletionNotification(jobId, downloadUrl);
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification for jobId={}: {}", jobId, e.getMessage(), e);
            ctx.status(500).json(Map.of("message", "Failed to send WebSocket notification."));
            return;
        }

        ctx.status(200).result("OK");
    }
}
