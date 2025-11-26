package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cloud.Request.Method.GET
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface NetworkConnectionCreator {

    fun createUrlConnection(request: Request): HttpURLConnection

    fun getTimeOut() = 30 * 1000
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultConnectionCreator : NetworkConnectionCreator {

    override fun createUrlConnection(request: Request) =
        (URL(request.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = getTimeOut()
            readTimeout = getTimeOut()
            requestMethod = request.method.name
            doInput = true

            request.headers.forEach { header ->
                setRequestProperty(header.key, header.value)
            }

            if (request.body != null) {
                doOutput = true
                val os = outputStream
                BufferedWriter(OutputStreamWriter(os, "UTF-8")).apply {
                    write(request.body)
                    flush()
                    close()
                }
                os.close()
            }
        }
}