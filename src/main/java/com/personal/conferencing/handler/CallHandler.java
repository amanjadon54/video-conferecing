package com.personal.conferencing.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.personal.conferencing.model.UserSession;
import com.personal.conferencing.registry.UserRegistry;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class CallHandler extends TextWebSocketHandler {

    @Autowired
    private Gson gson;

    @Autowired
    private KurentoClient kurentoClient;

    @Autowired
    private UserRegistry userRegistry;

    private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
        UserSession user = userRegistry.getBySession(session);

        String incomingMessgaeDebug = String.format("Incoming message from user '{}': {}", (user == null ? session.getId() : user), jsonMessage);
        log.debug(incomingMessgaeDebug);

        switch (jsonMessage.get("id").getAsString()) {
            case "register":
                try {
                    register(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "registerResponse");
                }
                break;
            case "call":
                try {
                    call(user, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "callResponse");
                }
                break;
            case "incomingCallResponse":
                incomingCallResponse(user, jsonMessage);
                break;
            case "onIceCandidate": {
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();
                if (user != null) {
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                            candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(cand);
                }
                break;
            }
            case "stop":
                stop(session);
                break;
            default:
                break;
        }
    }

    private void handleErrorResponse(Throwable t, WebSocketSession session,
                                     String responseId) throws IOException {
        stop(session);
        log.error(t.getMessage(), t);
        JsonObject response = new JsonObject();
        response.addProperty("id", responseId);
        response.addProperty("response", "rejected");
        response.addProperty("message", t.getMessage());
        session.sendMessage(new TextMessage(response.toString()));
    }

    private void register(WebSocketSession session, JsonObject jsonMessage) throws IOException {
        String name = jsonMessage.getAsJsonPrimitive("name").getAsString();

        UserSession caller = new UserSession(name, session);
        String responseMsg = "accepted";
        if (name.isEmpty()) {
            responseMsg = "rejected: empty user name";
        } else if (userRegistry.exists(name)) {
            responseMsg = "rejected: user '" + name + "' already registered";
        } else {
            userRegistry.register(caller);
        }

        JsonObject response = new JsonObject();
        response.addProperty("id", "resgisterResponse");
        response.addProperty("response", responseMsg);
        caller.sendMessage(response);
    }

    private void call(UserSession caller, JsonObject jsonMessage) throws IOException {

    }

    private void incomingCallResponse(final UserSession callee, JsonObject jsonMessage) throws IOException {

    }

    public void stop(WebSocketSession session) throws IOException {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        stop(session);
        userRegistry.removeBySession(session);
    }


}
