package com.personal.conferencing.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConferencingConfiguration {

    @Bean
    public Gson gson() {
        return new GsonBuilder().create();
    }
}
