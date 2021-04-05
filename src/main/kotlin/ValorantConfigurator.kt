@file:JvmName("ValorantConfigurator")

import cli.CommandLineOptions
import cli.Mode
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import display.Display
import interaction.EmergencyBrake
import interaction.WindowsHook
import logging.Logger
import settings.*
import kotlin.system.exitProcess

fun main(args: Array<String>) = mainBody<Unit> {
    val options = ArgParser(args).parseInto(::CommandLineOptions)

    try {
        if (!options.instant) {
            Thread.sleep(5_000)
        }

        if (options.focus) {
            if (WindowsHook.focusValorant()) {
                Thread.sleep(3_000)
                Logger.info("Focused Valorant client using the Windows API")
            } else {
                Logger.error("Couldn't focus the Valorant client!")
                exitProcess(3)
            }
        }

        val structure = parseStructureFromXML()
        EmergencyBrake.init()
        Display.init()

        when (options.mode) {
            Mode.READ -> readSettings(structure, options.configurationFile)
            Mode.WRITE -> writeSettings(structure, options.configurationFile)
        }

        Logger.info("Finished!")
        exitProcess(0)
    } catch (e: Exception) {
        Logger.error("An error occurred while executing the Valorant Configurator")
        e.printStackTrace()
        exitProcess(1)
    }
}