package com.gmail.sas.serviceappupdate

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.*

class TarDecomposes(filesDir: String) {

    val tempDir = filesDir
    var flag = true
    fun tarDecomposesStart(tarFileSource: String) {
        val inp = BufferedInputStream(FileInputStream(tarFileSource))
        val tis = TarArchiveInputStream(inp)
        decomposes(tis)
    }

    private fun decomposesDir(tis: TarArchiveInputStream, entry: TarArchiveEntry) {
        val file = File("$tempDir/${entry.name}")
        file.mkdirs()
        var bodyEntry: TarArchiveEntry? = null
        var flagDir = true

        while (flagDir || bodyEntry == null) {
            flagDir = false
            bodyEntry = tis.nextTarEntry

            if (bodyEntry.isDirectory) {
                decomposesDir(tis, bodyEntry)
            }
            if (bodyEntry.isFile) {
                decomposesFile(tis, bodyEntry)
            }
        }

    }

    private fun decomposesFile(tis: TarArchiveInputStream, entry: TarArchiveEntry) {
        val file = File(tempDir, entry.name)
        val fos = FileOutputStream(file)
        val dest = BufferedOutputStream(fos)
        val bytes = ByteArray(entry.size.toInt())
        tis.read(bytes)
        dest.write(bytes)
        dest.flush()
        dest.close()
        flag = true
    }

    private fun decomposes(tis: TarArchiveInputStream) {
        var entry: TarArchiveEntry? = null
        while (flag || entry == null) {
            flag = false
            entry = tis.nextTarEntry

            if (entry == null) {
                flag = false
                break
            }

            if (entry.isDirectory) {
                decomposesDir(tis, entry)

            }

            if (entry.isFile) {
                decomposesFile(tis, entry)
            }
        }
    }

}