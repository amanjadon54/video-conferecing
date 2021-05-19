package com.personal.conferencing.handler;

import com.google.gson.Gson;
import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class CallHandler extends TextWebSocketHandler {

    @Autowired
    private Gson gson;

    @Autowired
    private KurentoClient kurentoClient;

    private static final Logger log = LoggerFactory.getLogger(CallHandler.class);


}
