package com.gmail.sas.serviceappupdate

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface Api {

    @GET("/up2.php?")
    fun checkUpdate(
        @Query("ser") serial: String,
        @Query("ver") ver: String,
        @Query("t") t: String
    ): Single<ResponseBody>

    @POST("/upload/{name}")
    fun loadTarArchive(@Path("name") name: String): Single<ResponseBody>

    @GET("/md5.php?")
    fun getMd5SumForPacket(@Query("md5") md5: String): Single<ResponseBody>

}