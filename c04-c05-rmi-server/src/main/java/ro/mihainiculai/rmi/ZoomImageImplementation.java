package ro.mihainiculai.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ZoomImageImplementation extends UnicastRemoteObject implements ZoomImageInterface {

    private static final Logger logger = LoggerFactory.getLogger(ZoomImageImplementation.class);

    protected ZoomImageImplementation() throws RemoteException {
        super();
    }

    @Override
    public byte[] zoomImage(byte[] imageData, double zoomLevel) throws RemoteException {
        logger.info("Received {} bytes for zoom level: {}", imageData.length, zoomLevel);

        try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage originalImage = ImageIO.read(imageInputStream);
            if (originalImage == null) {
                throw new IllegalArgumentException("Invalid or unsupported BMP data.");
            }

            int newWidth = (int) Math.round(originalImage.getWidth() * zoomLevel);
            int newHeight = (int) Math.round(originalImage.getHeight() * zoomLevel);

            Image scaledInstanceImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

            BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D graphics2D = scaledImage.createGraphics();
            graphics2D.drawImage(scaledInstanceImage, 0, 0, null);
            graphics2D.dispose();

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(scaledImage, "bmp", outputStream);
                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            logger.error("Error scaling BMP: ", e);
            throw new RemoteException("Error scaling BMP", e);
        }
    }
}
