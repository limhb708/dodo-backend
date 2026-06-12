package com.dodo.backend.board.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 게시판(Board) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * 게시글 작성, 상세 조회, 수정, 삭제 및 임시 저장 과정에서 발생할 수 있는 예외를 포함하며,
 * HTTP 상태 코드와 클라이언트에게 전달할 메시지를 Key-Value 형태로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum BoardErrorCode implements BaseErrorCode {

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
     * 게시글 생성 권한이 없는 사용자가 작성 요청을 시도했을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    CREATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "게시글을 생성할 권한이 없습니다."),

    /**
     * 특정 게시글에 대한 조회 권한이 없는 사용자가 상세 조회를 요청했을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    VIEW_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "특정 게시글을 조회할 권한이 없습니다."),

    /**
     * 게시글 수정 또는 임시 저장 권한이 없는 사용자가 요청했을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    UPDATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "게시글을 수정할 권한이 없습니다."),

    /**
     * 게시글 삭제 권한이 없는 사용자가 삭제 요청을 시도했을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    DELETE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "게시글을 삭제할 권한이 없습니다."),

    /**
     * 요청한 식별자(ID)에 해당하는 게시글을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 게시글을 찾을 수 없습니다."),

    /**
     * Redis에 해당 세션 키로 저장된 임시 게시글 데이터가 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    TEMP_SAVE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 세션키를 찾을 수 없습니다."),

    /**
     * 게시글 임시 저장 요청 사용자가 게시글 작성자가 아니거나 삭제된 게시글을 임시 저장하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    TEMP_SAVE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "게시글을 수정할 권한이 없습니다."),

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
