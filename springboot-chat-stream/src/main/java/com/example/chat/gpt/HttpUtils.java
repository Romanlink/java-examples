package com.example.chat.gpt;

import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author liangwang
 * @create 3/14/24 19:21
 */
public class HttpUtils {

    public static final int DEFAULT_CONNECT_TIMEOUT = 300;
    public static final int DEFAULT_READ_TIMEOUT = 300;

    public static OkHttpClient newClient() {
        return newClient(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static OkHttpClient newClient(int connectTimeout, int readTimeout) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();
    }

    public static String get(String url) throws IOException, HttpRequestException {
        return get(url, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static String get(String url, int connectTimeout, int readTimeout) throws IOException, HttpRequestException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return submitRequest(request, connectTimeout, readTimeout);
    }

    public static String post(Map<String, String> headers, String url, String payload) throws IOException, HttpRequestException {
        return post(headers, url, payload, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static String post(Map<String, String> headersMap, String url, String payload, int connectTimeOut, int readTimeOut) throws IOException, HttpRequestException {
        RequestBody body = RequestBody.create(MediaType.get("application/json"), payload);
        Headers headers = null;
        if (headersMap != null) {
            headers = Headers.of(headersMap);
        }
        Request request = new Request.Builder()
                .headers(headers)
                .url(url)
                .post(body)
                .build();
        return submitRequest(request, connectTimeOut, readTimeOut);
    }

    private static String submitRequest(Request request, int connectTimeout, int readTimeout) throws IOException, HttpRequestException {
        OkHttpClient client = newClient(connectTimeout, readTimeout);


        try (Response response = client.newCall(request).execute()) {
            String content = response.body().string();
            if (!response.isSuccessful()) {
                throw new HttpRequestException("Visit: " + request.url().toString() + "\n " + content);
            }
            return content;
        }
    }

    public static String submitRequest(Request request) throws IOException, HttpRequestException {
        return submitRequest(request, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }
}
