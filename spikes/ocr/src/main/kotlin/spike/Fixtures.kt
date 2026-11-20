package spike

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import java.util.Random
import javax.imageio.ImageIO

/** How a rendered page is degraded to imitate a photo. */
enum class Degrade { None, Skew, Noise, BlurLowContrast }

class FixtureSpec(
    val name: String,
    val language: String,
    val script: String,
    val fontFile: String,
    val lines: List<String>,
    val degrade: Degrade = Degrade.None,
    val fontSize: Int = 34,
)

class Fixture(val name: String, val language: String, val script: String, val image: File, val expected: String)

object Fixtures {
    val dir = File("engine/src/test/resources/fixtures")
    private const val FONTS = "C:/Windows/Fonts/"

    // All sentences are written for this project or are public-domain phrases (pangrams, a line from Soseki, 1905).
    val specs = listOf(
        FixtureSpec("en-clean", "en", "Latin", "arial.ttf", listOf(
            "The quick brown fox jumps over the lazy dog.",
            "Meeting moved to Thursday at 10:30, room 4B.",
            "Invoice 2026-1187 totals 1,249.50 EUR.")),
        FixtureSpec("de-clean", "de", "Latin", "times.ttf", listOf(
            "Zwölf Boxkämpfer jagen Viktor quer über den großen Sylter Deich.",
            "Die Besprechung beginnt am Donnerstag um 10:30 Uhr.",
            "Bitte bringen Sie Ihren Ausweis mit.")),
        FixtureSpec("ru-clean", "ru", "Cyrillic", "arial.ttf", listOf(
            "Съешь же ещё этих мягких французских булок, да выпей чаю.",
            "Собрание перенесено на четверг, кабинет 4Б.",
            "Пожалуйста, возьмите с собой паспорт.")),
        FixtureSpec("zh-clean", "zh", "Han", "msyh.ttc", listOf(
            "今天天气很好，我们一起去公园散步。",
            "会议改到星期四上午十点半，在四号会议室。",
            "请携带您的身份证件。"), fontSize = 36),
        FixtureSpec("ja-clean", "ja", "Japanese", "YuGothM.ttc", listOf(
            "吾輩は猫である。名前はまだ無い。",
            "会議は木曜日の午前十時半に変更されました。",
            "身分証明書をお持ちください。"), fontSize = 36),
        FixtureSpec("he-clean", "he", "Hebrew", "arial.ttf", listOf(
            "השועל החום המהיר קופץ מעל הכלב העצלן.",
            "הפגישה נדחתה ליום חמישי בשעה עשר וחצי.",
            "אנא הביאו איתכם תעודת זהות.")),
        FixtureSpec("mixed-clean", "en+ru", "Latin+Cyrillic", "arial.ttf", listOf(
            "Hello, мир! This sign reads Добро пожаловать.",
            "Exit / Выход, gate 7 / ворота 7.")),
        FixtureSpec("en-skew", "en", "Latin", "arial.ttf", listOf(
            "Photographed receipt: total due 58.20 USD.",
            "Thank you for shopping with us today.",
            "Returns accepted within 30 days."), Degrade.Skew),
        FixtureSpec("ru-noise", "ru", "Cyrillic", "times.ttf", listOf(
            "Расписание поездов изменено с понедельника.",
            "Касса работает с восьми до двадцати часов.",
            "Справки по телефону в зале ожидания."), Degrade.Noise),
        FixtureSpec("de-blur", "de", "Latin", "arial.ttf", listOf(
            "Straße gesperrt wegen Bauarbeiten bis Freitag.",
            "Umleitung über die Hauptstraße ist ausgeschildert.",
            "Wir danken für Ihr Verständnis."), Degrade.BlurLowContrast),
        FixtureSpec("zh-noise", "zh", "Han", "msyh.ttc", listOf(
            "前方施工，请绕行。",
            "营业时间每天上午九点至下午六点。"), Degrade.Noise, fontSize = 36),
    )

