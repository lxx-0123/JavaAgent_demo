package com.example.agent.transformer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.agent.collector.ApiInfoCollector;
import com.example.agent.model.ApiInfo;
import javassist.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javassist.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Modifier;

public class SpringControllerTransformer implements ClassFileTransformer {
    private final ClassPool classPool;
    private static final String OUTPUT_DIR = "D:/api-logs/";
    private static final ConcurrentHashMap<String, List<JSONObject>> apiLogs = new ConcurrentHashMap<>();

    public SpringControllerTransformer() {
        this.classPool = ClassPool.getDefault();
        new File(OUTPUT_DIR).mkdirs();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // 跳过 CGLIB 生成的代理类
        if (className == null || className.contains("$$")) {
            return null;
        }

        // 只处理 controller 包下的类
        if (!className.contains("controller")) {
            return null;
        }

        try {
            if (loader != null) {
                classPool.insertClassPath(new LoaderClassPath(loader));
            }

            // 获取原始类名（去除代理类后缀）
            String originalClassName = className.replace('/', '.');
            if (originalClassName.contains("$$")) {
                originalClassName = originalClassName.substring(0, originalClassName.indexOf("$$"));
            }

            classPool.insertClassPath(new ByteArrayClassPath(originalClassName, classfileBuffer));
            CtClass cc = classPool.get(originalClassName);

            if (cc.isFrozen()) {
                cc.defrost();
            }

            boolean isModified = false;
            for (CtMethod method : cc.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    enhanceMethod(method);
                    isModified = true;
                }
            }

            return isModified ? cc.toBytecode() : null;
        } catch (Exception e) {
            // 只记录非代理类的错误
            if (!className.contains("$$")) {
                System.err.println("Transform error for class: " + className);
                e.printStackTrace();
            }
            return null;
        } finally {
            // 清理 ClassPool 缓存
            try {
                String originalClassName = className.replace('/', '.');
                if (originalClassName.contains("$$")) {
                    originalClassName = originalClassName.substring(0, originalClassName.indexOf("$$"));
                }
                classPool.removeClassPath(new ByteArrayClassPath(originalClassName, classfileBuffer));
            } catch (Exception ignored) {
            }
        }
    }

    private void enhanceMethod(CtMethod method) throws CannotCompileException {
        // 检查方法是否是代理方法
        if (method.getName().contains("$$")) {
            return;
        }

        StringBuilder code = new StringBuilder();
        code.append("{")
                .append("  com.example.agent.transformer.SpringControllerTransformer")
                .append(".saveToJson(\"")
                .append(method.getLongName())
                .append("\", \"")
                .append(getMethodPath(method))
                .append("\", \"")
                .append(getHttpMethod(method))
                .append("\", $args, \"")
                .append(method.getDeclaringClass().getName())
                .append("\");")
                .append("}");

        method.insertBefore(code.toString());
    }

    private String getMethodPath(CtMethod method) {
        try {
            // 获取类级别的路径
            String basePath = "";
            Object[] classAnnotations = method.getDeclaringClass().getAnnotations();
            for (Object anno : classAnnotations) {
                if (anno.toString().contains("RequestMapping")) {
                    // 从注解中提取基础路径
                    basePath = "/api"; // 这里需要根据实际注解获取
                    break;
                }
            }

            // 获取方法级别的路径
            String methodPath = "";
            Object[] methodAnnotations = method.getAnnotations();
            for (Object anno : methodAnnotations) {
                if (anno.toString().contains("Mapping")) {
                    // 从注解中提取方法路径
                    methodPath = "/test"; // 这里需要根据实际注解获取
                    break;
                }
            }

            return basePath + methodPath;
        } catch (Exception e) {
            return "";
        }
    }

    private String getHttpMethod(CtMethod method) {
        try {
            Object[] annotations = method.getAnnotations();
            for (Object anno : annotations) {
                String annoName = anno.toString();
                if (annoName.contains("GetMapping")) return "get";
                if (annoName.contains("PostMapping")) return "post";
                if (annoName.contains("PutMapping")) return "put";
                if (annoName.contains("DeleteMapping")) return "delete";
                if (annoName.contains("RequestMapping")) return "get"; // 默认为 GET
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return "get";
    }


    public static void saveToJson(String methodName, String path, String httpMethod,
                                  Object[] args, String controllerName) {
        try {
            JSONObject log = new JSONObject();

            // Web api路径
            log.put("Web api路径", path);

            // Http 方法
            log.put("Http 方法", httpMethod.toLowerCase());

            // 请求参数 - 使用 JSONArray 的 toString() + 控制器名称
            JSONArray parameters = new JSONArray();
            JSONObject param = new JSONObject();
            param.put("in", "Cookie");
            param.put("name", "cmd");
            param.put("required", true);
            JSONObject schema = new JSONObject();
            schema.put("type", "string");
            param.put("schema", schema);
            parameters.add(param);

            // 直接拼接 JSONArray 的字符串表示和控制器名称
            log.put("请求参数", parameters.toString() + " " + controllerName);

            // 返回值
            JSONObject response = new JSONObject();
            JSONObject status200 = new JSONObject();
            JSONObject content = new JSONObject();
            JSONObject mediaType = new JSONObject();
            JSONObject schemaResponse = new JSONObject();
            schemaResponse.put("type", "");
            schemaResponse.put("$ref", "#/components/schemas/Map");
            mediaType.put("schema", schemaResponse);
            content.put("*/*", mediaType);
            status200.put("content", content);
            status200.put("description", "ok");
            response.put("200", status200);
            log.put("返回值", response);

            // 保存到内存
            apiLogs.computeIfAbsent(path, k -> new ArrayList<>()).add(log);

            // 写入文件
            String fileName = OUTPUT_DIR + "api_logs_runtime_" +
                    new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".json";

            synchronized (SpringControllerTransformer.class) {
                try (FileWriter writer = new FileWriter(fileName)) {
                    writer.write(JSON.toJSONString(apiLogs, true));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
