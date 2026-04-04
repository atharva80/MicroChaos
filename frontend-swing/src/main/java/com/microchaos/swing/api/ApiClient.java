package com.microchaos.swing.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

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
            return parseResponse(execute(request), responseType);
        } catch (Exception e) {
            throw new IOException("Failed to GET " + endpoint, e);
        }
    }

    public <T> List<T> getList(String endpoint, Class<T> itemType) throws IOException {
        HttpGet request = new HttpGet(apiBase + endpoint);
        try {
            String content = execute(request);
            JsonElement root = gson.fromJson(content, JsonElement.class);
            JsonArray array = new JsonArray();

            if (root != null && root.isJsonObject()) {
                JsonObject json = root.getAsJsonObject();
                if (json.has("items") && json.get("items").isJsonArray()) {
                    array = json.getAsJsonArray("items");
                }
            } else if (root != null && root.isJsonArray()) {
                array = root.getAsJsonArray();
            }

            List<T> items = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                items.add(gson.fromJson(array.get(i), itemType));
            }
            return items;
        } catch (Exception e) {
            throw new IOException("Failed to GET " + endpoint, e);
        }
    }

    public <T> T post(String endpoint, Class<T> responseType) throws IOException {
        HttpPost request = new HttpPost(apiBase + endpoint);
        request.setHeader("Content-Type", "application/json");

        try {
            return parseResponse(execute(request), responseType);
        } catch (Exception e) {
            throw new IOException("Failed to POST " + endpoint, e);
        }
    }

    public void delete(String endpoint) throws IOException {
        HttpDelete request = new HttpDelete(apiBase + endpoint);
        try {
            execute(request);
        } catch (Exception e) {
            throw new IOException("Failed to DELETE " + endpoint, e);
        }
    }

    private String execute(ClassicHttpRequest request) throws IOException {
        return httpClient.execute(request, response -> {
            int statusCode = response.getCode();
            HttpEntity entity = response.getEntity();
            String content = entity == null ? "" : EntityUtils.toString(entity);
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP " + statusCode + (content.isBlank() ? "" : ": " + content));
            }
            return content;
        });
    }

    private <T> T parseResponse(String content, Class<T> responseType) {
        if (responseType == null || responseType == Void.class) {
            return null;
        }
        if (responseType == String.class) {
            return responseType.cast(content);
        }
        if (content == null || content.isBlank()) {
            return null;
        }
        return gson.fromJson(content, responseType);
    }

    public void close() throws IOException {
        httpClient.close();
    }
}
