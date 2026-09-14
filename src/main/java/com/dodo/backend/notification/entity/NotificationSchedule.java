package com.dodo.backend.notification.entity;

import com.dodo.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notification_schedule")
public class NotificationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_schedule_id")
    private Long notificationScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "notification_title", nullable = false, length = 255)
    private String notificationTitle;

    @Column(name = "notification_body", nullable = false, columnDefinition = "TEXT")
    private String notificationBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private NotificationScheduleTargetType targetType;

    @Column(name = "target_user_ids", columnDefinition = "TEXT")
    private String targetUserIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false)
    private NotificationScheduleRepeatType repeatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_status", nullable = false)
    private NotificationScheduleStatus scheduleStatus;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "processing_token", length = 36)
    private String processingToken;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    public void complete(LocalDateTime executedAt) {
        this.scheduleStatus = NotificationScheduleStatus.COMPLETED;
        this.executedAt = executedAt;
        clearProcessing();
    }

    public void reschedule(LocalDateTime nextScheduledAt, LocalDateTime executedAt) {
        this.scheduledAt = nextScheduledAt;
        this.executedAt = executedAt;
        this.scheduleStatus = NotificationScheduleStatus.PENDING;
        clearProcessing();
    }

    public void cancel() {
        this.scheduleStatus = NotificationScheduleStatus.CANCELED;
        clearProcessing();
    }

    private void clearProcessing() {
        this.processingToken = null;
        this.processingStartedAt = null;
    }
}
