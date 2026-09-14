package com.dodo.backend.notification.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 알림 도메인에서 발생하는 예외 상황을 관리하는 에러 코드입니다.
 */
@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    /**
     * 요청 값이 올바르지 않은 경우 사용합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 알림을 찾을 수 없는 경우 사용합니다.
     */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 알림을 찾을 수 없습니다."),

    /**
     * 알림 스케줄을 찾을 수 없는 경우 사용합니다.
     */
    NOTIFICATION_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 알림 스케줄을 찾을 수 없습니다."),

    /**
     * 알림 수정 권한이 없는 경우 사용합니다.
     */
    NOTIFICATION_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "알림을 수정할 권한이 없습니다."),

    /**
     * 알림 삭제 권한이 없는 경우 사용합니다.
     */
    NOTIFICATION_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "알림을 삭제할 권한이 없습니다."),

    /**
     * 서버 내부 오류가 발생한 경우 사용합니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
