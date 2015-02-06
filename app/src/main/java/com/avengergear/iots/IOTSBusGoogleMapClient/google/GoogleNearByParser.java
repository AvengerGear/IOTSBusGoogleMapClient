package com.avengergear.iots.IOTSBusGoogleMapClient.google;

import android.util.Log;

import com.avengergear.iots.IOTSBusGoogleMapClient.parser.JSONParser;
import com.avengergear.iots.IOTSBusGoogleMapClient.parser.NearByParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class GoogleNearByParser extends JSONParser implements NearByParser {
    /**
     * Distance covered. *
     */
    private String url;

    public GoogleNearByParser(String feedUrl) {
        super(feedUrl);
    }

    /**
     * Parses a url pointing to a Google Place JSON object to a Route object.
     *
     * @return a Route object based on the JSON object by KNightWeng@Avengergear
     */

    public String parse() {
        // turn the stream into a string
        final String result = convertStreamToString(this.getInputStream());
        if (result == null) return null;

        try {
            //Transform the string into a json object
            final JSONObject json = new JSONObject(result);

            //Get the results array
            final JSONObject jsonResultObject = json.getJSONObject("result");

            //Get the individual result
            url = jsonResultObject.get("url").toString();

            Log.d("IOTSBusGoogleMapRoute", "url = " + url);

        } catch (JSONException e) {
            Log.e("Routing Error", e.getMessage());
            return null;
        }
        return url;
    }
}
