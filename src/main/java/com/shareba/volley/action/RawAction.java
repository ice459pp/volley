package com.shareba.volley.action;


import com.google.gson.Gson;
import com.shareba.volley.GsonObject;

import java.util.HashMap;

public class RawAction<T> {
    private Class<T> cls;
    private HashMap<String, String> header;
    private String url;
    private GsonObject gsonObject;

    public RawAction(String url, Class<T> cls) {
        this.url = url;
        this.cls = cls;
    }

    public Class<T> getResponseClass() {
        return cls;
    }

    public RawAction<T> putHeaders(String key, String value) {
        if (header == null) {
            header = new HashMap<>();
        }
        header.put(key, value);
        return this;
    }

    public RawAction<T> putGsonObject(GsonObject gsonObject) {
        this.gsonObject = gsonObject;
        return this;
    }

    public String getJsonString() {
        Gson gson = new Gson();
        return gson.toJson(gsonObject);
    }

    public String getUrl() {
        return url;
    }

    public HashMap<String, String> getHeaders() {
        return header;
    }
}
