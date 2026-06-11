package com.dodo.backend.userpet.exception;

import lombok.Getter;

/**
 * 가족 및 멤버십(UserPet) 도메인 비즈니스 로직 수행 중 발생하는 전용 예외 클래스입니다.
 * <p>
 * 가족 초대, 멤버십 관리, 초대 코드 검증 등 유저와 펫의 연결 관계 로직에서 예외 상황 발생 시 이 클래스를 throw 합니다.
 * {@code GlobalExceptionHandler}에서 이 예외를 가로채어 {@link UserPetErrorCode}에 정의된 표준 응답으로 변환합니다.
 */
@Getter
public class UserPetException extends RuntimeException {

    /**
     * 발생한 예외의 구체적인 종류(상태 코드, 메시지)를 담고 있는 Enum입니다.
     */
    private final UserPetErrorCode errorCode;

    private final String customMessage;

    public UserPetException(UserPetErrorCode errorCode) {
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public UserPetException(UserPetErrorCode errorCode, String customMessage) {
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }
}
