package org.nano.updater.util

import android.content.Context
import com.topjohnwu.superuser.ShellUtils
import org.nano.updater.ui.home.HomeViewModel
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FileUtils {

    fun getIsUpdateVerified(context: Context, homeViewModel: HomeViewModel, position: Int): Boolean {
        val updateData = homeViewModel.getUpdateData().value!!
        return verifyChecksum(
            updateFile = getUpdatePackage(
                context,
                homeViewModel.getUpdatePackageName(if (position == 1) updateData.kernel else updateData.updater),
                position == 1
            ),
            referenceChecksum = homeViewModel.getUpdateMD5Checksum(if (position == 1) updateData.kernel else updateData.updater)
        )
    }

    fun getUpdatePackage(context: Context, fileName: String, isKernelUpdate: Boolean): File {
        val updateType = if (isKernelUpdate)
            "kernel"
        else
            "updater"
        return File(
            context.getExternalFilesDir(updateType),
            fileName
        )
    }

    fun verifyChecksum(updateFile: File, referenceChecksum: String): Boolean {
        if (!updateFile.exists())
            return false
        val isChecksumVerified = ShellUtils.checkSum("MD5", updateFile, referenceChecksum)
        if (!isChecksumVerified)
            updateFile.delete()
        return isChecksumVerified
    }

    fun unzip(zipFile: String, location: String) {
        val f = File(location)
        if (!f.isDirectory) {
            f.mkdirs()
        }
        val zin = ZipInputStream(FileInputStream(zipFile))
        zin.use { zin ->
            var ze: ZipEntry? = zin.nextEntry
            while (ze != null) {
                val path = location + File.separator + ze.name
                if (ze.isDirectory) {
                    val unzipFile = File(path)
                    if (!unzipFile.isDirectory) {
                        unzipFile.mkdirs()
                    }
                } else {
                    val fout = FileOutputStream(path, false)
                    val bout = BufferedOutputStream(fout)

                    fout.use {
                        val b = ByteArray(1024)
                        var n = zin.read(b, 0, 1024)
                        while (n >= 0) {
                            bout.write(b, 0, n)
                            n = zin.read(b, 0, 1024)
                        }
                        zin.closeEntry()
                        bout.close()
                    }
                }
                ze = zin.nextEntry
            }
        }
    }
}