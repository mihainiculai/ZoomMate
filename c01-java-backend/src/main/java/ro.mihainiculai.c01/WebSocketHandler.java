package ro.mihainiculai.c01;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final Set<WsContext> sessions = new HashSet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static synchronized void registerSession(WsContext session) {
        sessions.add(session);
        logger.debug("WebSocket session registered: {}", session.sessionId());
    }

    public static synchronized void removeSession(WsContext session) {
        sessions.remove(session);
        logger.debug("WebSocket session removed: {}", session.sessionId());
    }

    public static void sendJobCompletionNotification(String jobId, String downloadUrl) throws JsonProcessingException {
        Map<String, String> notification = new HashMap<>();
        notification.put("jobId", jobId);
        notification.put("downloadUrl", downloadUrl);
        String jsonMessage = objectMapper.writeValueAsString(notification);

        logger.debug("Sending job completion notification: {}", jsonMessage);

        synchronized (sessions) {
            for (WsContext session : sessions) {
                session.send(jsonMessage);
            }
        }
    }
}
