package com.dodo.backend.common.exception;

import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.board.exception.BoardException;
import com.dodo.backend.fence.exception.FenceException;
import com.dodo.backend.healthanalysis.exception.HealthAnalysisException;
import com.dodo.backend.imagefile.exception.ImageFileException;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.petweight.exception.PetWeightException;
import com.dodo.backend.reaction.exception.ReactionException;
import com.dodo.backend.user.exception.UserErrorCode;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.userpet.exception.UserPetException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static com.dodo.backend.auth.exception.AuthErrorCode.INTERNAL_SERVER_ERROR;
import static com.dodo.backend.common.exception.ErrorResponse.toResponseEntity;

/**
 * 전역(Global)에서 발생하는 예외를 중앙에서 감지하고 처리하는 핸들러 클래스입니다.
 * <p>
 * 비즈니스 로직 예외 및 입력값 유효성 검증 예외를 포착하여
 * 통일된 JSON 포맷으로 변환해 응답합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 인증 및 인가 로직에서 발생하는 {@link AuthException}을 처리합니다.
     */
    @ExceptionHandler(AuthException.class)
    protected ResponseEntity<ErrorResponse> handleAuthException(AuthException e) {
        log.error("AuthException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 유저 도메인 비즈니스 로직에서 발생하는 {@link UserException}을 처리합니다.
     */
    @ExceptionHandler(UserException.class)
    protected ResponseEntity<ErrorResponse> handleUserException(UserException e) {
        log.error("UserException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 게시글(Board) 도메인 비즈니스 로직에서 발생하는 {@link BoardException}을 처리합니다.
     */
    @ExceptionHandler(BoardException.class)
    protected ResponseEntity<ErrorResponse> handleBoardException(BoardException e) {
        log.error("BoardException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 펫(Pet) 도메인 비즈니스 로직에서 발생하는 {@link PetException}을 처리합니다.
     */
    @ExceptionHandler(PetException.class)
    protected ResponseEntity<ErrorResponse> handlePetException(PetException e) {
        log.error("PetException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 가족/초대(UserPet) 도메인 비즈니스 로직에서 발생하는 {@link UserPetException}을 처리합니다.
     */
    @ExceptionHandler(UserPetException.class)
    protected ResponseEntity<ErrorResponse> handleUserPetException(UserPetException e) {
        log.error("UserPetException occurred: {}", e.getErrorCode());
        if (e.getCustomMessage() != null) {
            return toResponseEntity(e.getErrorCode(), e.getCustomMessage());
        }
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 활동 기록(ActivityHistory) 도메인 비즈니스 로직에서 발생하는 {@link ActivityHistoryException}을 처리합니다.
     */
    @ExceptionHandler(ActivityHistoryException.class)
    protected ResponseEntity<ErrorResponse> handleActivityHistoryException(ActivityHistoryException e) {
        log.error("ActivityHistoryException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 펫 체중(PetWeight) 도메인 비즈니스 로직에서 발생하는 {@link PetWeightException}을 처리합니다.
     */
    @ExceptionHandler(PetWeightException.class)
    protected ResponseEntity<ErrorResponse> handlePetWeightException(PetWeightException e) {
        log.error("PetWeightException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 울타리(Fence) 도메인 비즈니스 로직에서 발생하는 {@link FenceException}을 처리합니다.
     */
    @ExceptionHandler(FenceException.class)
    protected ResponseEntity<ErrorResponse> handleFenceException(FenceException e) {
        log.error("FenceException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 건강 분석(HealthAnalysis) 도메인 비즈니스 로직에서 발생하는 {@link HealthAnalysisException}을 처리합니다.
     */
    @ExceptionHandler(HealthAnalysisException.class)
    protected ResponseEntity<ErrorResponse> handleHealthAnalysisException(HealthAnalysisException e) {
        log.error("HealthAnalysisException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 반응(Reaction) 도메인 비즈니스 로직에서 발생하는 {@link ReactionException}을 처리합니다.
     */
    @ExceptionHandler(ReactionException.class)
    protected ResponseEntity<ErrorResponse> handleReactionException(ReactionException e) {
        log.error("ReactionException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * 이미지 파일(ImageFile) 도메인 비즈니스 로직에서 발생하는 {@link ImageFileException}을 처리합니다.
     */
    @ExceptionHandler(ImageFileException.class)
    protected ResponseEntity<ErrorResponse> handleImageFileException(ImageFileException e) {
        log.error("ImageFileException occurred: {}", e.getErrorCode());
        return toResponseEntity(e.getErrorCode());
    }

    /**
     * {@code @Valid} 어노테이션을 통한 요청 데이터 유효성 검증 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();

        String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "잘못된 요청입니다.";

        log.warn("Validation failed: {}", errorMessage);

        return toResponseEntity(UserErrorCode.INVALID_PARAMETER, errorMessage);
    }

    /**
     * 요청 파라미터 타입 불일치, JSON 파싱 오류, 필수 파라미터 누락 등
     * 잘못된 요청(Bad Request) 관련 예외를 처리합니다.
     */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    protected ResponseEntity<ErrorResponse> handleBadRequestException(Exception e) {
        log.warn("Bad Request occurred: {}", e.getMessage());
        return toResponseEntity(UserErrorCode.INVALID_PARAMETER, "잘못된 요청입니다.");
    }

    /**
     * 처리되지 않은 예상치 못한 모든 예외(Exception)를 처리합니다.
     * <p>
     * 500 Internal Server Error로 응답합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Internal Server Error : {}", e.getMessage(), e);
        return toResponseEntity(INTERNAL_SERVER_ERROR);
    }
}
