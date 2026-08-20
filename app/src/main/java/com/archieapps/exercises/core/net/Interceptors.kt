package com.archieapps.exercises.core.net

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class BaseUrlInterceptor(private val baseUrl: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val target = baseUrl()?.toHttpUrlOrNull() ?: return chain.proceed(request)

        return chain.proceed(request.newBuilder().url(rewrite(request.url, target)).build())
    }

    private fun rewrite(original: HttpUrl, target: HttpUrl): HttpUrl =
        original.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .encodedPath(target.encodedPath.trimEnd('/') + original.encodedPath)
            .build()
}

class SessionInterceptor(
    private val token: () -> String?,
    private val safeCode: () -> String = SafeCode::current,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder().header("Accept", "application/json")

        token()?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }

        if (chain.request().method != "GET") {
            builder.header("code", safeCode())
        }

        return chain.proceed(builder.build())
    }
}
