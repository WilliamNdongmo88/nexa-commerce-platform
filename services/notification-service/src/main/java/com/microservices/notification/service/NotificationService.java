package com.microservices.notification.service;

import com.microservices.notification.entity.Notification;
import com.microservices.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    /**
     * Crée une notification "SIMULATED_SENT" (pas d'envoi réel en dev).
     * Idempotent : si eventId existe déjà, ne recrée pas.
     */
    @Transactional
    public void createNotification(Notification notification) {
        if (notification.getEventId() != null && repository.existsByEventId(notification.getEventId())) {
            log.debug("Notification already processed for event {}", notification.getEventId());
            return;
        }

        // En dev : on simule l'envoi immédiat
        notification.setStatus(Notification.NotificationStatus.SIMULATED_SENT);
        notification.setSentAt(Instant.now());

        Notification saved = repository.save(notification);
        log.info("📧 Notification [{}] created for {}: {}",
                saved.getType(), saved.getRecipientEmail(), saved.getSubject());
    }

    @Transactional(readOnly = true)
    public Page<Notification> listByRecipient(UUID recipientId, Pageable pageable) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> listAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id);
    }
}