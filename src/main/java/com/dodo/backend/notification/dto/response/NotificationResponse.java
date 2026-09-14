package com.dodo.backend.notification.dto.response;

import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleRepeatType;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.entity.NotificationScheduleTargetType;
import com.dodo.backend.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 알림 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "알림 응답 DTO 그룹")
public class NotificationResponse {

    /**
     * 단순 메시지 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 단순 응답")
    public static class NotificationSimpleResponse {
        @Schema(description = "알림 처리 결과 메시지", example = "알림을 읽음 처리했습니다.")
        private String message;

        public static NotificationSimpleResponse toDto(String message) {
            return NotificationSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 페이지 정보 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "페이지 정보")
    public static class PageInfoResponse {
        @Schema(description = "현재 페이지 번호. API 응답 기준에 따른 페이지 번호입니다.", example = "0")
        private int page;
        @Schema(description = "페이지당 알림 개수", example = "20")
        private int size;
        @Schema(description = "조회 조건에 맞는 전체 알림 개수", example = "42")
        private long totalElements;
        @Schema(description = "전체 페이지 수", example = "3")
        private int totalPages;

        public static PageInfoResponse toDto(Page<?> page, int displayPage) {
            return PageInfoResponse.builder()
                    .page(displayPage)
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }
    }

    /**
     * 알림 목록 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 목록 아이템")
    public static class NotificationItemResponse {
        @Schema(description = "알림 고유 ID", example = "101")
        private Long notificationId;
        @Schema(description = "알림 제목", example = "새로운 공지사항이 등록되었습니다.")
        private String notificationTitle;
        @Schema(description = "알림 본문", example = "서비스 점검 안내를 확인해주세요.")
        private String notificationBody;
        @Schema(description = "알림 유형", example = "SYSTEM")
        private NotificationType notificationType;
        @Schema(description = "알림과 연결된 리소스 ID. COMMENT/게시글 REACTION은 boardId, 활동기록 REACTION은 historyId입니다.", example = "31", nullable = true)
        private Long relatedId;
        @Schema(description = "알림 읽음 여부", example = "false")
        private Boolean isRead;
        @Schema(description = "알림 생성 일시", example = "2026-09-14T15:30:00")
        private LocalDateTime createdAt;

        public static NotificationItemResponse toDto(Notification notification) {
            return NotificationItemResponse.builder()
                    .notificationId(notification.getNotificationId())
                    .notificationTitle(notification.getNotificationTitle())
                    .notificationBody(notification.getNotificationBody())
                    .notificationType(notification.getNotificationType())
                    .relatedId(notification.getRelatedId())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getNotificationCreatedAt())
                    .build();
        }
    }

    /**
     * 알림 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 목록 조회 응답")
    public static class NotificationListResponse {
        @Schema(description = "알림 목록의 페이지 정보")
        private PageInfoResponse pageInfo;
        @Schema(description = "조회된 알림 목록")
        private List<NotificationItemResponse> data;
    }

    /**
     * 읽지 않은 알림 개수 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "읽지 않은 알림 개수 응답")
    public static class UnreadNotificationCountResponse {
        @Schema(description = "현재 사용자가 읽지 않은 알림 개수", example = "5")
        private long unreadCount;

        public static UnreadNotificationCountResponse toDto(long unreadCount) {
            return UnreadNotificationCountResponse.builder()
                    .unreadCount(unreadCount)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 스케줄 등록 응답")
    public static class NotificationScheduleCreateResponse {
        @Schema(description = "알림 스케줄 등록 결과 메시지", example = "알림 스케줄이 성공적으로 등록되었습니다.")
        private String message;
        @Schema(description = "생성된 알림 스케줄 ID", example = "10")
        private Long scheduleId;
        @Schema(description = "알림 스케줄 처리 상태", example = "PENDING")
        private NotificationScheduleStatus scheduleStatus;
        @Schema(description = "알림 발송 예정 일시", example = "2026-09-20T09:00:00")
        private LocalDateTime scheduledAt;

        public static NotificationScheduleCreateResponse toDto(NotificationSchedule schedule) {
            return NotificationScheduleCreateResponse.builder()
                    .message("알림 스케줄이 성공적으로 등록되었습니다.")
                    .scheduleId(schedule.getNotificationScheduleId())
                    .scheduleStatus(schedule.getScheduleStatus())
                    .scheduledAt(schedule.getScheduledAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 스케줄 목록 아이템")
    public static class NotificationScheduleItemResponse {
        @Schema(description = "알림 스케줄 ID", example = "10")
        private Long scheduleId;
        @Schema(description = "알림 제목", example = "공지 알림")
        private String notificationTitle;
        @Schema(description = "알림 본문", example = "새로운 공지가 등록되었습니다.")
        private String notificationBody;
        @Schema(description = "알림 유형", example = "SYSTEM")
        private NotificationType notificationType;
        @Schema(description = "발송 대상 유형", example = "ALL")
        private NotificationScheduleTargetType targetType;
        @Schema(description = "targetType이 USERS일 때 발송 대상 사용자 ID 목록")
        private List<UUID> targetUserIds;
        @Schema(description = "반복 유형", example = "NONE")
        private NotificationScheduleRepeatType repeatType;
        @Schema(description = "알림 스케줄 처리 상태", example = "PENDING")
        private NotificationScheduleStatus scheduleStatus;
        @Schema(description = "알림 발송 예정 일시", example = "2026-09-20T09:00:00")
        private LocalDateTime scheduledAt;
        @Schema(description = "알림 스케줄 생성 일시", example = "2026-09-14T15:30:00")
        private LocalDateTime createdAt;
        @Schema(description = "알림 스케줄 마지막 실행 일시", example = "2026-09-20T09:00:00", nullable = true)
        private LocalDateTime executedAt;

        public static NotificationScheduleItemResponse toDto(NotificationSchedule schedule) {
            return NotificationScheduleItemResponse.builder()
                    .scheduleId(schedule.getNotificationScheduleId())
                    .notificationTitle(schedule.getNotificationTitle())
                    .notificationBody(schedule.getNotificationBody())
                    .notificationType(schedule.getNotificationType())
                    .targetType(schedule.getTargetType())
                    .targetUserIds(parseTargetUserIds(schedule.getTargetUserIds()))
                    .repeatType(schedule.getRepeatType())
                    .scheduleStatus(schedule.getScheduleStatus())
                    .scheduledAt(schedule.getScheduledAt())
                    .createdAt(schedule.getCreatedAt())
                    .executedAt(schedule.getExecutedAt())
                    .build();
        }

        private static List<UUID> parseTargetUserIds(String targetUserIds) {
            if (targetUserIds == null || targetUserIds.isBlank()) {
                return List.of();
            }
            return Arrays.stream(targetUserIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(UUID::fromString)
                    .toList();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 스케줄 목록 조회 응답")
    public static class NotificationScheduleListResponse {
        @Schema(description = "알림 스케줄 목록의 페이지 정보")
        private PageInfoResponse pageInfo;
        @Schema(description = "조회된 알림 스케줄 목록")
        private List<NotificationScheduleItemResponse> data;
    }
}
