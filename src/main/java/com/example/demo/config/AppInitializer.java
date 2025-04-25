package com.example.demo.config;

import com.example.demo.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppInitializer implements ApplicationRunner {

    @Autowired
    private ApiService apiService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        apiService.executeOnStartup();
    }
}