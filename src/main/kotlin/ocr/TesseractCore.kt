package ocr

import logging.Logger
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import java.awt.Image

const val DEFAULT_CHAR_WHITELIST = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-. "
const val SINGLE_CHAR_WHITELIST = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

val tesseract: Tesseract = Tesseract().apply {
    setDatapath("src/main/resources")
    setTessVariable("user_defined_dpi", "300")
    setTessVariable("tessedit_char_whitelist", DEFAULT_CHAR_WHITELIST)
}

fun BufferedImage.makeTextReadable() = invert().blackWhite()

fun BufferedImage.readText(): String {
    Logger.debug("Performing OCR on buffered image")
    return tesseract.doOCR(this).removeSuffix("\n")
}

fun Image.toBufferedImage(): BufferedImage {
    if (this is BufferedImage) {
        return this
    }

    val bufferedImage = BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)
    val bGr = bufferedImage.createGraphics()
    bGr.drawImage(this, 0, 0, null)
    bGr.dispose()

    return bufferedImage
}