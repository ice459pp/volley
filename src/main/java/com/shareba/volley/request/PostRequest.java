package com.shareba.volley.request;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.shareba.volley.action.PostAction;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PostRequest<T> extends Request<T> {
    private static final String TAG = "PostRequest";

    private final Listener<T> listener;
    private final Class<T> clazz;
    private HashMap<String, String> params;
    private HashMap<String, String> headers;
    private final Gson gson = new Gson();
    private HttpEntity entity;
    private boolean mForceCache = false;

    public PostRequest(PostAction<T> postAction, Listener<T> listener, ErrorListener errorListener) {
        super(Method.POST, postAction.getUrl(), errorListener);
        this.clazz = postAction.getResponseClass();
        this.listener = listener;
        this.params = postAction.getParams();
        this.headers = postAction.getHeaders();
        this.entity = buildMultipartEntity(params);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return this.params;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    private HttpEntity buildMultipartEntity(HashMap<String, String> params) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (Entry<String, String> p : params.entrySet()) {
            if (PostAction.UPLOAD_NAME.equals(p.getKey()))
                builder.addPart(PostAction.UPLOAD_NAME, new FileBody(new File(p.getValue())));
            else
                builder.addTextBody(p.getKey(), p.getValue(), ContentType.APPLICATION_JSON);
            Log.v(TAG, p.getKey() + ":" + p.getValue());
        }
        return builder.build();
    }

    @Override
    public String getBodyContentType() {
        return entity.getContentType().getValue();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            entity.writeTo(bos);
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));

            // protect JSON format, since remote server random throw unexpected
            // char like: NOTICE, Warning, Unable to fork...
            if (TextUtils.isEmpty(json))
                json = null;

            final int regionStart = json.indexOf("{");
            final int regionEnd = json.lastIndexOf("}");
            if (regionStart == -1 || regionEnd == -1)
                json = null;

            json = json.substring(regionStart, regionEnd + 1);

            // Since our webservice server not supported cache-control
            // mechanism, dirty hack here to FORCE-cache
            if (mForceCache) {
                return Response.success(gson.fromJson(json, clazz), parseIgnoreCacheHeaders(response));
            } else {
                return Response.success(gson.fromJson(json, clazz), HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(new Throwable(new String(response.data))));
        }
    }

    public void setForceCache(boolean b) {
        mForceCache = b;
    }

    private static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response) {
        long now = System.currentTimeMillis();

        Map<String, String> headers = response.headers;
        long serverDate = 0;
        String serverEtag = null;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
            serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }

        serverEtag = headers.get("ETag");

        // in 3 minutes cache will be hit, but also refreshed on background
        final long cacheHitButRefreshed = 3 * 60 * 1000;

        // in 24 hours this cache entry expires completely
        final long cacheExpired = 24 * 60 * 60 * 1000;
        final long softExpire = now + cacheHitButRefreshed;
        final long ttl = now + cacheExpired;

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;

        return entry;
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    public void disableTimeoutRetry() {
        setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));
    }
}
