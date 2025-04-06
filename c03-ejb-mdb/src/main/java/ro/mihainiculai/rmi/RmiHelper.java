package ro.mihainiculai.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Naming;

public class RmiHelper {
    private static final Logger logger = LoggerFactory.getLogger(RmiHelper.class);

    private final String c04Url;
    private final String c05Url;

    public RmiHelper(String c04Host, String c04Port, String c05Host, String c05Port) {
        this.c04Url = "rmi://" + c04Host + ":" + c04Port + "/ZOOM-SERVER";
        this.c05Url = "rmi://" + c05Host + ":" + c05Port + "/ZOOM-SERVER";
    }

    public byte[] callC04(byte[] imagePart, double zoomLevel) {
        try {
            ZoomImageInterface stub = (ZoomImageInterface) Naming.lookup(c04Url);
            return stub.zoomImage(imagePart, zoomLevel);
        } catch (Exception e) {
            logger.error("Error calling RMI C04: {}", e.getMessage(), e);
            return null;
        }
    }

    public byte[] callC05(byte[] imagePart, double zoomLevel) {
        try {
            ZoomImageInterface stub = (ZoomImageInterface) Naming.lookup(c05Url);
            return stub.zoomImage(imagePart, zoomLevel);
        } catch (Exception e) {
            logger.error("Error calling RMI C05: {}", e.getMessage(), e);
            return null;
        }
    }
}
