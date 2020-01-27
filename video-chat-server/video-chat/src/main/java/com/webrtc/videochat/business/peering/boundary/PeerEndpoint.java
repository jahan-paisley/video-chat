package com.webrtc.videochat.business.peering.boundary;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ServerEndpoint("/peers")
public class PeerEndpoint implements PeeringEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeerEndpoint.class);
    private static Set<Session> sessions = new HashSet<>();

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        LOGGER.info("session {} connected", session.getId());
        sessions.add(session);

        if (sessions.size() < 2) {
            if (sessions.size() == 1) {
                send(session, "CreatePeer");
            }
        } else {
            send(session, "SessionActive");
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        Event event;
        try {
            event = new Gson().fromJson(message, Event.class);
        } catch (JsonSyntaxException ex) {
            Map map = new Gson().fromJson(message, Map.class);
            event = new Event((String) map.get("name"), new Gson().toJson(map.get("data")));
        }

        switch (event.getName()) {
            case "NewClient":
                onNewClient();
                break;
            case "Offer":
                onOffer(event.getData());
                break;
            case "Answer":
                onAnswer(event.getData());
                break;
        }

        LOGGER.info("message received: {}", message);
    }

    @OnClose
    public void onClose(Session session, CloseReason cr) {
        LOGGER.info("session {} closed", session);
        sessions.remove(session);
    }

    @OnError
    public void onErrorCallback(Session s, Throwable t) {
        LOGGER.error("error for session " + s.getId(), t);
    }

    public void send(Session session, String message) {
        send(session, new Event(message, ""));
    }

    public void send(Session session, Event message) {
        try {
            session.getBasicRemote().sendText(new Gson().toJson(message));
        } catch (IOException e) {
            LOGGER.error("error sending message to session", e);
        }
    }

    public void broadcast(Event message) {
        sessions.stream().forEach(session -> send(session, message));
    }

    @Override
    public void onNewClient() {

    }

    @Override
    public void onOffer(String data) {
        broadcast(new Event("BackOffer", data));
    }

    @Override
    public void onAnswer(String data) {
        broadcast(new Event("BackAnswer", data));
    }
}
