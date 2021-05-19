package com.personal.meetup.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.personal.meetup.model.CallMediaPipeline;
import com.personal.meetup.model.UserSession;
import com.personal.meetup.registry.UserRegistry;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallHandler extends TextWebSocketHandler {

    @Autowired
    private Gson gson;

    @Autowired
    private KurentoClient kurentoClient;

    @Autowired
    private UserRegistry userRegistry;

    private final ConcurrentHashMap<String, CallMediaPipeline> pipelines = new ConcurrentHashMap<>();
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
        response.addProperty("id", "registerResponse");
        response.addProperty("response", responseMsg);
        caller.sendMessage(response);
    }

    private void call(UserSession caller, JsonObject jsonMessage) throws IOException {
        String to = jsonMessage.get("to").getAsString();
        String from = jsonMessage.get("from").getAsString();
        JsonObject response = new JsonObject();

        if (userRegistry.exists(to)) {
            UserSession callee = userRegistry.getByName(to);
            caller.setSdpOffer(jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString());
            caller.setCallingTo(to);

            response.addProperty("id", "incomingCall");
            response.addProperty("from", from);

            callee.sendMessage(response);
            callee.setCallingFrom(from);
        } else {
            response.addProperty("id", "callResponse");
            response.addProperty("response", "rejected: user '" + to + "' is not registered");

            caller.sendMessage(response);
        }
    }

    private void incomingCallResponse(final UserSession callee, JsonObject jsonMessage) throws IOException {
        String callResponse = jsonMessage.get("callResponse").getAsString();
        String from = jsonMessage.get("from").getAsString();
        final UserSession calleer = userRegistry.getByName(from);
        String to = calleer.getCallingTo();

        if ("accept".equals(callResponse)) {
            log.debug("Accepted call from '{}' to '{}'", from, to);

            CallMediaPipeline pipeline = null;
            try {
                pipeline = new CallMediaPipeline(kurentoClient);
                pipelines.put(calleer.getSessionId(), pipeline);
                pipelines.put(callee.getSessionId(), pipeline);

                String calleeSdpOffer = jsonMessage.get("sdpOffer").getAsString();
                callee.setWebRtcEndpoint(pipeline.getCalleeWebRtcEp());
                pipeline.getCalleeWebRtcEp().addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
                    @Override
                    public void onEvent(IceCandidateFoundEvent event) {
                        JsonObject response = new JsonObject();
                        response.addProperty("id", "iceCandidate");
                        response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                        try {
                            synchronized (callee.getSession()) {
                                callee.getSession().sendMessage(new TextMessage(response.toString()));
                            }
                        } catch (IOException e) {
                            log.debug(e.getMessage());
                        }
                    }
                });

                String calleeSdpAnswer = pipeline.generateSdpAnswerForCallee(calleeSdpOffer);
                String callerSdpOffer = userRegistry.getByName(from).getSdpOffer();
                calleer.setWebRtcEndpoint(pipeline.getCallerWebRtcEp());
                pipeline.getCallerWebRtcEp().addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                    @Override
                    public void onEvent(IceCandidateFoundEvent event) {
                        JsonObject response = new JsonObject();
                        response.addProperty("id", "iceCandidate");
                        response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                        try {
                            synchronized (calleer.getSession()) {
                                calleer.getSession().sendMessage(new TextMessage(response.toString()));
                            }
                        } catch (IOException e) {
                            log.debug(e.getMessage());
                        }
                    }
                });

                String callerSdpAnswer = pipeline.generateSdpAnswerForCaller(callerSdpOffer);

                JsonObject startCommunication = new JsonObject();
                startCommunication.addProperty("id", "startCommunication");
                startCommunication.addProperty("sdpAnswer", calleeSdpAnswer);

                synchronized (callee) {
                    callee.sendMessage(startCommunication);
                }

                pipeline.getCalleeWebRtcEp().gatherCandidates();

                JsonObject response = new JsonObject();
                response.addProperty("id", "callResponse");
                response.addProperty("response", "accepted");
                response.addProperty("sdpAnswer", callerSdpAnswer);

                synchronized (calleer) {
                    calleer.sendMessage(response);
                }

                pipeline.getCallerWebRtcEp().gatherCandidates();

            } catch (Throwable t) {
                log.error(t.getMessage(), t);

                if (pipeline != null) {
                    pipeline.release();
                }

                pipelines.remove(calleer.getSessionId());
                pipelines.remove(callee.getSessionId());

                JsonObject response = new JsonObject();
                response.addProperty("id", "callResponse");
                response.addProperty("response", "rejected");
                calleer.sendMessage(response);

                response = new JsonObject();
                response.addProperty("id", "stopCommunication");
                callee.sendMessage(response);
            }

        } else {
            JsonObject response = new JsonObject();
            response.addProperty("id", "callResponse");
            response.addProperty("response", "rejected");
            calleer.sendMessage(response);
        }
    }

    public void stop(WebSocketSession session) throws IOException {
        String sessionId = session.getId();
        if (pipelines.containsKey(sessionId)) {
            pipelines.get(sessionId).release();
            CallMediaPipeline pipeline = pipelines.remove(sessionId);
            pipeline.release();

            // Both users can stop the communication. A 'stopCommunication'
            // message will be sent to the other peer.
            UserSession stopperUser = userRegistry.getBySession(session);
            if (stopperUser != null) {
                UserSession stoppedUser = (stopperUser.getCallingFrom() != null)
                        ? userRegistry.getByName(stopperUser.getCallingFrom())
                        : stopperUser.getCallingTo() != null
                        ? userRegistry.getByName(stopperUser.getCallingTo())
                        : null;

                if (stoppedUser != null) {
                    JsonObject message = new JsonObject();
                    message.addProperty("id", "stopCommunication");
                    stoppedUser.sendMessage(message);
                    stoppedUser.clear();
                }
                stopperUser.clear();
            }

        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        stop(session);
        userRegistry.removeBySession(session);
    }


}
