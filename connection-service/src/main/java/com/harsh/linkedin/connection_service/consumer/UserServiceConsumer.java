package com.harsh.linkedin.connection_service.consumer;

import com.harsh.linkedin.connection_service.entity.Person;
import com.harsh.linkedin.connection_service.repository.PersonRepository;
import com.harsh.linkedin.UserService.event.UserCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceConsumer {

    private final PersonRepository personRepository;

    @KafkaListener(topics = "user-created-topic", groupId = "connection-service-group")
    public void handleUserCreated(UserCreatedEvent event) {

        log.info("🔥 Received UserCreatedEvent for userId: {}", event.getUserId());

        // ✅ Check if already exists (important for idempotency)
        boolean exists = personRepository.existsByUserId(event.getUserId());

        if (exists) {
            log.warn("⚠️ User already exists in Neo4j, skipping userId: {}", event.getUserId());
            return;
        }

        // ✅ Create new Person node
        Person person = new Person();
        person.setUserId(event.getUserId());
        person.setName(event.getName());

        personRepository.save(person);

        log.info("✅ Person node created in Neo4j for userId: {}", event.getUserId());
    }
}