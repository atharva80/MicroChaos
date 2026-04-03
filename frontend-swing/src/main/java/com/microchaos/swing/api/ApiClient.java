package com.microchaos.swing.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApiClient {
    private final String apiBase;
    private final Gson gson = new Gson();
    private final CloseableHttpClient httpClient;

    public ApiClient(String apiBase) {
        this.apiBase = apiBase;
        this.httpClient = HttpClients.createDefault();
    }

    public <T> T get(String endpoint, Class<T> responseType) throws IOException {
        HttpGet request = new HttpGet(apiBase + endpoint);
        try {
            return httpClient.execute(request, response -> {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                return gson.fromJson(content, responseType);
            });
        } catch (Exception e) {
            throw new IOException("Failed to GET " + endpoint, e);
        }
    }

    public <T> List<T> getList(String endpoint, Class<T> itemType) throws IOException {
        HttpGet request = new HttpGet(apiBase + endpoint);
        try {
            return httpClient.execute(request, response -> {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                JsonObject json = gson.fromJson(content, JsonObject.class);
                
                List<T> items = new ArrayList<>();
                if (json.has("items") && json.get("items").isJsonArray()) {
                    JsonArray array = json.getAsJsonArray("items");
                    for (int i = 0; i < array.size(); i++) {
                        items.add(gson.fromJson(array.get(i), itemType));
                    }
                }
                return items;
            });
        } catch (Exception e) {
            throw new IOException("Failed to GET " + endpoint, e);
        }
    }

    public <T> T post(String endpoint, Class<T> responseType) throws IOException {
        HttpPost request = new HttpPost(apiBase + endpoint);
        request.setHeader("Content-Type", "application/json");
        
        try {
            return httpClient.execute(request, response -> {
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                return gson.fromJson(content, responseType);
            });
        } catch (Exception e) {
            throw new IOException("Failed to POST " + endpoint, e);
        }
    }

    public void close() throws IOException {
        httpClient.close();
    }
}
