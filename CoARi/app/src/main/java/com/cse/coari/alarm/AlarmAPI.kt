package com.cse.coari.alarm

import com.cse.coari.data.AlarmDTO
import com.cse.coari.retrofit.CoariApi
import io.reactivex.Observable
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface AlarmAPI {
    @GET("/api/alarms/search")
    fun getAlarms(): Observable<AlarmDTO>

    @DELETE("/api/alarms/{alarm_id}")
    fun deleteAlarm(@Path("alarm_id")id: String) : Observable<AlarmDTO>

    companion object{
        fun create(): AlarmAPI {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            val headerInterceptor = Interceptor {
                val request = it.request()
                    .newBuilder()
                    .build()
                return@Interceptor it.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(headerInterceptor)
                .addInterceptor(httpLoggingInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(CoariApi.DOMAIN)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(AlarmAPI::class.java)
        }
    }
}