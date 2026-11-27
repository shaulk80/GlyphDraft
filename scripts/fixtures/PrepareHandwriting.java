import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Turns a photo or a drawn page (JPEG or PNG) into a handwriting fixture: upright (the EXIF orientation is applied), at
 * most 2048 pixels on the long side, JPEG without metadata (a phone photo carries the place where it was taken).
 *
 * Run from the repository root: java scripts/fixtures/PrepareHandwriting.java &lt;input&gt; &lt;output.jpg&gt;
 */
public class PrepareHandwriting {
    private static final int MAX_LONG_SIDE = 2048;
    private static final float JPEG_QUALITY = 0.85f;
    private static final int UPRIGHT = 1;
    private static final int APP1 = 0xE1;
    private static final int START_OF_SCAN = 0xDA;
    private static final int ORIENTATION_TAG = 0x0112;
    private static final byte[] EXIF_HEADER = {'E', 'x', 'i', 'f', 0, 0};

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java scripts/fixtures/PrepareHandwriting.java <input> <output.jpg>");
            System.exit(2);
        }

        Path input = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        if (input.equals(output)) {
            System.err.println("The output must not be the input: the original would be lost.");
            System.exit(2);
        }

        try {
            if (!prepare(input, output)) {
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("The fixture could not be made from " + input + ": " + e);
            System.exit(1);
        }
    }

    private static boolean prepare(Path input, Path output) throws IOException {
        byte[] bytes = Files.readAllBytes(input);
        BufferedImage source = ImageIO.read(input.toFile());
        if (source == null) {
            System.err.println("Not an image this tool can read (JPEG or PNG): " + input);
            return false;
        }

        int orientation = orientationOf(bytes);
        BufferedImage result = shrink(upright(source, orientation));
        writeJpeg(result, output);
        System.out.printf(
                "%s: %d x %d, %d KB (source %d x %d, EXIF orientation %d)%n",
                output.getFileName(),
                result.getWidth(),
                result.getHeight(),
                Files.size(output) / 1024,
                source.getWidth(),
                source.getHeight(),
                orientation);
        return true;
    }

    /** The EXIF orientation (1 to 8) of a JPEG, or 1 (upright) when it has none. */
    private static int orientationOf(byte[] file) {
        ByteBuffer jpeg = ByteBuffer.wrap(file);
        if (file.length < 4 || (jpeg.getShort(0) & 0xFFFF) != 0xFFD8) {
            return UPRIGHT;
        }

        try {
            int position = 2;
            while ((jpeg.get(position) & 0xFF) == 0xFF && (jpeg.get(position + 1) & 0xFF) != START_OF_SCAN) {
                int length = jpeg.getShort(position + 2) & 0xFFFF;
                if ((jpeg.get(position + 1) & 0xFF) == APP1 && isExif(jpeg, position + 4)) {
                    return orientationInTiff(jpeg.slice(position + 4 + EXIF_HEADER.length, length - 2 - EXIF_HEADER.length));
                }

                position += 2 + length;
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Warning: the metadata is damaged, the orientation is not applied; check the result. " + e);
        }

        return UPRIGHT;
    }

    private static boolean isExif(ByteBuffer jpeg, int position) {
        for (int i = 0; i < EXIF_HEADER.length; i++) {
            if (jpeg.get(position + i) != EXIF_HEADER[i]) {
                return false;
            }
        }

        return true;
    }

    /** Looks the tag up in the first directory of the TIFF structure that EXIF data is. */
    private static int orientationInTiff(ByteBuffer tiff) {
        tiff.order(tiff.get(0) == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        int directory = tiff.getInt(4);
        int entries = tiff.getShort(directory) & 0xFFFF;
        for (int i = 0; i < entries; i++) {
            int entry = directory + 2 + 12 * i;
            if ((tiff.getShort(entry) & 0xFFFF) == ORIENTATION_TAG) {
                int value = tiff.getShort(entry + 8) & 0xFFFF;
                return value >= 1 && value <= 8 ? value : UPRIGHT;
            }
        }

        return UPRIGHT;
    }

    /** The image turned and mirrored as the EXIF orientation says, on white (a drawn PNG may be transparent). */
    private static BufferedImage upright(BufferedImage source, int orientation) {
        int w = source.getWidth();
        int h = source.getHeight();
        AffineTransform transform =
                switch (orientation) {
                    case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
                    case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
                    case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
                    case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
                    case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
                    case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
                    case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
                    default -> new AffineTransform();
                };
        boolean turned = orientation >= 5;
        BufferedImage result = whitePage(turned ? h : w, turned ? w : h);
        Graphics2D g = result.createGraphics();
        g.drawImage(source, transform, null);
        g.dispose();
        return result;
    }

    private static BufferedImage shrink(BufferedImage image) {
        double scale = (double) MAX_LONG_SIDE / Math.max(image.getWidth(), image.getHeight());
        if (scale >= 1) {
            return image;
        }

        int width = (int) Math.round(image.getWidth() * scale);
        int height = (int) Math.round(image.getHeight() * scale);
        BufferedImage result = whitePage(width, height);
        Graphics2D g = result.createGraphics();
        // Area averaging keeps thin pencil strokes that one-pass bilinear scaling would break up.
        g.drawImage(image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g.dispose();
        return result;
    }

    private static BufferedImage whitePage(int width, int height) {
        BufferedImage page = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = page.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return page;
    }

    /** Java's writer adds no metadata of its own beyond the JFIF header. */
    private static void writeJpeg(BufferedImage image, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Files.deleteIfExists(output);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output.toFile())) {
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }
}
