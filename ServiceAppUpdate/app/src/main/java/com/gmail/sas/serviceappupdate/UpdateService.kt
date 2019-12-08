package com.gmail.sas.serviceappupdate

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.*
import java.util.concurrent.TimeUnit


class UpdateService : Service() {

    val TAG = UpdateService::class.java.canonicalName
    val compositeDisposable = CompositeDisposable()

    lateinit var host: String
    lateinit var serial: String
    lateinit var mkid: String
    lateinit var ver: String
    lateinit var network: Network
    lateinit var tempFolderScript: String
    lateinit var tempFolder: String
    lateinit var folderForCopy: String
    var namePackUpdate = ""

    override fun onBind(p0: Intent?): IBinder? {
        throw  UnsupportedOperationException("Not yet implemented")
    }


    override fun onCreate() {
        super.onCreate()

        if (initialize()) {
            val method = network.api.checkUpdate(serial, ver, "and")
                .subscribeOn(Schedulers.io())
                .delay(1, TimeUnit.MINUTES)
                .retryWhen { it.repeat() }
                .repeat()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { body -> handlerResponse(body.string()) },
                    { Log.d(TAG, "Error::${it.message}") })

            compositeDisposable.add(method)
        }

    }


    private fun initialize(): Boolean {
        val configSd = UtilProperties().getConfigSd()
        folderForCopy = configSd.getProperty("folderForCopy")
        tempFolder = configSd.getProperty("tempFolder")
        tempFolderScript = configSd.getProperty("tempFolderScript")
        host = configSd.getProperty("host")
        mkid = configSd.getProperty("mkid")
        serial = "${getSDCARDiD()}_($mkid)"
        ver = configSd.getProperty("ver")
        return if (checkInitialize()) {
            Log.d(TAG, "$host, $serial, $mkid, $ver")
            network = Network(host)
            true
        } else false

    }

    private fun checkInitialize(): Boolean {

        return ::host.isInitialized && ::serial.isInitialized
                && ::mkid.isInitialized && ::ver.isInitialized && ::folderForCopy.isInitialized
                && ::tempFolder.isInitialized && ::tempFolderScript.isInitialized

    }

    private fun getSDCARDiD(): String {
        var sd_cid: String? = null
        try {
            val file = File("/sys/block/mmcblk1")
            val memBlk: String
            memBlk = if (file.exists() && file.isDirectory) {
                "mmcblk1"
            } else { //System.out.println("not a directory");
                "mmcblk0"
            }
            val cmd =
                Runtime.getRuntime().exec("cat /sys/block/$memBlk/device/cid")
            val br = BufferedReader(InputStreamReader(cmd.inputStream))
            sd_cid = br.readLine()
            //System.out.println(sd_cid);
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sd_cid.toString()
    }

    private fun handlerResponse(msg: String) {
        when (msg) {
            "ok" -> {
                Log.d(TAG, "Response::OK")
            }
            "add" -> {
                Log.d(TAG, "Response::ADD on SERVER")
            }
            else -> {
                if (msg.contains("update")) {
                    Log.d(TAG, "Response::UPDATE $msg")
                    loadUpdateArchive(msg.drop(9))
                }
            }
        }
    }

    private fun loadUpdateArchive(namePacket: String) {
        namePackUpdate = "$namePacket.tar"
        val method = network.api.loadTarArchive(namePackUpdate)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ body ->
                validateMd5Sum((handlerResponseFile(body)))
            },
                {})
        compositeDisposable.add(method)
    }


    private fun validateMd5Sum(md5FileSum: String) {
        val method = network.api.getMd5SumForPacket(namePackUpdate.dropLast(4))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ body ->
                if (md5FileSum == body.string()) {
                    Log.d(TAG, "Md5 valid")
                    TarDecomposes("$filesDir/$tempFolder").apply {
                        tarDecomposesStart("$filesDir/$namePackUpdate")
                        runPreCmdScript()
                        copyUpdFolder()
                        runCmdScript()
                    }
                } else {
                    Log.d(TAG, "Md5 not valid")
                }
            }, {})
        compositeDisposable.add(method)
    }

    private fun handlerResponseFile(body: ResponseBody): String {

        val filePacket = File(filesDir, namePackUpdate)
        try {
            val fileReader = ByteArray(4096)
            val fileSize = body.contentLength()
            var fileSizesDownload = 0

            val inp = body.byteStream()
            val out = FileOutputStream(filePacket)

            while (true) {

                val read = inp.read(fileReader)

                if (read == -1) {
                    break
                }

                out.write(fileReader, 0, read)
                fileSizesDownload += read
                Log.d(TAG, "file download: $fileSizesDownload of $fileSize")
            }

            out.flush()
            val fileInp = FileInputStream(filePacket)
            return String(Hex.encodeHex(DigestUtils.md5(fileInp)))
        } catch (e: Exception) {
            return ""
        }
    }

    private fun runPreCmdScript() {
        val precmd = Runtime.getRuntime().exec("sh $filesDir/${tempFolderScript}precmd.sh")
        val inBuf = BufferedReader(InputStreamReader(precmd.inputStream))
    }

    private fun runCmdScript() {
        val cmd = Runtime.getRuntime().exec("sh $filesDir/${tempFolderScript}cmd.sh")
        val inBuf = BufferedReader(InputStreamReader(cmd.inputStream))
    }

    private fun copyUpdFolder() {
        val file = File("$filesDir/${tempFolderScript.dropLast(1)}")
        file.copyRecursively(File(folderForCopy), true)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}
