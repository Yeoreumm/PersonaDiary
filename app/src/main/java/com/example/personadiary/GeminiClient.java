package com.example.personadiary;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiClient {

    // 나중에 API 키 여기에 입력
    private static final String API_KEY = "";
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public interface GeminiCallback {
        void onSuccess(String result);
        void onFailure(String error);
    }

    public static void ask(String prompt, GeminiCallback callback) {
        // android.util.Log.e("GEMINI", "ask() 호출됨");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        try {
            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(URL)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    android.util.Log.e("GEMINI", "네트워크 에러: " + e.getMessage());
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure(e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int code = response.code();
                    String body = response.body().string();
                    android.util.Log.e("GEMINI", "코드: " + code + " / 응답: " + body);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (code != 200) {
                            callback.onFailure("HTTP " + code + ": " + body);
                        } else {
                            try {
                                // 기존 파싱 코드
                                org.json.JSONObject json = new org.json.JSONObject(body);
                                String text = json
                                        .getJSONArray("candidates")
                                        .getJSONObject(0)
                                        .getJSONObject("content")
                                        .getJSONArray("parts")
                                        .getJSONObject(0)
                                        .getString("text");
                                callback.onSuccess(text.trim());
                            } catch (Exception e) {
                                callback.onFailure("파싱 오류: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            callback.onFailure("요청 오류: " + e.getMessage());
        }
    }
}