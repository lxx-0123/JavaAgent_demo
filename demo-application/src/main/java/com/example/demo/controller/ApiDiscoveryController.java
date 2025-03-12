package com.example.demo.controller;

import com.example.agent.model.ApiInfo;
import com.example.agent.transformer.ApiInfoCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api-discovery")
public class ApiDiscoveryController {

    @GetMapping("/apis")
    public List<ApiInfo> getAllApis() {
        return new ArrayList<>(ApiInfoCache.getAllApis());
    }
}