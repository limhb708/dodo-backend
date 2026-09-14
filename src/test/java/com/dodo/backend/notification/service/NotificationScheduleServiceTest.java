package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleRepeatType;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.entity.NotificationScheduleTargetType;
import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.notification.exception.NotificationErrorCode;
import com.dodo.backend.notification.exception.NotificationException;
import com.dodo.backend.notification.repository.NotificationScheduleRepository;
import com.dodo.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationScheduleServiceTest {

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationScheduleExecutor notificationScheduleExecutor;

    @Mock
    private FcmNotificationSender fcmNotificationSender;

    @InjectMocks
    private NotificationScheduleServiceImpl notificationScheduleService;

    @Test
    @DisplayName("알림 스케줄 목록 조회 성공")
    void getSchedules_Success() {
        UUID adminId = UUID.randomUUID();
        NotificationSchedule schedule = createSchedule(NotificationScheduleStatus.PENDING);
        given(notificationScheduleRepository.findAllByScheduleStatus(
                eq(NotificationScheduleStatus.PENDING),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(schedule)));

        NotificationScheduleListResponse response = notificationScheduleService.getSchedules(
                adminId,
                1,
                20,
                NotificationScheduleStatus.PENDING
        );

        assertEquals(1, response.getPageInfo().getPage());
        assertEquals(1, response.getData().size());
        assertEquals(1L, response.getData().get(0).getScheduleId());
        assertEquals(NotificationScheduleStatus.PENDING, response.getData().get(0).getScheduleStatus());
        verify(notificationScheduleRepository).findAllByScheduleStatus(eq(NotificationScheduleStatus.PENDING), any(Pageable.class));
    }

    @Test
    @DisplayName("알림 스케줄 취소 성공")
    void cancelSchedule_Success() {
        UUID adminId = UUID.randomUUID();
        NotificationSchedule schedule = createSchedule(NotificationScheduleStatus.PROCESSING);
        given(notificationScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));

        NotificationSimpleResponse response = notificationScheduleService.cancelSchedule(adminId, 1L);

        assertEquals("알림 스케줄이 성공적으로 취소되었습니다.", response.getMessage());
        assertEquals(NotificationScheduleStatus.CANCELED, schedule.getScheduleStatus());
        assertNull(schedule.getProcessingToken());
        assertNull(schedule.getProcessingStartedAt());
    }

    @Test
    @DisplayName("완료된 알림 스케줄 취소 실패")
    void cancelSchedule_Fail_CompletedSchedule() {
        UUID adminId = UUID.randomUUID();
        NotificationSchedule schedule = createSchedule(NotificationScheduleStatus.COMPLETED);
        given(notificationScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));

        NotificationException exception = assertThrows(
                NotificationException.class,
                () -> notificationScheduleService.cancelSchedule(adminId, 1L)
        );

        assertEquals(NotificationErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    private NotificationSchedule createSchedule(NotificationScheduleStatus status) {
        return NotificationSchedule.builder()
                .notificationScheduleId(1L)
                .notificationTitle("공지 알림")
                .notificationBody("새로운 공지가 등록되었습니다.")
                .notificationType(NotificationType.SYSTEM)
                .targetType(NotificationScheduleTargetType.ALL)
                .repeatType(NotificationScheduleRepeatType.NONE)
                .scheduleStatus(status)
                .scheduledAt(LocalDateTime.of(2099, 1, 1, 14, 30))
                .processingToken("claim-token")
                .processingStartedAt(LocalDateTime.of(2099, 1, 1, 14, 29))
                .build();
    }
}
