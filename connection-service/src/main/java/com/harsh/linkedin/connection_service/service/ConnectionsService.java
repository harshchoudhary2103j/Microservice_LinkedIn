
package com.harsh.linkedin.connection_service.service;

import com.harsh.linkedin.connection_service.auth.UserContextHolder;
import com.harsh.linkedin.connection_service.entity.Person;
import com.harsh.linkedin.connection_service.event.AcceptConnectionRequestEvent;
import com.harsh.linkedin.connection_service.event.SendConnectionRequestEvent;
import com.harsh.linkedin.connection_service.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ConnectionsService {

    private final PersonRepository personRepository;
    private final KafkaTemplate<Long, SendConnectionRequestEvent> sendRequestKafkaTemplate;
    private final KafkaTemplate<Long, AcceptConnectionRequestEvent> acceptRequestKafkaTemplate;

    public List<Person> getFirstDegreeConnections() {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("Getting first degree connections for user with id: {}", userId);

        return personRepository.getFirstDegreeConnections(userId);
    }

    public Boolean sendConnectionRequest(Long receiverId) {

        Long senderId = UserContextHolder.getCurrentUserId();
        log.info("Trying to send connection request, sender: {}, receiver: {}", senderId, receiverId);

        // ❌ Self connection check
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Sender and receiver cannot be the same");
        }

        // ❌ Ensure users exist in Neo4j (important due to Kafka sync)
        if (!personRepository.existsByUserId(senderId) ||
                !personRepository.existsByUserId(receiverId)) {
            throw new RuntimeException("User not present in graph DB (Kafka sync delay)");
        }

        // ❌ Already connected
        if (personRepository.alreadyConnected(senderId, receiverId)) {
            throw new RuntimeException("Users are already connected");
        }

        // ❌ Already requested
        if (personRepository.connectionRequestExists(senderId, receiverId)) {
            throw new RuntimeException("Connection request already sent");
        }

        // ✅ DB operation first
        personRepository.addConnectionRequest(senderId, receiverId);
        log.info("✅ Connection request created in DB: {} -> {}", senderId, receiverId);

        // ✅ Kafka event (non-blocking)
        try {
            SendConnectionRequestEvent event = SendConnectionRequestEvent.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .build();

            sendRequestKafkaTemplate.send("send-connection-request-topic", event);

        } catch (Exception e) {
            log.error("❌ Failed to send Kafka event for connection request", e);
        }

        return true;
    }

    public Boolean acceptConnectionRequest(Long senderId) {

        Long receiverId = UserContextHolder.getCurrentUserId();
        log.info("Accepting connection request: {} -> {}", senderId, receiverId);

        // ❌ Validate users exist
        if (!personRepository.existsByUserId(senderId) ||
                !personRepository.existsByUserId(receiverId)) {
            throw new RuntimeException("User not found in graph DB");
        }

        // ❌ Check request exists
        if (!personRepository.connectionRequestExists(senderId, receiverId)) {
            throw new RuntimeException("No connection request exists to accept");
        }

        // ✅ Update DB
        personRepository.acceptConnectionRequest(senderId, receiverId);
        log.info("✅ Connection established between {} and {}", senderId, receiverId);

        // ✅ Kafka event
        try {
            AcceptConnectionRequestEvent event = AcceptConnectionRequestEvent.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .build();

            acceptRequestKafkaTemplate.send("accept-connection-request-topic", event);

        } catch (Exception e) {
            log.error("❌ Failed to send Kafka accept event", e);
        }

        return true;
    }

    public Boolean rejectConnectionRequest(Long senderId) {

        Long receiverId = UserContextHolder.getCurrentUserId();
        log.info("Rejecting connection request: {} -> {}", senderId, receiverId);

        // ❌ Check request exists
        if (!personRepository.connectionRequestExists(senderId, receiverId)) {
            throw new RuntimeException("No connection request exists");
        }

        // ✅ Delete request
        personRepository.rejectConnectionRequest(senderId, receiverId);
        log.info("✅ Connection request rejected: {} -> {}", senderId, receiverId);

        return true;
    }
}

