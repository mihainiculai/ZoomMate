package ro.mihainiculai.c03;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class BmpUtils {
    private static final Logger logger = LoggerFactory.getLogger(BmpUtils.class);

    public static BmpDimensions extractDimensions(byte[] bmp) {
        if (bmp.length < 54 || bmp[0] != 'B' || bmp[1] != 'M') {
            throw new IllegalArgumentException("Invalid BMP format");
        }

        return new BmpDimensions(
                readInt(bmp, 18),
                readInt(bmp, 22),
                readInt(bmp, 10)
        );
    }

    public static byte[] getTopHalf(byte[] original) {
        BmpDimensions dim = extractDimensions(original);
        return extractRegion(original, dim, 0, dim.height() / 2);
    }

    public static byte[] getBottomHalf(byte[] original) {
        BmpDimensions dim = extractDimensions(original);
        int start = dim.height() / 2;
        return extractRegion(original, dim, start, dim.height() - start);
    }

    private static byte[] extractRegion(byte[] original, BmpDimensions dim, int startRow, int numRows) {
        int rowSize = ((dim.width() * 3) + 3) & ~3;
        int pixelStart = dim.pixelDataOffset();
        int totalRows = dim.height();

        int from = totalRows - startRow - numRows;
        int to = from + numRows;

        byte[] header = Arrays.copyOfRange(original, 0, dim.pixelDataOffset());
        updateHeader(header, numRows, rowSize * numRows);

        byte[] pixels = new byte[numRows * rowSize];
        System.arraycopy(
                original,
                pixelStart + from * rowSize,
                pixels,
                0,
                pixels.length
        );

        return concatenate(header, pixels);
    }

    public static byte[] combineParts(byte[] top, byte[] bottom) {
        try {
            BmpDimensions topDim = extractDimensions(top);
            BmpDimensions bottomDim = extractDimensions(bottom);

            if (topDim.width() != bottomDim.width()) {
                throw new IllegalArgumentException("Incompatible widths");
            }

            byte[] newHeader = Arrays.copyOfRange(top, 0, topDim.pixelDataOffset());
            updateHeader(newHeader, topDim.height() + bottomDim.height(),
                    (topDim.height() + bottomDim.height()) * ((topDim.width() * 3 + 3) & ~3));

            byte[] combinedPixels = concatenate(
                    Arrays.copyOfRange(bottom, bottomDim.pixelDataOffset(), bottom.length),
                    Arrays.copyOfRange(top, topDim.pixelDataOffset(), top.length)
            );

            return concatenate(newHeader, combinedPixels);
        } catch (Exception e) {
            logger.error("Combine failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    private static int readInt(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }

    private static void updateHeader(byte[] header, int height, int imageSize) {
        ByteBuffer.wrap(header)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(22, height)
                .putInt(34, imageSize)
                .putInt(2, header.length + imageSize);
    }

    private static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public record BmpDimensions(int width, int height, int pixelDataOffset) {
    }
}
