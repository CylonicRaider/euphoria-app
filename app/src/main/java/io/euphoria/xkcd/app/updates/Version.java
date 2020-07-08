package io.euphoria.xkcd.app.updates;

import java.util.regex.Pattern;

/** Created by Xyzzy on 2020-07-08. */

public class Version implements Comparable<Version> {

    private static final Pattern VALID_VERSION_STRING = Pattern.compile("^[1-9][0-9]*(\\.[1-9][0-9]*)*$");

    private final int[] fields;

    public Version(int... fields) {
        this.fields = fields.clone();
    }

    @Override
    public boolean equals(Object other) {
        return ((other instanceof Version) && (compareTo((Version) other) == 0));
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int f : fields) {
            hash = hash * 31 + f;
        }
        return hash;
    }

    @Override
    public int compareTo(Version other) {
        int fc = getFieldCount(), ofc = other.getFieldCount();
        for (int i = 0;; i++) {
            if (i == fc) {
                if (i == ofc) return 0;
                return -1;
            } else if (i == ofc) {
                return 1;
            } else if (getField(i) != other.getField(i)) {
                return getField(i) - other.getField(i);
            }
        }
    }

    public int[] getFields() {
        return fields.clone();
    }

    public int getFieldCount() {
        return fields.length;
    }

    public int getField(int index) {
        return fields[index];
    }

    public static Version parseVersion(String text) {
        if (!VALID_VERSION_STRING.matcher(text).matches())
            throw new IllegalArgumentException("Invalid version string " + text);
        String[] rawFields = text.split("\\.");
        int[] fields = new int[rawFields.length];
        for (int i = 0; i < rawFields.length; i++) {
            fields[i] = Integer.parseInt(rawFields[i]);
        }
        return new Version(fields);
    }

}
