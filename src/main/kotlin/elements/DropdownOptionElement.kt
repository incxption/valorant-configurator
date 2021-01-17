package elements

import interaction.takeMouse
import interaction.takeScreen
import logging.Logger
import java.awt.Color

const val DROPDOWN_TEXT_WIDTH = 440
const val DROPDOWN_VALUE_WIDTH = 468
const val DROPDOWN_ITEM_HEIGHT = 18
const val READABLE_DROPDOWN_ITEMS = 10

class DropdownOptionElement(name: String) : OptionElement<Int>(name) {
    override fun readValue(x: Int, y: Int): Int {
        val valueX = x + width - DROPDOWN_VALUE_WIDTH

        takeMouse().move(valueX + 1, y + 1).click()
        val capture = takeScreen().capture(valueX, y, DROPDOWN_TEXT_WIDTH, height + READABLE_DROPDOWN_ITEMS * DROPDOWN_ITEM_HEIGHT)

        for (i in 0..READABLE_DROPDOWN_ITEMS) {
            val itemY = height + (i * DROPDOWN_ITEM_HEIGHT)
            val color = Color(capture.getRGB(DROPDOWN_TEXT_WIDTH - 1, itemY + 1), true)

            if (color.isSelectedColor()) {
                takeMouse().hideCursor()
                return i
            }
        }

        Logger.warn("Cannot read value of dropdown option $name")
        takeMouse().hideCursor()
        return 0
    }

    override fun writeValue(x: Int, y: Int, value: Int) {
        val valueX = x + width - DROPDOWN_VALUE_WIDTH
        takeMouse()
            .move(valueX + 1, y + 1).click()
            .move(valueX + 1, y + height + (value * DROPDOWN_ITEM_HEIGHT) + 1).click()
    }
}

private fun Color.isSelectedColor(): Boolean =
    red >= 220 && green >= 220 && blue <= 175