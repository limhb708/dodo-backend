package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationReadUpdateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.UnreadNotificationCountResponse;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.reaction.entity.Reaction;

import java.util.UUID;

/**
 * 알림 API 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface NotificationService {

    NotificationListResponse getNotifications(UUID userId, int page, int size, Boolean isRead, String type);

    NotificationSimpleResponse updateReadStatus(UUID userId, Long notificationId, NotificationReadUpdateRequest request);

    void deleteNotification(UUID userId, Long notificationId);

    UnreadNotificationCountResponse getUnreadCount(UUID userId);

    NotificationSimpleResponse readAll(UUID userId);

    void deleteAll(UUID userId);

    void notifyCommentCreated(Comment comment);

    void notifyReactionCreated(Reaction reaction);
}
