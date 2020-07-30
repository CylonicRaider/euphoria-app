package io.euphoria.xkcd.app.updates

import java.io.IOException
import java.io.Reader

/** Created by Xyzzy on 2020-07-12.  */
object UpdateUtilities {
    @Throws(IOException::class)
    fun readAll(reader: Reader): String {
        var data: CharArray = CharArray(2048)
        var length: Int = 0
        while (true) {
            val read: Int = reader.read(data, length, data.size - length)
            if (read == -1) break
            length += read
            if (length == data.size) {
                val newData: CharArray = CharArray(data.size * 2)
                System.arraycopy(data, 0, newData, 0, length)
                data = newData
            }
        }
        return String(data, 0, length)
    }
}
