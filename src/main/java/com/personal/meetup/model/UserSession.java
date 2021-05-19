package com.personal.meetup.model;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserSession {

    private static final Logger log = LoggerFactory.getLogger(UserSession.class);

    @Getter
    private final String name;

    @Getter
    private final WebSocketSession session;

    @Getter
    @Setter
    private String sdpOffer;
    @Getter
    @Setter
    private String callingFrom;
    @Getter
    @Setter
    private String callingTo;

    @Getter
    private WebRtcEndpoint webRtcEndpoint;
    @Getter
    @Setter
    private WebRtcEndpoint playingWebRtcEndpoint;

    @Getter
    private final List<IceCandidate> candidates;

    public UserSession(String name, WebSocketSession session) {
        this.name = name;
        this.session = session;
        this.candidates = new ArrayList<>();
    }


    public void sendMessage(JsonObject message) throws IOException {
        log.debug("Sending message from user '{}': {}", name, message);
        session.sendMessage(new TextMessage(message.toString()));
    }

    public String getSessionId() {
        return session.getId();
    }

    public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
        this.webRtcEndpoint = webRtcEndpoint;

        for (IceCandidate e : candidates) {
            this.webRtcEndpoint.addIceCandidate(e);
        }
        this.candidates.clear();
    }

    public void addCandidate(IceCandidate candidate) {
        if (this.webRtcEndpoint != null) {
            this.webRtcEndpoint.addIceCandidate(candidate);
        } else {
            candidates.add(candidate);
        }
    }

    public void clear() {
        this.webRtcEndpoint = null;
        this.candidates.clear();
    }

}
