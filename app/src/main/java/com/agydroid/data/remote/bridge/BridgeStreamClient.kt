package com.agydroid.data.remote.bridge

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BridgeStreamClient @Inject constructor(
    private val gson: Gson
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamChat(
        request: ChatPromptRequest,
        baseUrl: String = "http://127.0.0.1:7860/"
    ): Flow<BridgeEvent> = callbackFlow {
        val jsonBody = gson.toJson(request)
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val httpRequest = Request.Builder()
            .url("${baseUrl}chat")
            .post(body)
            .addHeader("Accept", "text/event-stream")
            .build()

        val call = client.newCall(httpRequest)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.e(e, "Bridge stream connection failure")
                trySend(BridgeEvent.Error(e.message ?: "Failed to connect to Termux Bridge on localhost:7860"))
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val msg = "Bridge error HTTP ${response.code}: ${response.message}"
                    trySend(BridgeEvent.Error(msg))
                    close(Exception(msg))
                    return
                }

                val responseBody = response.body
                if (responseBody == null) {
                    trySend(BridgeEvent.Error("Empty response from Termux Bridge"))
                    close()
                    return
                }

                val source: BufferedSource = responseBody.source()
                try {
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: continue
                        if (line.isBlank()) continue

                        try {
                            val json = gson.fromJson(line, JsonObject::class.java)
                            val eventType = json.get("event")?.asString ?: "text"

                            when (eventType) {
                                "start" -> {
                                    val msg = json.get("message")?.asString ?: "Session started"
                                    trySend(BridgeEvent.Start(msg))
                                }
                                "data" -> {
                                    val type = json.get("type")?.asString
                                    val content = json.get("content")?.asString
                                        ?: json.get("text")?.asString
                                    trySend(BridgeEvent.StreamData(line, type, content))
                                }
                                "text" -> {
                                    val content = json.get("content")?.asString ?: line
                                    trySend(BridgeEvent.Text(content))
                                }
                                "done" -> {
                                    val exitCode = json.get("exit_code")?.asInt ?: 0
                                    val success = json.get("success")?.asBoolean ?: (exitCode == 0)
                                    trySend(BridgeEvent.Done(exitCode, success))
                                }
                                "error" -> {
                                    val errMsg = json.get("message")?.asString ?: "Unknown error"
                                    trySend(BridgeEvent.Error(errMsg))
                                }
                                else -> {
                                    trySend(BridgeEvent.Text(line))
                                }
                            }
                        } catch (e: Exception) {
                            trySend(BridgeEvent.Text(line))
                        }
                    }
                    close()
                } catch (e: Exception) {
                    trySend(BridgeEvent.Error(e.message ?: "Stream read error"))
                    close(e)
                } finally {
                    responseBody.close()
                }
            }
        })

        awaitClose {
            call.cancel()
        }
    }
}
