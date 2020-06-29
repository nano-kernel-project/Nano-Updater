package org.nano.updater.network

import org.nano.updater.model.NanoUpdate
import retrofit2.Call
import retrofit2.http.GET

interface UpdateService {
    @GET("dev/test.json")
    fun getUpdateData(): Call<NanoUpdate>

}
