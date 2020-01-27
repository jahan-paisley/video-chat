package com.webrtc.videochat.business.peering.boundary;

public interface PeeringEvent {
    void onNewClient();

    void onOffer(String data);

    void onAnswer(String data);
}
