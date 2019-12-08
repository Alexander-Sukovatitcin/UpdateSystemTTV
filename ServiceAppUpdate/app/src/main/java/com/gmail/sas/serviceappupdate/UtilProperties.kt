package com.gmail.sas.serviceappupdate

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.util.*

class UtilProperties {

    fun getConfigSd(): Properties {
        val property = Properties()
        try {
            var fis = FileInputStream("/storage/self/primary/configSd.txt")
            property.load(fis)

        } catch (e: Exception) {
            Log.d("properties", e.message)
        }
        return property
    }


    fun getConfigInternal(): Properties {
        val property = Properties()
        try {
            var fis = FileInputStream(
                File(
                    Environment.getDataDirectory(),
                    "/storage/self/primary/configInternal.txt"
                )
            )
            property.load(fis)

        } catch (e: Exception) {
            Log.d("properties", e.message)
        }
        return property
    }
}