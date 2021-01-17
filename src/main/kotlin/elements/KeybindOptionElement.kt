package elements

import interaction.*
import logging.Logger
import ocr.*
import settings.KeybindTranslator
import java.awt.*
import java.awt.image.BufferedImage


const val KEYBIND_VALUE_WIDTH = 232
const val KEYBIND_VALUE_GAP = 4

class KeybindOptionElement(name: String) : OptionElement<Keybind>(name) {
    override fun readValue(x: Int, y: Int): Keybind {
        val valueX = x + width - (KEYBIND_VALUE_WIDTH * 2) - KEYBIND_VALUE_GAP

        val capture = takeScreen().capture(valueX, y, (KEYBIND_VALUE_WIDTH * 2) + KEYBIND_VALUE_GAP, height)
            .makeTextReadable()

        val primary = readKeybind(capture.getSubimage(0, 0, KEYBIND_VALUE_WIDTH, height))
        val secondary = readKeybind(capture.getSubimage(KEYBIND_VALUE_WIDTH + KEYBIND_VALUE_GAP, 0, KEYBIND_VALUE_WIDTH, height))

        return Keybind(primary, secondary)
    }

    override fun writeValue(x: Int, y: Int, value: Keybind) {
        val valueX = x + width - (KEYBIND_VALUE_WIDTH * 2) - KEYBIND_VALUE_GAP

        takeMouse().move(valueX + (KEYBIND_VALUE_WIDTH / 2), y + 15).click()
        if (value.primary != null) {
            if (!produceInput(value.primary)) Logger.error("Failed to set primary keybind for <$name>")
        } else {
            takeMouse().move(valueX + KEYBIND_VALUE_WIDTH - 15, y + (height / 2)).click()
        }

        takeMouse().move((valueX + (KEYBIND_VALUE_WIDTH * 1.5) + KEYBIND_VALUE_GAP + 5).toInt(), y + 15).click()
        if (value.secondary != null) {
            if (!produceInput(value.secondary)) Logger.error("Failed to set secondary keybind for <$name>")
        } else {
            takeMouse().move(valueX + (KEYBIND_VALUE_WIDTH * 2) + KEYBIND_VALUE_GAP - 15, y + (height / 2)).click()
        }
    }

    companion object {
        fun String.replaceIllegalCharacters() = replace("é", "3")

        fun produceInput(descriptor: String): Boolean {
            KeybindTranslator.translateMouseWheel(descriptor)?.let {
                takeMouse().wheel(it)
                return true
            }

            KeybindTranslator.translateMouseButton(descriptor)?.let {
                takeMouse().click(it)
                return true
            }

            KeybindTranslator.translateKey(descriptor)?.let {
                takeKeyboard().type(it)
                return true
            }

            Logger.warn("Could not produce input for descriptor <$descriptor>")
            return false
        }

        fun readKeybind(_image: BufferedImage): String? {
            val image = _image.getScaledInstance((KEYBIND_VALUE_WIDTH * 1.1).toInt(), _image.height, Image.SCALE_DEFAULT)
                .toBufferedImage().let {
                    val marginTop = 10
                    val marginLeft = 40
                    it.getSubimage(marginLeft, marginTop, it.width - (marginLeft * 2), it.height - (marginTop * 2))
                }

            var text = image.readText()
            if (text == "-") return null

            if (text.toUpperCase().startsWith("F")) {
                tesseract.setTessVariable("tessedit_char_whitelist", "F0123456789")
                val rescannedText = increaseCharacterDistance(image).debugFile("f-rescan").readText()
                tesseract.setTessVariable("tessedit_char_whitelist", DEFAULT_CHAR_WHITELIST)

                if (rescannedText.length == 2 && rescannedText[0] == 'F') {
                    Logger.debug("Performed F-rescan on $text and got $rescannedText")
                    text = rescannedText
                }
            }

            if (text.length == 1 && text[0].isLowerCase()) {
                tesseract.setTessVariable("tessedit_char_whitelist", SINGLE_CHAR_WHITELIST)
                tesseract.setPageSegMode(10)
                val rescannedText = scaleForSingleCharacter(image).readText()[0].toString()
                tesseract.setTessVariable("tessedit_char_whitelist", DEFAULT_CHAR_WHITELIST)
                tesseract.setPageSegMode(7)

                Logger.debug("Performed single-lowercase-rescan for $text and got $rescannedText")
                text = rescannedText
            }

            return text.takeUnless { it == "-" }?.toLowerCase()
        }

        private fun increaseCharacterDistance(image: BufferedImage): BufferedImage {
            val new = BufferedImage(image.width, image.height, image.type)
            val g = new.createGraphics()

            g.color = Color.WHITE
            g.fillRect(0, 0, new.width, new.height)

            var currentX = 5
            var fillStart = -1
            var fillWidth = 0

            outer@for (x in 0 until image.width) {
                var isColumnFilled = false

                inner@for (y in 0 until image.height) {
                    val color = Color(image.getRGB(x, y), true)
                    if (color.red < 255 || color.green < 255 || color.blue < 255) {
                        isColumnFilled = true
                        break@inner
                    }
                }

                if (isColumnFilled) {
                    if (fillStart == -1) {
                        fillStart = x
                    }
                    fillWidth++
                } else if (fillStart != -1) {
                    g.drawImage(image.getSubimage(fillStart, 0, fillWidth, image.height), currentX, 0, null)
                    currentX += fillWidth + 10

                    fillStart = -1
                    fillWidth = 0
                }
            }

            g.dispose()
            return new
        }

        private fun scaleForSingleCharacter(image: BufferedImage): BufferedImage {
            val width = 30
            val subimage = image.getSubimage(image.width / 2 - width / 2, 0, width, image.height)

            val targetWidth = (subimage.width * 2.5).toInt()
            val targetHeight = (subimage.height * 2.1).toInt()

            val resizedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
            val graphics2D = resizedImage.createGraphics()
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics2D.drawImage(subimage, 0, 0, targetWidth, targetHeight, null)
            graphics2D.dispose()
            return resizedImage
        }
    }
}

data class Keybind(val primary: String?, val secondary: String?)