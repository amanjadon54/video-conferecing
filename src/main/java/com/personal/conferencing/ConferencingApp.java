package com.personal.conferencing;

import com.personal.conferencing.handler.CallHandler;
import com.personal.conferencing.registry.UserRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
public class ConferencingApp implements WebSocketConfigurer {

    @Autowired
    private CallHandler callHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(callHandler, "/call");
    }

    public static void main(String[] s) {
        SpringApplication.run(ConferencingApp.class);
    }
}
