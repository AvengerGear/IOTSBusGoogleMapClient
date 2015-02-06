package com.avengergear.iots.IOTSBusGoogleMapClient.parser;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class JSONParser {
    // names of the XML tags
    protected static final String MARKERS = "markers";
    protected static final String MARKER = "marker";

    protected URL feedUrl;
    protected List<String> referenceListArray;

    protected JSONParser(final String feedUrl) {
        try {
            this.feedUrl = new URL(feedUrl);
        } catch (MalformedURLException e) {
            Log.e("Routing Error", e.getMessage());
        }
    }

    protected JSONParser(final List<String> referenceListArray) {
            this.referenceListArray = referenceListArray;
    }

    protected InputStream getInputStream() {
        try {
            return feedUrl.openConnection().getInputStream();
        } catch (IOException e) {
            Log.e("Routing Error", e.getMessage());
            return null;
        }
    }

    /**
     * Convert an inputstream to a string.
     *
     * @param input inputstream to convert.
     * @return a String of the inputstream.
     */

    protected static String convertStreamToString(final InputStream input) {
        if (input == null) return null;

        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        final StringBuilder sBuf = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sBuf.append(line);
            }
        } catch (IOException e) {
            Log.e("Routing Error", e.getMessage());
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                Log.e("Routing Error", e.getMessage());
            }
        }
        return sBuf.toString();
    }
}