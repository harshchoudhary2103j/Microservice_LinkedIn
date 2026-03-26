package com.harsh.linkedIn.notification_service.consumer;

import com.harsh.linkedIn.connection_service.event.AcceptConnectionRequestEvent;
import com.harsh.linkedIn.connection_service.event.SendConnectionRequestEvent;
import com.harsh.linkedIn.notification_service.service.SendNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionServiceConsumer {
    private final SendNotification sendNotification;
    @KafkaListener(topics = "send-connection-request-topic")
    public void handleSendConnectionRequest(SendConnectionRequestEvent sendConnectionRequestEvent){
        String message = "You have recieved a connection request from user with id: %d"+sendConnectionRequestEvent.getSenderId();
        sendNotification.send(sendConnectionRequestEvent.getReceiverId(), message);

    }
    @KafkaListener(topics = "accept-connection-request-topic")
    public void handleAcceptConnectionRequest(AcceptConnectionRequestEvent acceptConnectionRequestEvent){
        String message = "Your connection request is accpeted by user with id: %d"+acceptConnectionRequestEvent.getReceiverId();
        sendNotification.send(acceptConnectionRequestEvent.getSenderId(), message);

    }


}
