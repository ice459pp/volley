package com.shareba.volley.action;

import android.net.Uri;

import java.util.HashMap;

public class GetAction<T> {

    private Uri.Builder uriBuilder;
    private HashMap<String, String> header;
    private Class<T> cls;

    public GetAction(String url, Class<T> cls) {
        this.uriBuilder = Uri.parse(url).buildUpon();
        this.cls = cls;
    }

    public Class<T> getResponseClass() {
        return cls;
    }

    public GetAction putParam(String key, String value) {
        uriBuilder.appendQueryParameter(key, value);
        return this;
    }

    public GetAction putHeaders(String key, String value) {
        if (header == null) {
            header = new HashMap<>();
        }
        header.put(key, value);
        return this;
    }

    public String getUrl() {
        return uriBuilder.toString();
    }

    public HashMap<String, String> getHeaders() {
        return header;
    }
}
