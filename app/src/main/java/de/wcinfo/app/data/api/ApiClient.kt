package de.wcinfo.app.data.api

import de.wcinfo.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ApiClient {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore.getOrPut(url.host) { mutableListOf() }.apply {
                val newNames = cookies.map { it.name }.toSet()
                removeAll { it.name in newNames }
                addAll(cookies)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            val list = cookieStore[url.host] ?: return emptyList()
            list.removeAll { it.expiresAt < now }
            return list.toList()
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")

        // Add API Key
        val apiKey = BuildConfig.API_KEY
        if (apiKey.isNotEmpty()) {
            builder.header("X-Api-Key", apiKey)
            builder.header("Authorization", "Bearer $apiKey")
        }

        // Add CSRF token if cookie present
        val cookies = cookieJar.loadForRequest(original.url)
        val xsrfCookie = cookies.firstOrNull { it.name == "XSRF-TOKEN" }
        if (xsrfCookie != null) {
            val decodedToken = java.net.URLDecoder.decode(xsrfCookie.value, "UTF-8")
            builder.header("X-XSRF-TOKEN", decodedToken)
        }

        var response = chain.proceed(builder.build())

        // 419 Session Expired handling: Call /health once and retry
        if (response.code == 419) {
            response.close()
            val healthUrl = original.url.newBuilder().encodedPath("/health").query(null).build()
            val healthReq = Request.Builder().url(healthUrl).get().build()
            chain.proceed(healthReq).close()

            // Retry original request
            response = chain.proceed(builder.build())
        }

        response
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val apiService: WCInfoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WCInfoApiService::class.java)
    }
}
