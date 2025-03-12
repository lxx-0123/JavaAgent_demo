package com.example.agent;

import com.example.agent.transformer.SpringControllerTransformer;
import java.lang.instrument.Instrumentation;

public class ApiCollectorAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("API collector agent started!");
        inst.addTransformer(new SpringControllerTransformer());
    }
} 