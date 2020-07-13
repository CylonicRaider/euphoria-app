package io.euphoria.xkcd.app.updates;

import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static io.euphoria.xkcd.app.updates.UpdateUtilities.readAll;

/** Created by Xyzzy on 2020-07-10. */

// TODO: Create relative URLs when formatting?
public class Manifest {

    public static class MappingException extends Exception {

        public MappingException() {
            super();
        }
        public MappingException(String message) {
            super(message);
        }
        public MappingException(String message, Throwable cause) {
            super(message, cause);
        }
        public MappingException(Throwable cause) {
            super(cause);
        }

    }

    public class Release {

        private final Version version;
        private final String name;
        private final URL icon;
        private final long date;
        private final URL fileURL;
        private final String fileHash;

        public Release(JSONObject obj) throws MappingException {
            try {
                this.version = Version.parseVersion(obj.getString("version"));
                this.name = obj.getString("name");
                this.icon = new URL(baseURL, obj.getString("icon"));
                this.date = parseDate(obj.getString("date"));
                this.fileURL = new URL(baseURL, obj.getString("file"));
                this.fileHash = obj.getString("sha256");
            } catch (JSONException | MalformedURLException | ParseException exc) {
                throw new MappingException(exc);
            }
        }

        public Version getVersion() {
            return version;
        }

        public String getName() {
            return name;
        }

        public URL getIcon() {
            return icon;
        }

        public long getDate() {
            return date;
        }

        public URL getFileURL() {
            return fileURL;
        }

        public String getFileHash() {
            return fileHash;
        }

        public JSONObject toJSONObject() throws JSONException {
            JSONObject ret = new JSONObject();
            ret.put("version", getVersion().toString());
            ret.put("name", getName());
            ret.put("icon", getIcon().toString());
            ret.put("date", formatDate(getDate()));
            ret.put("file", getFileURL().toString());
            ret.put("fileHash", getFileHash());
            return ret;
        }

    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss.SSS X",
                                                                             Locale.ROOT);

    private final URL baseURL;
    private final String name;
    private final Version version;
    private final URL icon;
    private final List<Release> releases;

    public Manifest(URL baseURL, JSONObject obj) throws MappingException {
        this.baseURL = baseURL;
        try {
            this.name = obj.getString("name");
            this.version = Version.parseVersion(obj.getString("version"));
            this.icon = new URL(baseURL, obj.getString("icon"));
            this.releases = extractReleases(obj.getJSONArray("releases"));
        } catch (JSONException | MalformedURLException exc) {
            throw new MappingException(exc);
        }
    }

    private List<Release> extractReleases(JSONArray array) throws JSONException, MappingException {
        List<Release> ret = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            ret.add(new Release(array.getJSONObject(i)));
        }
        return Collections.unmodifiableList(ret);
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    public URL getIcon() {
        return icon;
    }

    public List<Release> getReleases() {
        return releases;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject ret = new JSONObject();
        ret.put("name", getName());
        ret.put("version", getVersion().toString());
        ret.put("icon", getIcon().toString());
        JSONArray releases = new JSONArray();
        for (Release r : getReleases()) {
            releases.put(r.toJSONObject());
        }
        ret.put("releases", releases);
        return ret;
    }

    public static long parseDate(String text) throws ParseException {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.parse(text).getTime();
        }
    }
    public static String formatDate(long value) {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(value);
        }
    }

    public static Future<Manifest> download(final URL source, ExecutorService exec) {
        return exec.submit(new Callable<Manifest>() {
            @Override
            public Manifest call() throws IOException, JSONException, MappingException {
                URLConnection conn = source.openConnection();
                conn.connect();
                InputStream input = conn.getInputStream();
                Reader inputReader = new InputStreamReader(input, "UTF-8");
                String result = readAll(inputReader);
                return new Manifest(source, new JSONObject(result));
            }
        });
    }

}
