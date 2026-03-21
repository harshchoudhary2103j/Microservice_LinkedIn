package com.harsh.linkedIn.notification_service.repository;

import com.harsh.linkedIn.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
