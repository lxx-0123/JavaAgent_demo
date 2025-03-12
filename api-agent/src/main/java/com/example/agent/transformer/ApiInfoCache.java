package com.example.agent.transformer;

import com.example.agent.model.ApiInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiInfoCache {
    private static final Map<String, ApiInfo> API_CACHE = new ConcurrentHashMap<>();

    public static void addApiInfo(String key, ApiInfo info) {
        API_CACHE.putIfAbsent(key, info);
    }

    public static Collection<ApiInfo> getAllApis() {
        return new ArrayList<>(API_CACHE.values());
    }
}
