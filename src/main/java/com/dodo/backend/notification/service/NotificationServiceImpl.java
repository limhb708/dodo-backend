package com.dodo.backend.notification.service;

import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationReadUpdateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationItemResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.PageInfoResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.UnreadNotificationCountResponse;
import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.notification.exception.NotificationErrorCode;
import com.dodo.backend.notification.exception.NotificationException;
import com.dodo.backend.notification.repository.NotificationRepository;
import com.dodo.backend.reaction.entity.Reaction;
import com.dodo.backend.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.dodo.backend.notification.exception.NotificationErrorCode.INVALID_REQUEST;
import static com.dodo.backend.notification.exception.NotificationErrorCode.NOTIFICATION_DELETE_FORBIDDEN;
import static com.dodo.backend.notification.exception.NotificationErrorCode.NOTIFICATION_NOT_FOUND;
import static com.dodo.backend.notification.exception.NotificationErrorCode.NOTIFICATION_UPDATE_FORBIDDEN;

/**
 * {@link NotificationService} 구현체입니다.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String READ_SUCCESS_MESSAGE = "알림이 성공적으로 읽음 처리되었습니다.";
    private static final String READ_ALL_SUCCESS_MESSAGE = "모든 알림이 성공적으로 읽음 처리되었습니다.";
    private static final String COMMENT_NOTIFICATION_TITLE = "게시글에 새 댓글이 달렸습니다.";
    private static final int COMMENT_NOTIFICATION_BODY_MAX_LENGTH = 50;
    private static final String BOARD_REACTION_TARGET_NAME = "내 게시글";
    private static final String HISTORY_REACTION_TARGET_NAME = "내 활동 기록";

    private final NotificationRepository notificationRepository;
    private final FcmNotificationSender fcmNotificationSender;

    /**
     * 알림 목록을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param page 조회할 페이지 번호
     * @param size 페이지당 알림 수
     * @param isRead 읽음 여부 필터
     * @param type 알림 유형 필터
     * @return 알림 목록 조회 결과
     */
    @Transactional(readOnly = true)
    @Override
    public NotificationListResponse getNotifications(UUID userId, int page, int size, Boolean isRead, String type) {
        validatePageRequest(userId, page, size);
        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "notificationCreatedAt").and(Sort.by(Sort.Direction.DESC, "notificationId"))
        );
        List<NotificationType> types = parseTypes(type);

        Page<Notification> notifications = notificationRepository.findAll(
                buildNotificationSpecification(userId, isRead, types),
                pageable
        );
        return NotificationListResponse.builder()
                .pageInfo(PageInfoResponse.toDto(notifications, page))
                .data(notifications.getContent().stream().map(NotificationItemResponse::toDto).toList())
                .build();
    }

    /**
     * 특정 알림의 읽음 여부를 변경합니다.
     *
     * @param userId 요청 사용자 ID
     * @param notificationId 읽음 여부를 변경할 알림 ID
     * @param request 읽음 여부 변경 요청
     * @return 읽음 처리 성공 메시지
     */
    @Transactional
    @Override
    public NotificationSimpleResponse updateReadStatus(UUID userId, Long notificationId, NotificationReadUpdateRequest request) {
        if (request == null || request.getIsRead() == null) {
            throw new NotificationException(INVALID_REQUEST);
        }
        Notification notification = findOwnedNotification(userId, notificationId, NOTIFICATION_UPDATE_FORBIDDEN);
        notification.updateReadStatus(request.getIsRead());
        return NotificationSimpleResponse.toDto(READ_SUCCESS_MESSAGE);
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param userId 요청 사용자 ID
     * @param notificationId 삭제할 알림 ID
     */
    @Transactional
    @Override
    public void deleteNotification(UUID userId, Long notificationId) {
        Notification notification = findOwnedNotification(userId, notificationId, NOTIFICATION_DELETE_FORBIDDEN);
        notificationRepository.delete(notification);
    }

    /**
     * 읽지 않은 알림 개수를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    @Override
    public UnreadNotificationCountResponse getUnreadCount(UUID userId) {
        validateUserId(userId);
        return UnreadNotificationCountResponse.toDto(notificationRepository.countByUserUsersIdAndIsReadFalse(userId));
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * @param userId 요청 사용자 ID
     * @return 전체 읽음 처리 성공 메시지
     */
    @Transactional
    @Override
    public NotificationSimpleResponse readAll(UUID userId) {
        validateUserId(userId);
        notificationRepository.markAllAsRead(userId);
        return NotificationSimpleResponse.toDto(READ_ALL_SUCCESS_MESSAGE);
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param userId 요청 사용자 ID
     */
    @Transactional
    @Override
    public void deleteAll(UUID userId) {
        validateUserId(userId);
        notificationRepository.deleteAllByUserUsersId(userId);
    }

    /**
     * 댓글 작성 시 게시글 작성자에게 COMMENT 알림을 생성하고 커밋 이후 FCM 푸시를 발송합니다.
     *
     * @param comment 생성된 댓글
     */
    @Transactional
    @Override
    public void notifyCommentCreated(Comment comment) {
        if (comment == null || comment.getBoard() == null || comment.getUser() == null || comment.getBoard().getUser() == null) {
            throw new NotificationException(INVALID_REQUEST);
        }

        UUID commenterId = comment.getUser().getUsersId();
        UUID boardWriterId = comment.getBoard().getUser().getUsersId();
        if (commenterId == null || boardWriterId == null || commenterId.equals(boardWriterId)) {
            return;
        }
        if (!Boolean.TRUE.equals(comment.getBoard().getUser().getNotificationEnabled())) {
            return;
        }

        createNotificationAndSendAfterCommit(
                comment.getBoard().getUser(),
                COMMENT_NOTIFICATION_TITLE,
                buildCommentNotificationBody(comment.getCommentContent()),
                NotificationType.COMMENT,
                comment.getBoard().getBoardId()
        );
    }

    /**
     * 반응 작성 시 대상 작성자에게 REACTION 알림을 생성하고 커밋 이후 FCM 푸시를 발송합니다.
     *
     * @param reaction 생성된 반응
     */
    @Transactional
    @Override
    public void notifyReactionCreated(Reaction reaction) {
        if (reaction == null || reaction.getUser() == null || reaction.getReactionType() == null) {
            throw new NotificationException(INVALID_REQUEST);
        }

        ReactionNotificationTarget target = resolveReactionNotificationTarget(reaction);
        UUID reactorId = reaction.getUser().getUsersId();
        UUID recipientId = target.recipient().getUsersId();
        if (reactorId == null || recipientId == null || reactorId.equals(recipientId)) {
            return;
        }
        if (!Boolean.TRUE.equals(target.recipient().getNotificationEnabled())) {
            return;
        }

        String reactionName = resolveReactionName(reaction);
        createNotificationAndSendAfterCommit(
                target.recipient(),
                target.targetName() + "에 " + reactionName + "가 눌렸습니다.",
                "누군가 " + reactionName + "를 눌렀습니다.",
                NotificationType.REACTION,
                target.relatedId()
        );
    }

    private Specification<Notification> buildNotificationSpecification(UUID userId, Boolean isRead, List<NotificationType> types) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user").get("usersId"), userId));

            if (isRead != null) {
                predicates.add(criteriaBuilder.equal(root.get("isRead"), isRead));
            }
            if (!types.isEmpty()) {
                predicates.add(root.get("notificationType").in(types));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String buildCommentNotificationBody(String commentContent) {
        String content = commentContent == null ? "" : commentContent.strip();
        if (content.length() > COMMENT_NOTIFICATION_BODY_MAX_LENGTH) {
            content = content.substring(0, COMMENT_NOTIFICATION_BODY_MAX_LENGTH) + "...";
        }
        return "새 댓글: '" + content + "'";
    }

    private ReactionNotificationTarget resolveReactionNotificationTarget(Reaction reaction) {
        if (reaction.getBoard() != null) {
            if (reaction.getBoard().getUser() == null || reaction.getBoard().getBoardId() == null) {
                throw new NotificationException(INVALID_REQUEST);
            }
            return new ReactionNotificationTarget(
                    reaction.getBoard().getUser(),
                    reaction.getBoard().getBoardId(),
                    BOARD_REACTION_TARGET_NAME
            );
        }

        if (reaction.getHistory() != null) {
            if (reaction.getHistory().getUser() == null || reaction.getHistory().getHistoryId() == null) {
                throw new NotificationException(INVALID_REQUEST);
            }
            return new ReactionNotificationTarget(
                    reaction.getHistory().getUser(),
                    reaction.getHistory().getHistoryId(),
                    HISTORY_REACTION_TARGET_NAME
            );
        }

        throw new NotificationException(INVALID_REQUEST);
    }

    private String resolveReactionName(Reaction reaction) {
        return switch (reaction.getReactionType()) {
            case LIKE -> "좋아요";
            case DISLIKE -> "싫어요";
        };
    }

    private void createNotificationAndSendAfterCommit(User recipient, String title, String body, NotificationType type, Long relatedId) {
        Notification notification = Notification.builder()
                .user(recipient)
                .notificationTitle(title)
                .notificationBody(body)
                .notificationType(type)
                .relatedId(relatedId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        sendPushAfterCommit(List.of(recipient), title, body, type, relatedId);
    }

    private void sendPushAfterCommit(List<User> recipients, String title, String body, NotificationType type, Long relatedId) {
        Runnable pushTask = () -> fcmNotificationSender.sendToUsers(recipients, title, body, type, relatedId);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            pushTask.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushTask.run();
            }
        });
    }

    private record ReactionNotificationTarget(User recipient, Long relatedId, String targetName) {
    }

    private Notification findOwnedNotification(UUID userId, Long notificationId, NotificationErrorCode forbiddenErrorCode) {
        validateUserId(userId);
        validateId(notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NOTIFICATION_NOT_FOUND));
        if (notification.getUser() == null || !userId.equals(notification.getUser().getUsersId())) {
            throw new NotificationException(forbiddenErrorCode);
        }
        return notification;
    }

    private List<NotificationType> parseTypes(String type) {
        if (type == null || type.isBlank()) {
            return List.of();
        }
        try {
            List<NotificationType> types = Arrays.stream(type.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(value -> NotificationType.valueOf(value.toUpperCase(Locale.ROOT)))
                    .distinct()
                    .toList();

            if (types.isEmpty()) {
                throw new NotificationException(INVALID_REQUEST);
            }
            return types;
        } catch (IllegalArgumentException e) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private void validatePageRequest(UUID userId, int page, int size) {
        if (userId == null || page <= 0 || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }
}
