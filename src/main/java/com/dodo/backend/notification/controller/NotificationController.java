package com.dodo.backend.notification.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationReadUpdateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.UnreadNotificationCountResponse;
import com.dodo.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 알림 API 요청을 처리하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
@Tag(name = "Notification API", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록을 조회합니다.
     *
     * @param page 조회할 페이지 번호
     * @param size 페이지당 알림 수
     * @param isRead 읽음 여부 필터
     * @param type 알림 유형 필터
     * @param userDetails 인증 사용자 정보
     * @return 알림 목록 조회 결과
    */
    @Operation(summary = "알림 목록 조회", description = "로그인 사용자의 알림 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 목록 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = NotificationListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("알림 목록 조회 요청 - UserId: {}, Page: {}, Size: {}, IsRead: {}, Type: {}", userId, page, size, isRead, type);
        return ResponseEntity.ok(notificationService.getNotifications(userId, page, size, isRead, type));
    }

    /**
     * 특정 알림의 읽음 여부를 변경합니다.
     *
     * @param notificationId 읽음 여부를 변경할 알림 ID
     * @param request 읽음 여부 변경 요청
     * @param userDetails 인증 사용자 정보
     * @return 읽음 처리 성공 메시지
    */
    @Operation(summary = "알림 읽음 처리", description = "특정 알림의 읽음 여부를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 읽음 상태를 성공적으로 변경했습니다.",
                    content = @Content(schema = @Schema(implementation = NotificationSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "알림을 수정할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "알림을 수정할 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"알림을 수정할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 알림을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "해당 ID의 알림을 찾을 수 없습니다.", value = "{\"status\": 404, \"message\": \"해당 ID의 알림을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{notificationId}")
    public ResponseEntity<NotificationSimpleResponse> updateReadStatus(
            @PathVariable Long notificationId,
            @Valid @RequestBody NotificationReadUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("알림 읽음 처리 요청 - UserId: {}, NotificationId: {}", userId, notificationId);
        return ResponseEntity.ok(notificationService.updateReadStatus(userId, notificationId, request));
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 삭제할 알림 ID
     * @param userDetails 인증 사용자 정보
     * @return 응답 본문이 없는 204 응답
     */
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "알림이 성공적으로 삭제되었습니다.",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "알림을 삭제할 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "알림을 삭제할 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"알림을 삭제할 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 알림을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "해당 ID의 알림을 찾을 수 없습니다.", value = "{\"status\": 404, \"message\": \"해당 ID의 알림을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("알림 삭제 요청 - UserId: {}, NotificationId: {}", userId, notificationId);
        notificationService.deleteNotification(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 읽지 않은 알림 개수를 조회합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @return 읽지 않은 알림 개수
    */
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "로그인 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "읽지 않은 알림 개수 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = UnreadNotificationCountResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/count/unread")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("읽지 않은 알림 개수 조회 요청 - UserId: {}", userId);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @return 전체 읽음 처리 성공 메시지
    */
    @Operation(summary = "모든 알림 읽음 처리", description = "로그인 사용자의 모든 알림을 읽음 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "모든 알림을 성공적으로 읽음 처리했습니다.",
                    content = @Content(schema = @Schema(implementation = NotificationSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/read-all")
    public ResponseEntity<NotificationSimpleResponse> readAll(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("모든 알림 읽음 처리 요청 - UserId: {}", userId);
        return ResponseEntity.ok(notificationService.readAll(userId));
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @return 응답 본문이 없는 204 응답
     */
    @Operation(summary = "모든 알림 삭제", description = "로그인 사용자의 모든 알림을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "모든 알림이 성공적으로 삭제되었습니다.",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAll(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("모든 알림 삭제 요청 - UserId: {}", userId);
        notificationService.deleteAll(userId);
        return ResponseEntity.noContent().build();
    }
}
