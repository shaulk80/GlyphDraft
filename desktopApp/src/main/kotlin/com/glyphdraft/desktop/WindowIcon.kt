package com.glyphdraft.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.glyphdraft.engine.log.FileLog
import javax.imageio.ImageIO

private const val ICON_RESOURCE = "icon.png"

/** The window and task bar icon from the resources; null (the default Java icon) when it cannot be read. */
fun loadWindowIcon(log: FileLog): Painter? {
    val stream = ClassLoader.getSystemResourceAsStream(ICON_RESOURCE)
    if (stream == null) {
        log.info("window icon $ICON_RESOURCE not found, using the default icon")
        return null
    }

    val image = stream.use { ImageIO.read(it) }
    if (image == null) {
        log.info("window icon $ICON_RESOURCE is not a readable image, using the default icon")
        return null
    }

    return BitmapPainter(image.toComposeImageBitmap())
}
