package com.chatapp.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}
