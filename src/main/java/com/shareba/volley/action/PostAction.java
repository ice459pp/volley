package com.shareba.volley.action;

import java.io.File;
import java.util.HashMap;

public class PostAction<T> {
    public static String UPLOAD_NAME = "upload_file";
    private Class<T> cls;
    private HashMap<String, String> params;
    private HashMap<String, String> header;
    private String url;

    public PostAction(String url, Class<T> cls) {
        this.url = url;
        this.cls = cls;
    }

    public Class<T> getResponseClass() {
        return cls;
    }

    public PostAction putParam(String key, String value) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(key, value);
        return this;
    }

    public PostAction putHeaders(String key, String value) {
        if (header == null) {
            header = new HashMap<>();
        }
        header.put(key, value);
        return this;
    }

    public PostAction putFile(File file) {
        params.put(UPLOAD_NAME, file.getAbsolutePath());
        return this;
    }

    public String getUrl() {
        return url;
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public HashMap<String, String> getHeaders() {
        return header;
    }

}
