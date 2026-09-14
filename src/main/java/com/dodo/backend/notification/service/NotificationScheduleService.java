package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationScheduleCreateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleCreateResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;

import java.util.UUID;

public interface NotificationScheduleService {

    NotificationScheduleCreateResponse createSchedule(UUID adminId, NotificationScheduleCreateRequest request);

    NotificationScheduleListResponse getSchedules(UUID adminId, int page, int size, NotificationScheduleStatus status);

    NotificationSimpleResponse cancelSchedule(UUID adminId, Long scheduleId);
}
