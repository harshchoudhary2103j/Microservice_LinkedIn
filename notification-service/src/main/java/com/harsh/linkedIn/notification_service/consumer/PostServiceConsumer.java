package com.harsh.linkedIn.notification_service.consumer;

import com.harsh.linkedIn.notification_service.clients.ConnectionsClient;
import com.harsh.linkedIn.notification_service.dto.PersonDto;
import com.harsh.linkedIn.notification_service.entity.Notification;
import com.harsh.linkedIn.notification_service.repository.NotificationRepository;
import com.harsh.linkedIn.notification_service.service.SendNotification;
import com.harsh.linkedIn.posts_service.event.PostCreatedEvent;
import com.harsh.linkedIn.posts_service.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostServiceConsumer {
    private final ConnectionsClient connectionsClient;
    private final NotificationRepository notificationRepository;
    private final SendNotification sendNotification;
    @KafkaListener(topics = "post-created-topic")
    public void handlePostCreated(PostCreatedEvent postCreatedEvent){
        List<PersonDto> connectionClient = connectionsClient.getFirstConnections(postCreatedEvent.getCreatorId());
        if(connectionClient.isEmpty()){
            log.info("No connections of user "+postCreatedEvent.getCreatorId()+" found!");
        }
        for(PersonDto connection: connectionClient){
            sendNotification.send(connection.getUserId(), "Your connection: "+postCreatedEvent.getCreatorId()+" has created a post, Check it out!");
        }


    }

    @KafkaListener(topics = "post-liked-topic")
    public void handlePostLiked(PostLikedEvent postLikedEvent){
        String message = String.format("Your post, %d has been liked by %d",postLikedEvent.getPostId(),postLikedEvent.getLikedByUserId());
        sendNotification.send(postLikedEvent.getCreatorId(),message);

    }



}
