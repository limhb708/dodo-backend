package com.dodo.backend.userpet.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 가족 초대 및 펫 멤버십(UserPet) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * 가족 구성원 초대, 초대 코드 검증, 멤버십 관리 과정에서 발생하는 예외를 포함하며,
 * HTTP 상태 코드와 클라이언트에게 전달할 메시지를 {@code Key-Value} 형태로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum UserPetErrorCode implements BaseErrorCode {

    /**
     * 클라이언트의 요청 형식이 잘못되었거나 유효성 검사를 통과하지 못했을 때 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 인증되지 않은 사용자가 로그인이 필요한 기능을 요청했을 때 사용합니다.
     * <p>
     * HTTP {@code 401 Unauthorized}를 반환합니다.
     */
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),

    /**
     * 반려동물의 최초 등록자(소유자)가 아닌 사용자가 가족 초대를 시도할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    FAMILY_INVITE_OWNER_ONLY(HttpStatus.FORBIDDEN, "자신이 등록한 반려동물에만 가족을 초대할 수 있습니다."),

    /**
     * 해당 반려동물의 가족 멤버가 아닌 사용자가 초대를 시도할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    INVITE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "자신이 등록하거나 속해있는 반려동물만 초대할 수 있습니다."),

    /**
     * 요청한 식별자(ID)에 해당하는 반려동물을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 반려동물을 찾을 수 없습니다."),

    /**
     * 초대를 발송하려는 대상 사용자(회원)를 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    INVITEE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대하려는 사용자를 찾을 수 없습니다."),

    /**
     * 입력한 초대 코드가 만료되었거나 존재하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "만료되었거나 존재하지 않는 초대 코드입니다."),

    /**
     * 요청과 관련된 사용자 정보를 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    /**
     * 이미 해당 반려동물의 가족 구성원으로 등록된 사용자를 다시 초대하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    ALREADY_FAMILY_MEMBER(HttpStatus.CONFLICT, "이미 가족으로 등록되어있습니다."),

    /**
     * 이미 가족 등록 신청이 승인 대기 중인 사용자가 다시 같은 반려동물 가족 신청을 시도할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    FAMILY_REQUEST_PENDING(HttpStatus.CONFLICT, "이미 가족 등록 신청이 대기 중입니다."),

    /**
     * 이전 가족 등록 신청이 거절된 사용자가 다시 같은 반려동물 가족 신청을 시도할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    FAMILY_REQUEST_REJECTED(HttpStatus.CONFLICT, "가족 등록 신청이 거절되었습니다."),

    /**
     * 가족 등록 신청이 차단된 사용자가 다시 같은 반려동물 가족 신청을 시도할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}를 반환합니다.
     */
    FAMILY_REQUEST_BLOCKED(HttpStatus.FORBIDDEN, "가족 등록 신청이 차단되었습니다."),

    /**
     * 해당 반려동물에 대해 유효한 초대 코드가 이미 존재할 때 사용합니다.
     * <p>
     * 중복 발급을 방지하기 위해 사용되며, 기존 코드가 만료될 때까지 재발급이 제한됩니다.
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    INVITATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 유효한 초대 코드가 존재합니다. 만료 후 다시 시도해주세요."),

    /**
     * 서버 내부에서 예상치 못한 오류가 발생했을 때 사용합니다.
     * <p>
     * HTTP {@code 500 Internal Server Error}를 반환합니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    /**
     * 에러 상황에 해당하는 HTTP 상태 코드입니다.
     */
    private final HttpStatus httpStatus;

    /**
     * 클라이언트에게 전달할 상세 에러 메시지입니다.
     */
    private final String message;
}
