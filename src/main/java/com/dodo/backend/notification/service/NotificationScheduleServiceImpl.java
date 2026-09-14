package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationScheduleCreateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleCreateResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleItemResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.PageInfoResponse;
import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleRepeatType;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.entity.NotificationScheduleTargetType;
import com.dodo.backend.notification.exception.NotificationException;
import com.dodo.backend.notification.repository.NotificationScheduleRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.dodo.backend.notification.exception.NotificationErrorCode.INVALID_REQUEST;
import static com.dodo.backend.notification.exception.NotificationErrorCode.NOTIFICATION_SCHEDULE_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduleServiceImpl implements NotificationScheduleService {

    private static final int MAX_SCHEDULE_PAGE_SIZE = 100;
    private static final String SCHEDULE_CANCEL_SUCCESS_MESSAGE = "알림 스케줄이 성공적으로 취소되었습니다.";

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final UserRepository userRepository;
    private final NotificationScheduleExecutor notificationScheduleExecutor;
    private final FcmNotificationSender fcmNotificationSender;

    @Value("${notification.scheduler.processing-timeout-minutes:10}")
    private long processingTimeoutMinutes;

    @Transactional
    @Override
    public NotificationScheduleCreateResponse createSchedule(UUID adminId, NotificationScheduleCreateRequest request) {
        validateCreateRequest(adminId, request);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotificationException(INVALID_REQUEST));

        NotificationSchedule schedule = NotificationSchedule.builder()
                .createdBy(admin)
                .notificationTitle(request.getTitle())
                .notificationBody(request.getBody())
                .notificationType(request.getNotificationType())
                .targetType(request.getTargetType())
                .targetUserIds(toTargetUserIds(request))
                .repeatType(resolveRepeatType(request.getRepeatType()))
                .scheduleStatus(NotificationScheduleStatus.PENDING)
                .scheduledAt(request.getScheduledAt())
                .build();

        NotificationSchedule savedSchedule = notificationScheduleRepository.save(schedule);
        return NotificationScheduleCreateResponse.toDto(savedSchedule);
    }

    @Transactional(readOnly = true)
    @Override
    public NotificationScheduleListResponse getSchedules(UUID adminId, int page, int size, NotificationScheduleStatus status) {
        validateSchedulePageRequest(adminId, page, size);

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "scheduledAt")
                        .and(Sort.by(Sort.Direction.DESC, "notificationScheduleId"))
        );
        Page<NotificationSchedule> schedules = status == null
                ? notificationScheduleRepository.findAll(pageable)
                : notificationScheduleRepository.findAllByScheduleStatus(status, pageable);

        return NotificationScheduleListResponse.builder()
                .pageInfo(PageInfoResponse.toDto(schedules, page))
                .data(schedules.stream()
                        .map(NotificationScheduleItemResponse::toDto)
                        .toList())
                .build();
    }

    @Transactional
    @Override
    public NotificationSimpleResponse cancelSchedule(UUID adminId, Long scheduleId) {
        validateScheduleIdRequest(adminId, scheduleId);
        NotificationSchedule schedule = notificationScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotificationException(NOTIFICATION_SCHEDULE_NOT_FOUND));
        if (schedule.getScheduleStatus() == NotificationScheduleStatus.COMPLETED
                || schedule.getScheduleStatus() == NotificationScheduleStatus.CANCELED) {
            throw new NotificationException(INVALID_REQUEST);
        }

        schedule.cancel();
        return NotificationSimpleResponse.toDto(SCHEDULE_CANCEL_SUCCESS_MESSAGE);
    }

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay:60000}")
    public void executeDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        int releasedCount = notificationScheduleExecutor.releaseExpiredClaims(now.minusMinutes(processingTimeoutMinutes));
        if (releasedCount > 0) {
            log.warn("만료된 알림 스케줄 처리 선점 상태를 해제했습니다. count: {}", releasedCount);
        }

        List<ClaimedNotificationSchedule> claimedSchedules = notificationScheduleExecutor.claimDueSchedules(now);
        claimedSchedules.forEach(this::executeClaimedSchedule);
    }

    private void executeClaimedSchedule(ClaimedNotificationSchedule claimedSchedule) {
        Optional<NotificationScheduleDispatch> dispatch;
        try {
            dispatch = notificationScheduleExecutor.prepareDispatch(claimedSchedule, LocalDateTime.now());
        } catch (Exception e) {
            log.error("알림 스케줄 DB 처리 실패 - scheduleId: {}", claimedSchedule.scheduleId(), e);
            notificationScheduleExecutor.releaseClaim(claimedSchedule);
            return;
        }

        try {
            dispatch.ifPresent(this::sendPushOutsideTransaction);
        } catch (Exception e) {
            log.warn("알림 스케줄 FCM 발송 실패 - scheduleId: {}, reason: {}",
                    claimedSchedule.scheduleId(), e.getMessage());
        }
    }

    private void sendPushOutsideTransaction(NotificationScheduleDispatch dispatch) {
        fcmNotificationSender.sendToUsers(
                dispatch.targets(),
                dispatch.title(),
                dispatch.body(),
                dispatch.type(),
                dispatch.relatedId()
        );
    }

    private NotificationScheduleRepeatType resolveRepeatType(NotificationScheduleRepeatType repeatType) {
        return repeatType == null ? NotificationScheduleRepeatType.NONE : repeatType;
    }

    private void validateCreateRequest(UUID adminId, NotificationScheduleCreateRequest request) {
        if (adminId == null || request == null || request.getTargetType() == null) {
            throw new NotificationException(INVALID_REQUEST);
        }
        if (request.getTargetType() == NotificationScheduleTargetType.USERS
                && (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty())) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private String toTargetUserIds(NotificationScheduleCreateRequest request) {
        if (request.getTargetType() != NotificationScheduleTargetType.USERS) {
            return null;
        }
        return request.getTargetUserIds().stream()
                .distinct()
                .map(UUID::toString)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow(() -> new NotificationException(INVALID_REQUEST));
    }

    private void validateSchedulePageRequest(UUID adminId, int page, int size) {
        if (adminId == null || page <= 0 || size <= 0 || size > MAX_SCHEDULE_PAGE_SIZE) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private void validateScheduleIdRequest(UUID adminId, Long scheduleId) {
        if (adminId == null || scheduleId == null || scheduleId <= 0) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

}
