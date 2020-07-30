package io.euphoria.xkcd.app.updates

import io.euphoria.xkcd.app.updates.Manifest.MappingException
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** Created by Xyzzy on 2020-07-14.  */
object ManifestDownloader {
    private val EXECUTOR: Executor =
        Executors.newCachedThreadPool()

    fun download(
        source: URL?,
        exec: Executor,
        handler: Callback
    ) {
        exec.execute(object : Runnable {
            override fun run() {
                try {
                    val conn: URLConnection = source!!.openConnection()
                    conn.connect()
                    val input: InputStream = conn.getInputStream()
                    val inputReader: Reader = InputStreamReader(input, "UTF-8")
                    val result: String? = UpdateUtilities.readAll(inputReader)
                    handler.downloadFinished(
                        Manifest(
                            source,
                            JSONObject(result)
                        )
                    )
                } catch (exc: IOException) {
                    handler.downloadFailed(exc)
                } catch (exc: JSONException) {
                    handler.downloadFailed(exc)
                } catch (exc: MappingException) {
                    handler.downloadFailed(exc)
                }
            }
        })
    }

    fun download(source: URL?, handler: Callback) {
        download(source, EXECUTOR, handler)
    }

    open interface Callback {
        fun downloadFinished(result: Manifest)
        fun downloadFailed(error: Throwable)
    }
}
