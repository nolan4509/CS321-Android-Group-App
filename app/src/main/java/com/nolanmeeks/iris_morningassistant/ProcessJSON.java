package com.nolanmeeks.iris_morningassistant;

import android.util.JsonReader;
import android.util.JsonToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by wip on 3/31/17.
 */

public class ProcessJSON {
    public static String APIKEY = "wEyiUFGH9cOOMcAmceXGtdvBE0o6BGQl";
    public static String location = null;
    public void Main(String args[]) {

    }

    public static JsonReader getReader(String address) {
        JsonReader rdr = null;
        try {
            URL url = new URL(address);
            InputStream is = url.openStream();
            Reader r = new InputStreamReader(is);
            rdr = new JsonReader(r);
        } catch (Exception e) {

        }
        return rdr;
    }

    public static String getLocationKey(double lat, double lon) {
        String locKey = null;
        String addr = String.format("http://dataservice.accuweather.com/locations/v1/cities/geoposition/search?apikey=%s&q=%f,%f",
                APIKEY, lat, lon);
        JsonReader rdr = getReader(addr);
        try {
            findMatch(rdr, "Key");
            locKey = rdr.nextString();
            findMatch(rdr, "LocalizedName");
            location = rdr.nextString();
        } catch (Exception e) {
            System.err.println("[!!] Error getting Key!!");
            System.err.println(e);
        }
        //System.out.println(locKey);
        return locKey;
    }

    public static HashMap<String,String> getCurrentForecast(String locKey) {
        if (locKey == null) return null;
        HashMap<String,String> data= new HashMap<String, String>();
        String addr = String.format("http://dataservice.accuweather.com/currentconditions/v1/%s?apikey=%s",
                locKey, APIKEY);
        JsonReader rdr = getReader(addr);
        try {
            findMatch(rdr,"WeatherText");
            data.put("condition",rdr.nextString());

            findMatch(rdr,"IsDayTime");
            data.put("day?",""+rdr.nextBoolean());

            findMatch(rdr,"Temperature");
            findMatch(rdr,"Imperial");
            findMatch(rdr,"Value");
            data.put("temp",""+rdr.nextInt());
        } catch (Exception e) {
            System.err.println("[!!] Error getting Data!!");
            System.err.println(e);
        }
        return data;
    }
    public static void findMatch(JsonReader rdr, String match) {
        try {
            JsonToken next = JsonToken.NULL;
            while (next != JsonToken.END_DOCUMENT) {
                //System.out.println(next.toString());
                if (next == JsonToken.BEGIN_OBJECT) rdr.beginObject();
                else if(next == JsonToken.END_OBJECT) rdr.endObject();
                else if(next == JsonToken.END_ARRAY) rdr.endArray();
                else if(next == JsonToken.BEGIN_ARRAY) rdr.beginArray();
                else if(next == JsonToken.NAME) {
                    String name = rdr.nextName();
                    //System.out.println(name);
                    if (name.equals(match)) return;
                    else rdr.skipValue();
                }
                next = rdr.peek();
            }
        } catch (Exception e) {

        }
    }
    public static HashMap<Integer,HashMap<String,String>> getDailyForecast(String locKey) {
        if (locKey == null) return null;
        HashMap<Integer,HashMap<String,String>> data=
                new HashMap<Integer,HashMap<String,String>>();
        String addr = String.format("http://dataservice.accuweather.com/forecasts/v1/daily/5day/%s?apikey=%s",
                locKey, APIKEY);
        JsonReader rdr = getReader(addr);
        HashMap<String, String> day;

        SimpleDateFormat sdf = new SimpleDateFormat("M/d E");
        findMatch(rdr,"DailyForecasts");

        for(int i = 0 ; i < 5; i++) {
            try {
                day = new HashMap<String, String>();

                findMatch(rdr,"EpochDate");
                day.put("date",sdf.format(rdr.nextLong()*1000));
                //System.out.println(day.get("date"));

                findMatch(rdr,"Temperature");
                findMatch(rdr,"Minimum");
                findMatch(rdr,"Value");
                day.put("low",""+rdr.nextInt());
                //System.out.println(day.get("low"));

                findMatch(rdr,"Maximum");
                findMatch(rdr,"Value");
                day.put("high",""+rdr.nextInt());
                //System.out.println(day.get("high"));

                findMatch(rdr,"Day");
                findMatch(rdr,"IconPhrase");
                day.put("condition", rdr.nextString());
                //System.out.println(day.get("condition"));

                data.put(i,day);

            } catch (Exception e) {
                System.err.println("[!!] Error getting Data!!");
                System.err.println(e);
            }
        }
        return data;
    }

    public static HashMap<Integer,HashMap<String,String>> getHourlyForecast(String locKey) {
        HashMap<Integer,HashMap<String,String>> data = new HashMap<Integer,HashMap<String,String>>();
        String addr = String.format("http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/%s?apikey=%s",
                locKey, APIKEY);
        JsonReader rdr = getReader(addr);

        SimpleDateFormat sdf = new SimpleDateFormat("h a");

        try {
            for(int i = 0; i < 12; i++) {
                HashMap<String,String> hour = new HashMap<String, String>();

                findMatch(rdr,"EpochDateTime");
                hour.put("time",sdf.format(rdr.nextLong()*1000));

                findMatch(rdr,"IconPhrase");
                hour.put("condition",rdr.nextString());

                findMatch(rdr,"IsDaylight");
                hour.put("day?",""+rdr.nextBoolean());

                findMatch(rdr,"Temperature");
                findMatch(rdr,"Value");
                hour.put("temp",""+rdr.nextInt());


                findMatch(rdr,"PrecipitationProbability");
                hour.put("rain",""+rdr.nextInt());

                data.put(new Integer(i), hour);
            }
        } catch (Exception e) {
            System.err.println("[!!] Failed to Read Hourly Data");
            System.err.println(e);
        }
        return data;
    }

}
