**Volley init on App Application**
```java
public class ShareBaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // init VolleyTool
        Api.with(this);
    }

}
```
**Http Get Action**
```java
GetAction<BaseResponse> getAction = Api.with(this).getApi();
GetRequest<BaseResponse> getRequest =
        new GetRequest<>(getAction, new Response.Listener<BaseResponse>() {
    @Override
    public void onResponse(BaseResponse response) {
        if (response.status) {
            Log.d(TAG, "response is OK");
        } else {
            Log.e(TAG, "response is error");
        }
    }
}, new Response.ErrorListener() {
    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, "error message: " + error.getMessage());
    }
});

getRequest.disableTimeoutRetry();
Api.with(this).addToRequestQueue(this, getRequest);
```


**Http Post Action**
```java
PostAction<BaseResponse> postAction = Api.with(this).postApi("contentId", "token");
PostRequest<BaseResponse> postRequest = new PostRequest<>(postAction, new Response.Listener<BaseResponse>() {
    @Override
    public void onResponse(BaseResponse response) {
        if (response.status) {
            Log.d(TAG, "response is OK");
        } else {
            Log.e(TAG, "response is error");
        }
    }
}, new Response.ErrorListener() {
    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, "error message: " + error.getMessage());
    }
});

postRequest.disableTimeoutRetry();
Api.with(this).addToRequestQueue(this, postRequest);
```


**Http Raw Data Post Action**
```java
RawAction<BaseResponse> rawAction = Api.with(this).rawApi(gsonObject);
RawRequest<BaseResponse> rawRequest = new RawRequest<>(rawAction, new Response.Listener<BaseResponse>() {
    @Override
    public void onResponse(BaseResponse response) {
        if (response.status) {
            Log.d(TAG, "response is OK");
        } else {
            Log.e(TAG, "response is error");
        }

    }
}, new Response.ErrorListener() {
    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, "error message: " + error.getMessage());
    }
});

rawRequest.disableTimeoutRetry();
Api.with(this).addToRequestQueue(this, rawRequest);
```

**Api.java**
```java
public class Api {

    private static final String TAG = "Api";
    private static Context mContext;
    private static RequestQueue requestQueue;
    private static final String OAUTH_DOMAIN = "url.tw";

    public static Api init(Context context) {
        mContext = context;
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(mContext);
        }
        return new Api();
    }

    public static Api with(Context context) {
        mContext = context;
        if (requestQueue == null) {
            init(mContext);
        }
        return new Api();
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            init(mContext);
        }
        return requestQueue;
    }

    private String getApiDomainUrl(String apiFile) {
        try {
//            URL apiUrl = new URL("http", API_DOMAIN, "/App_Post/" + apiFile);
            URL apiUrl = new URL("http", BuildConfig.API_DOMAIN, apiFile);
            return apiUrl.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }

    public <T> void addToRequestQueue(Request<T> request, String tag) {
        request.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(request);
    }

    public <T> void addToRequestQueue(AppCompatActivity activity, Request<T> req) {
        // TODO check BaseActivity
        req.setTag(getTagName(activity));
        getRequestQueue().add(req);
    }

    public void setBitmapWithUrl(Context context, final ImageView imageView, String url, int replaceholderRes) {

        ImageListener listener = ImageLoader.getImageListener(imageView, replaceholderRes, replaceholderRes);
        ImageLoader imageLoader = getImageLoader(context);
        imageLoader.get(url, listener);
    }

    private ImageLoader getImageLoader(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int largestWidth = metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels;

        // Create ImageLoader instance
        return getImageLoader(context, largestWidth);
    }

    private ImageLoader getImageLoader(Context context, int maxImageSize) {

        FragmentManager manager = null;
        if (context instanceof AppCompatActivity) {
            manager = ((AppCompatActivity) context).getSupportFragmentManager();
        } else if (context instanceof FragmentActivity) {
            manager = ((FragmentActivity) context).getSupportFragmentManager();
        }
        return new ImageLoader(getRequestQueue(), ImageLoaderCache.getInstance(manager, maxImageSize));
    }

    public String getTagName(AppCompatActivity activity) {
        final String name = activity.getComponentName().getShortClassName();
        final String data = activity.getIntent().getDataString();
        final Bundle bundle = activity.getIntent().getExtras();
        if (data != null)
            return name + data.hashCode();
        else if (bundle != null)
            return name + bundle.hashCode();
        else
            return name;
    }

    public void clearRequestCache(String url) {
        final Cache.Entry entry = getRequestQueue().getCache().get(url);
        if (entry != null && entry.data != null && entry.data.length > 0)
            if (!entry.isExpired()) {
                getRequestQueue().getCache().invalidate(url, true);
            }
    }

    public PostAction<BaseResponse> postApi(String contentId, String accessToken) {
        String url = getApiDomainUrl("/bookmark/insert?accessToken=" + accessToken);
        PostAction<BaseResponse> postAction = new PostAction<>(url, BaseResponse.class);
        postAction.putParam("type", BuildConfig.APP_TYPE)
                .putParam("contentId", contentId);
        return postAction;
    }

    public GetAction<BaseResponse> getApi() {
        GetAction<BaseResponse> getAction = new GetAction<>(getApiDomainUrl("/menu"), BaseResponse.class);
        getAction.putParam("type", BuildConfig.APP_TYPE);
        return getAction;
    }

    public RawAction<BaseResponse> rawApi(GsonObject gsonObject) {
        RawAction<BaseResponse> rawAction = new RawAction<>(getApiDomainUrl("/rowData"), BaseResponse.class);
        rawAction.putGsonObject(gsonObject);
        return rawAction;
    }

}
```

If you use the following code:

```java
Api.with(this).addToRequestQueue(this, request);
```
Be remember to add cancel queue action on the activity lifecycle 
when onStop() being triggered.

```java
@Override
    protected void onStop() {
        super.onStop();
        Api.with(this).getRequestQueue().cancelAll(new RequestQueue.RequestFilter() {
            // To solve cancel volley request from fragment
            // Instead of using a tag for cancelAll, make an all-pass
            // RequestFilter.
            // ref:
            // http://stackoverflow.com/questions/16774667/cancel-all-volley-requests-android
            @Override
            public boolean apply(Request<?> request) {
                if (request.getTag() == null)
                    return false;

                return request.getTag().equals(Api.with(BaseActivity.this).getTagName(BaseActivity.this));
            }
        });
    }
```
