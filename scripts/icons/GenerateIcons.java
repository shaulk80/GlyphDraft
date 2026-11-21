import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Draws the GlyphDraft desktop icon (PNG and Windows ICO). The same geometry is written by hand in
 * androidApp/src/main/res/drawable/ic_launcher_foreground.xml; change both together.
 *
 * Run from the repository root: java scripts/icons/GenerateIcons.java
 * Writes desktopApp/icons/glyphdraft.png, desktopApp/icons/glyphdraft.ico and desktopApp/src/main/resources/icon.png.
 */
public class GenerateIcons {
    // The drawing is made on a 108 x 108 grid, like an Android adaptive icon layer.
    private static final double GRID = 108.0;
    private static final Color BACKGROUND = new Color(0x0F4C5C);
    private static final Color FRAME = Color.WHITE;
    private static final Color GLYPH = new Color(0xFFC857);
    private static final double MARK_SCALE = 1.3;
    private static final int[] ICO_SIZES = {16, 24, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws IOException {
        Path icons = Path.of("desktopApp", "icons");
        Files.createDirectories(icons);
        BufferedImage large = render(512);
        ImageIO.write(large, "png", icons.resolve("glyphdraft.png").toFile());
        Path resources = Path.of("desktopApp", "src", "main", "resources");
        Files.createDirectories(resources);
        ImageIO.write(large, "png", resources.resolve("icon.png").toFile());
        writeIco(icons.resolve("glyphdraft.ico"));
    }

    private static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(BACKGROUND);
        g.fill(new RoundRectangle2D.Double(0, 0, size, size, size * 0.22, size * 0.22));

        double scale = size / GRID * MARK_SCALE;
        double offset = size / 2.0 - GRID / 2.0 * scale;
        g.transform(new AffineTransform(scale, 0, 0, scale, offset, offset));
        g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.setColor(FRAME);
        g.draw(corner(31, 31, 1, 1));
        g.draw(corner(77, 31, -1, 1));
        g.draw(corner(31, 77, 1, -1));
        g.draw(corner(77, 77, -1, -1));

        g.setColor(GLYPH);
        g.draw(glyph());
        g.dispose();
        return image;
    }

    /** One scan-frame corner: from (x, y) ten units along each edge, directions given by the signs. */
    private static Path2D corner(double x, double y, int dx, int dy) {
        Path2D path = new Path2D.Double();
        path.moveTo(x, y + dy * 10);
        path.lineTo(x, y);
        path.lineTo(x + dx * 10, y);
        return path;
    }

    /** A "G": an arc of radius 14 around (54, 54) open on the right, and the bar going left from its lower end. */
    private static Path2D glyph() {
        Path2D path = new Path2D.Double();
        path.append(new Arc2D.Double(40, 40, 28, 28, 40, 320, Arc2D.OPEN), false);
        path.lineTo(56, 54);
        return path;
    }

    private static void writeIco(Path target) throws IOException {
        int count = ICO_SIZES.length;
        byte[][] images = new byte[count][];
        for (int i = 0; i < count; i++) {
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(render(ICO_SIZES[i]), "png", bytes);
            images[i] = bytes.toByteArray();
        }
        int offset = 6 + 16 * count;
        ByteBuffer header = ByteBuffer.allocate(offset).order(ByteOrder.LITTLE_ENDIAN);
        header.putShort((short) 0).putShort((short) 1).putShort((short) count);
        for (int i = 0; i < count; i++) {
            int size = ICO_SIZES[i];
            header.put((byte) (size >= 256 ? 0 : size)).put((byte) (size >= 256 ? 0 : size));
            header.put((byte) 0).put((byte) 0).putShort((short) 1).putShort((short) 32);
            header.putInt(images[i].length).putInt(offset);
            offset += images[i].length;
        }
        try (OutputStream out = Files.newOutputStream(target)) {
            out.write(header.array());
            for (byte[] image : images) {
                out.write(image);
            }
        }
    }
}
