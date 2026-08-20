package com.archieapps.exercises.core.net

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response

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

class SlowCallInterceptor(
    private val paths: List<String>,
    private val readTimeoutSeconds: Int,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath

        if (paths.none { path.contains(it) }) {
            return chain.proceed(chain.request())
        }

        return chain
            .withReadTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .proceed(chain.request())
    }
}
