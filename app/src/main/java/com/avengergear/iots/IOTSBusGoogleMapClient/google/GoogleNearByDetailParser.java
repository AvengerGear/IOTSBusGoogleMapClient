package com.avengergear.iots.IOTSBusGoogleMapClient.google;

import android.util.Log;

import com.avengergear.iots.IOTSBusGoogleMapClient.parser.BusNumberParser;
import com.avengergear.iots.IOTSBusGoogleMapClient.parser.JSONParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class GoogleNearByDetailParser extends JSONParser implements BusNumberParser {
    /**
     * Distance covered. *
     */
    private String url;
    private List<String> busNumberArray;
    private int busNumberArrayCnt = 0;

    public GoogleNearByDetailParser(String feedUrl) {
        super(feedUrl);
        busNumberArray = new ArrayList<String>();
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

        Document doc = Jsoup.parse(result);

        /* Get Bus number array */
        if (doc.select("div[class=text]") != null){
            for(org.jsoup.nodes.Element ele : doc.select("div[class=text]"))
            {
                Log.d("IOTSBusGoogleMapRoute", "Bus number: " + ele.text());
                busNumberArray.add(busNumberArrayCnt, ele.text());
                busNumberArrayCnt++;
            }
            return busNumberArray;
        }else
            return null;
    }
}