    fun generate() {
        dir.mkdirs()
        val manifest = StringBuilder("name\tlanguage\tscript\n")
        for (spec in specs) {
            ImageIO.write(render(spec), "png", File(dir, "${spec.name}.png"))
            File(dir, "${spec.name}.txt").writeText(spec.lines.joinToString("\n") + "\n", Charsets.UTF_8)
            manifest.append("${spec.name}\t${spec.language}\t${spec.script}\n")
        }
        File(dir, "manifest.tsv").writeText(manifest.toString(), Charsets.UTF_8)
        println("Wrote ${specs.size} fixtures to ${dir.absolutePath}")
    }

    fun load(): List<Fixture> {
        val rows = File(dir, "manifest.tsv").readLines(Charsets.UTF_8).drop(1).filter { it.isNotBlank() }
        return rows.map { row ->
            val (name, language, script) = row.split('\t')
            Fixture(name, language, script, File(dir, "$name.png"), File(dir, "$name.txt").readText(Charsets.UTF_8))
        }
    }

    private fun render(spec: FixtureSpec): BufferedImage {
        val font = Font.createFont(Font.TRUETYPE_FONT, File(FONTS + spec.fontFile)).deriveFont(spec.fontSize.toFloat())
        val lineHeight = (spec.fontSize * 1.9).toInt()
        val width = 1100
        val height = lineHeight * spec.lines.size + 80
        val page = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = page.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        g.color = Color(30, 30, 30)
        g.font = font
        spec.lines.forEachIndexed { index, line ->
            val rightToLeft = spec.script == "Hebrew"
            val x = if (rightToLeft) width - 40 - g.fontMetrics.stringWidth(line) else 40
            g.drawString(line, x, 40 + lineHeight * index + spec.fontSize)
        }
        g.dispose()
        return degrade(page, spec.degrade)
    }

    private fun degrade(page: BufferedImage, how: Degrade): BufferedImage = when (how) {
        Degrade.None -> page
        Degrade.Skew -> rotate(page, 3.5)
        Degrade.Noise -> addNoise(rotate(page, -1.5), 28.0)
        Degrade.BlurLowContrast -> lowContrast(blur(page))
    }

    private fun rotate(page: BufferedImage, degrees: Double): BufferedImage {
        val out = BufferedImage(page.width, page.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.color = Color(245, 245, 245)
        g.fillRect(0, 0, out.width, out.height)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(page, AffineTransform.getRotateInstance(Math.toRadians(degrees), page.width / 2.0, page.height / 2.0), null)
        g.dispose()
        return out
    }

    private fun addNoise(page: BufferedImage, sigma: Double): BufferedImage {
        val random = Random(42)
        for (y in 0 until page.height) {
            for (x in 0 until page.width) {
                val rgb = page.getRGB(x, y)
                val delta = (random.nextGaussian() * sigma).toInt()
                val r = (((rgb shr 16) and 0xFF) + delta).coerceIn(0, 255)
                val gr = (((rgb shr 8) and 0xFF) + delta).coerceIn(0, 255)
                val b = ((rgb and 0xFF) + delta).coerceIn(0, 255)
                page.setRGB(x, y, (r shl 16) or (gr shl 8) or b)
            }
        }
        return page
    }

    private fun blur(page: BufferedImage): BufferedImage {
        val size = 3
        val kernel = Kernel(size, size, FloatArray(size * size) { 1f / (size * size) })
        return ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null).filter(page, null)
    }

    private fun lowContrast(page: BufferedImage): BufferedImage {
        for (y in 0 until page.height) {
            for (x in 0 until page.width) {
                val rgb = page.getRGB(x, y)
                val r = 70 + (((rgb shr 16) and 0xFF) * 0.55).toInt()
                val gr = 70 + (((rgb shr 8) and 0xFF) * 0.55).toInt()
                val b = 70 + ((rgb and 0xFF) * 0.55).toInt()
                page.setRGB(x, y, (r shl 16) or (gr shl 8) or b)
            }
        }
        return page
    }
}
