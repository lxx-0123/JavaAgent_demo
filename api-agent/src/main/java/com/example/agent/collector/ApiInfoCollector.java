package com.example.agent.collector;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;


import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ApiInfoCollector {
    private static final String OUTPUT_FILE = "api_info.json";
    private static final Set<String> collectedApis = new HashSet<>();
    
    public static synchronized void collectApiInfo(HttpServletRequest request, String methodName) {
        try {
            String url = request.getRequestURI();
            String method = request.getMethod();
            
            // 构建唯一键，防止重复收集
            String key = method + ":" + url + ":" + methodName;
            if (collectedApis.contains(key)) {
                return;
            }
            collectedApis.add(key);
            
            JSONObject apiInfo = new JSONObject();
            apiInfo.put("url", url);
            apiInfo.put("method", method);
            apiInfo.put("methodName", methodName);
            
            // 收集请求参数
            Map<String, String[]> parameterMap = request.getParameterMap();
            JSONObject parameters = new JSONObject();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                parameters.put(entry.getKey(), entry.getValue());
            }
            apiInfo.put("parameters", parameters);
            
            // 写入文件
            writeToFile(apiInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static synchronized void writeToFile(JSONObject apiInfo) {
        try {
            File file = new File(OUTPUT_FILE);
            List<JSONObject> apiList = new ArrayList<>();
            
            // 读取现有文件内容
            if (file.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                if (!content.isEmpty()) {
                    apiList = JSON.parseArray(content, JSONObject.class);
                }
            }
            
            // 添加新的API信息
            apiList.add(apiInfo);
            
            // 写入文件
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(JSON.toJSONString(apiList, true));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 