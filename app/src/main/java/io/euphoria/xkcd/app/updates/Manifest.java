package io.euphoria.xkcd.app.updates;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    public static final Comparator<Release> RELEASE_COMPARATOR = new Comparator<Release>() {
        @Override
        public int compare(Release r1, Release r2) {
            return r1.getVersion().compareTo(r2.getVersion());
        }
    };

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss.SSS Z",
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

    public Release getLatestRelease() {
        if (releases.isEmpty()) return null;
        return Collections.max(releases, RELEASE_COMPARATOR);
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
        // On at least one device, SimpleDateFormat chokes on the final "Z"; substitute it with something equivalent.
        text = text.replaceAll("\\s+Z$", " +00:00");
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.parse(text).getTime();
        }
    }
    public static String formatDate(long value) {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(value);
        }
    }

}
