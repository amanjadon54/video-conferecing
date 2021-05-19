package com.personal.meetup;

import com.personal.meetup.handler.CallHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
public class MeetupApp implements WebSocketConfigurer {

    @Autowired
    private CallHandler callHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(callHandler, "/call");
    }

    public static void main(String[] s) {
        SpringApplication.run(MeetupApp.class);
    }
}
