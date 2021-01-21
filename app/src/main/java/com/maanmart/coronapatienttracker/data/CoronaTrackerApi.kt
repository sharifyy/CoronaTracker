package com.maanmart.coronapatienttracker.data

import com.google.gson.GsonBuilder
import com.maanmart.coronapatienttracker.shared.dto.CheckPatientsLocationResponse
import com.maanmart.coronapatienttracker.shared.dto.LoginResponse
import com.maanmart.coronapatienttracker.shared.dto.SavePersonLocationResponse
import com.maanmart.coronapatienttracker.shared.dto.SettingResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

interface CoronaTrackerApi {

    @POST("PersonLogin")
    @FormUrlEncoded
    suspend fun login(
        @Field("IndentityCode") identityCode: String,
        @Field("Mobile") mobile: String
    ): LoginResponse

    @GET("GetSetting")
    suspend fun getSettings(): SettingResponse


    @POST("SavePersonLocation")
    @FormUrlEncoded
    suspend fun sendLocations(
        @Field("PersonId") personId: String,
        @Field("Locations") locations: String
    ): SavePersonLocationResponse


    @POST("CheckProbabilityOfContact")
    @FormUrlEncoded
    suspend fun checkCoronaPatientsLocation(
            @Field("PersonId") personId: String,
    ): CheckPatientsLocationResponse

    // استفاده از رتروفیت جهت ارتباط با سرور
    companion object {
        private const val BASE_URL = "https://maanmarket.com/api/v1.0/Services/"
        operator fun invoke(
//            connectivityInterceptor: ConnectivityInterceptor,
//            tokenInterceptor: TokenInterceptor
        ): CoronaTrackerApi {

            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY


            val okHttpClient =
//                OkHttpClient.Builder()
//                .addInterceptor(authInterceptor)
//                .addInterceptor(tokenInterceptor)
                UnsafeOkHttpClient.unsafeOkHttpClient
                .addInterceptor(httpLoggingInterceptor)
//                .addInterceptor(connectivityInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BASE_URL)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
                    )
                )
//                .addConverterFactory(Json.nonstrict.asConverterFactory(MediaType.get("application/json")))
//                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
                .create(CoronaTrackerApi::class.java)
        }
    }
}

// ایجاد کلاینت http و غیر فعال کردن اعتبار سنجی TLS
object UnsafeOkHttpClient {
    // Create a trust manager that does not validate certificate chains
    val unsafeOkHttpClient:

    // Install the all-trusting trust manager

    // Create an ssl socket factory with our all-trusting manager
            OkHttpClient.Builder
        get() = try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts: Array<TrustManager> = arrayOf(object :X509TrustManager{
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {

                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }  )

            // Install the all-trusting trust manager
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { hostname, session -> true }
            builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
}