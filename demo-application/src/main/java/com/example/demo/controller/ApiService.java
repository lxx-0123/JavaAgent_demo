package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ApiService {
    private final Class<?> controllerClass = com.example.demo.controller.TestController.class;

    private static final String OUTPUT_DIR = "D:/api-logs/";

    public ApiService() {
        // 创建输出目录
        new File(OUTPUT_DIR).mkdirs();
    }

    private List<Class<?>> findControllers() {
        List<Class<?>> controllers = new ArrayList<>();
        try {
            // 扫描 controller 包下的所有类
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);

            // 获取所有类文件
            Resource[] resources = resolver.getResources("classpath*:com/example/demo/controller/**/*.class");

            for (Resource resource : resources) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                String className = metadataReader.getClassMetadata().getClassName();

                Class<?> clazz = Class.forName(className);
                // 检查是否是Controller类（有@RestController或@Controller注解）
                if (clazz.isAnnotationPresent(RestController.class) ||
                        clazz.isAnnotationPresent(Controller.class)) {
                    controllers.add(clazz);
                }
            }
        } catch (Exception e) {
            System.err.println("扫描Controller失败: " + e.getMessage());
            e.printStackTrace();
        }
        return controllers;
    }

    public void getApiOnStartup() {
        try {
            List<JSONObject> allApiInfo = new ArrayList<>();
            List<Class<?>> controllers = findControllers();

            for (Class<?> controllerClass : controllers) {
                allApiInfo.addAll(fetchApiData(controllerClass));
            }

            // 创建一个 Map 来组织 API 信息
            Map<String, List<JSONObject>> apiMap = new HashMap<>();
            for (JSONObject apiInfo : allApiInfo) {
                String path = apiInfo.getString("Web api路径");
                apiMap.computeIfAbsent(path, k -> new ArrayList<>()).add(apiInfo);
            }

            // 生成文件名（包含时间戳）
            String fileName = OUTPUT_DIR + "api_logs_startup_" +
                    new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".json";

            // 写入文件
            try (FileWriter writer = new FileWriter(fileName)) {
                writer.write(JSON.toJSONString(apiMap, true));
            }

        } catch (Exception e) {
            System.err.println("获取API失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<JSONObject> fetchApiData(Class<?> controllerClass) {
        List<JSONObject> apiList = new ArrayList<>();

        try {
            // 获取类级别的RequestMapping注解
            RequestMapping classMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
            String baseUrl = "";
            if (classMapping != null && classMapping.value().length > 0) {
                baseUrl = classMapping.value()[0];
            }

            // 获取所有方法
            Method[] methods = controllerClass.getDeclaredMethods();
            for (Method method : methods) {
                JSONObject apiInfo = new JSONObject();

                // 处理URL和HTTP方法
                String url = baseUrl;
                String httpMethod = "";

                // 处理各种Mapping注解
                GetMapping getMapping = AnnotationUtils.findAnnotation(method, GetMapping.class);
                PostMapping postMapping = AnnotationUtils.findAnnotation(method, PostMapping.class);
                PutMapping putMapping = AnnotationUtils.findAnnotation(method, PutMapping.class);
                DeleteMapping deleteMapping = AnnotationUtils.findAnnotation(method, DeleteMapping.class);
                RequestMapping methodMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);

                if (getMapping != null && getMapping.value().length > 0) {
                    url += getMapping.value()[0];
                    httpMethod = "get";
                } else if (postMapping != null && postMapping.value().length > 0) {
                    url += postMapping.value()[0];
                    httpMethod = "post";
                } else if (putMapping != null && putMapping.value().length > 0) {
                    url += putMapping.value()[0];
                    httpMethod = "put";
                } else if (deleteMapping != null && deleteMapping.value().length > 0) {
                    url += deleteMapping.value()[0];
                    httpMethod = "delete";
                } else if (methodMapping != null && methodMapping.value().length > 0) {
                    url += methodMapping.value()[0];
                    httpMethod = methodMapping.method().length > 0 ?
                            methodMapping.method()[0].toString().toLowerCase() : "get";
                }

                if (url.isEmpty() || httpMethod.isEmpty()) {
                    continue; // 跳过没有映射的方法
                }

                apiInfo.put("Web api路径", url);
                apiInfo.put("Http 方法", httpMethod);

                // 处理参数信息
                JSONArray parameters = new JSONArray();
                for (Parameter param : method.getParameters()) {
                    JSONObject paramInfo = new JSONObject();
                    paramInfo.put("in", getParameterType(param));
                    paramInfo.put("name", param.getName());
                    paramInfo.put("required", true);

                    JSONObject schema = new JSONObject();
                    schema.put("type", param.getType().getSimpleName().toLowerCase());
                    paramInfo.put("schema", schema);

                    parameters.add(paramInfo);
                }

                apiInfo.put("请求参数", parameters.toString() + " " + controllerClass.getName());

                // 处理返回值
                JSONObject response = new JSONObject();
                JSONObject status200 = new JSONObject();
                JSONObject content = new JSONObject();
                JSONObject mediaType = new JSONObject();
                JSONObject schema = new JSONObject();
                schema.put("type", "");
                schema.put("$ref", "#/components/schemas/Map");
                mediaType.put("schema", schema);
                content.put("*/*", mediaType);
                status200.put("content", content);
                status200.put("description", "ok");
                response.put("200", status200);
                apiInfo.put("返回值", response);

                apiList.add(apiInfo);
            }
        } catch (Exception e) {
            System.err.println("处理Controller失败: " + controllerClass.getName());
            e.printStackTrace();
        }

        return apiList;
    }


    private String getParameterType(Parameter param) {
        if (param.getAnnotation(RequestParam.class) != null) {
            return "Query";
        } else if (param.getAnnotation(RequestBody.class) != null) {
            return "Body";
        } else if (param.getAnnotation(PathVariable.class) != null) {
            return "Path";
        } else if (param.getAnnotation(RequestHeader.class) != null) {
            return "Header";
        } else if (param.getAnnotation(CookieValue.class) != null) {
            return "Cookie";
        }
        return "Query"; // 默认作为查询参数
    }
}