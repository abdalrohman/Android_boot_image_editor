package cfig.lazybox

import cfig.lazybox.sysinfo.CpuInfo
import cfig.lazybox.sysinfo.SysInfo
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

class App

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger(App::class.java)
    if (args.isEmpty()) {
        println("Usage: args: (Array<String>) ...")
        println("   or: function [arguments]...")
        println("\nCurrently defined functions:")
        println("\tcpuinfo sysinfo")
        exitProcess(0)
    }
    if (args.get(0) == "cpuinfo") {
        val ret = CpuInfo.construct()
        File("cpuinfo.json").writeText(
            ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(ret)
        )
        log.info("cpuinfo.json is ready")
    }
    if (args.get(0) == "sysinfo") {
        SysInfo().run()
    }
}
