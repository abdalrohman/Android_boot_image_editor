package cfig.lazybox.sysinfo

import cfig.helper.Helper
import cfig.helper.Helper.Companion.check_call
import cfig.helper.Helper.Companion.deleteIfExists
import cfig.helper.ZipHelper
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

class SysInfo {
    private fun runAndWrite(cmd: String, outStream: OutputStream, check: Boolean) {
        Helper.powerRun2(cmd, null).let {
            if (it[0] as Boolean) {
                outStream.write(it[1] as ByteArray)
            } else {
                if (check) {
                    log.warn(String(it[1] as ByteArray))
                    throw RuntimeException(String(it[2] as ByteArray))
                } else {
                    log.warn(String(it[1] as ByteArray))
                    log.warn(String(it[2] as ByteArray))
                }
            }
        }
    }

    private fun makeTar(tarFile: String, srcDir: String) {
        val pyScript =
            """
import os, sys, subprocess, gzip, logging, shutil, tarfile, os.path
def makeTar(output_filename, source_dir):
   with tarfile.open(output_filename, "w:xz") as tar:
       tar.add(source_dir, arcname=os.path.basename(source_dir))
makeTar("%s", "%s")
""".trim()
        val tmp = Files.createTempFile(Paths.get("."), "xx.", ".yy")
        tmp.writeText(String.format(pyScript, tarFile, srcDir))
        ("python " + tmp.fileName).check_call()
        tmp.deleteIfExists()
    }

    fun run() {
        "adb wait-for-device".check_call()
        "adb root".check_call()
        "sysinfo.tar".deleteIfExists()
        val prefix = "sysinfo"
        File("sysinfo").let {
            if (it.exists()) {
                log.info("purging directory sysinfo/ ...")
                it.deleteRecursively()
            }
        }
        File(prefix).mkdir()
        FileOutputStream("$prefix/0_prop").use {
            runAndWrite("adb shell getprop", it, true)
        }
        FileOutputStream("$prefix/1_partitions").use { file ->
            runAndWrite("adb shell cat /proc/partitions", file, false) //HMOS
            runAndWrite("adb shell ls -l /dev/block/by-name", file, false)
        }

        FileOutputStream("$prefix/2_mount").use { file ->
            runAndWrite("adb shell mount", file, true)
        }
        FileOutputStream("$prefix/3_kernel_cmdline").use { file ->
            file.write("[version]\n".toByteArray())
            runAndWrite("adb shell cat /proc/version", file, true)
            file.write("\n[cmdline]\n".toByteArray())
            runAndWrite("adb shell cat /proc/cmdline", file, false)
            file.write("\n[bootconfig]\n".toByteArray())
            runAndWrite("adb shell cat /proc/bootconfig", file, false)
            // cpuinfo
            file.write("\n[cpuinfo]\n".toByteArray())
            runAndWrite("adb shell cat /proc/cpuinfo", file, false)
            // meminfo
            file.write("\n[meminfo]\n".toByteArray())
            runAndWrite("adb shell cat /proc/meminfo", file, false)
            // defconfig
            file.write("\n[defconfig]\n".toByteArray())
            "adb pull /proc/config.gz".check_call()
            ZipHelper.zcat("config.gz", "config")
            file.write(File("config").readBytes())
            File("config.gz").deleteOnExit()
            File("config").deleteOnExit()
        }
        "adb pull /proc/device-tree".check_call(prefix)
        Files.move(Paths.get("$prefix/device-tree"), Paths.get("$prefix/device_tree"))
        makeTar("sysinfo.tar.xz", "sysinfo")
        File("sysinfo").deleteRecursively()
    }

    companion object {
        private val log = LoggerFactory.getLogger(SysInfo::class.java)
    }
}