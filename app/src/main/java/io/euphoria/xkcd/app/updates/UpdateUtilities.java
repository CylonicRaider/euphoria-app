package io.euphoria.xkcd.app.updates;

import java.io.IOException;
import java.io.Reader;

/** Created by Xyzzy on 2020-07-12. */

public class UpdateUtilities {

    private UpdateUtilities() {}

    public static String readAll(Reader reader) throws IOException {
        char[] data = new char[2048];
        int length = 0;
        for (;;) {
            int read = reader.read(data, length, data.length - length);
            if (read == -1) break;
            length += read;
            if (length == data.length) {
                char[] newData = new char[data.length * 2];
                System.arraycopy(data, 0, newData, 0, length);
                data = newData;
            }
        }
        return new String(data, 0, length);
    }

}
