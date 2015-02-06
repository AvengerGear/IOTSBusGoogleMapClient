package com.avengergear.iots.IOTSBusGoogleMapClient.google;

import android.util.Log;

import com.avengergear.iots.IOTSBusGoogleMapClient.parser.JSONParser;
import com.avengergear.iots.IOTSBusGoogleMapClient.parser.ReferenceParser;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GooglePlaceParser extends JSONParser implements ReferenceParser {
    /**
     * reference array list *
     */
    private final List<String> mReferenceListArray;


    public GooglePlaceParser(String feedUrl) {
        super(feedUrl);
        mReferenceListArray = new ArrayList<String>();
    }

    /**
     * Parses a url pointing to a Google Place JSON object to a Route object.
     *
     * @return a Route object based on the JSON object by KNightWeng@Avengergear
     */

    public List<String> parse() {
        // turn the stream into a string
        final String result = convertStreamToString(this.getInputStream());
        if (result == null) return null;

        try {
            //Transform the string into a json object
            final JSONObject json = new JSONObject(result);
            //Get the results array
            final JSONArray jsonResultsArray = json.getJSONArray("results");
            //Get the length of results array
            final int num = jsonResultsArray.length();

            for (int i = 0; i < num; i++) {
                //Get the individual result
                mReferenceListArray.add(i, jsonResultsArray.getJSONObject(i).get("reference").toString());
            }
        } catch (JSONException e) {
            Log.e("Routing Error", e.getMessage());
            return null;
        }
        return mReferenceListArray;
    }
}
