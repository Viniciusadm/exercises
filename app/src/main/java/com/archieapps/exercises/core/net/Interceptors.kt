package com.archieapps.exercises.core.net

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
