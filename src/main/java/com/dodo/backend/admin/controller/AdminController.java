package com.dodo.backend.admin.controller;

import com.dodo.backend.admin.dto.request.AdminRequest.AnnouncementCreateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.AnnouncementUpdateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.ReportStatusUpdateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.UserStatusUpdateRequest;
import com.dodo.backend.admin.dto.response.AdminResponse.AdminSimpleResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementListResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.BoardReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.CommentReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportListResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.UserReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.UserListResponse;
import com.dodo.backend.admin.entity.AdminReportType;
import com.dodo.backend.admin.service.AdminService;
import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationScheduleCreateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleCreateResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.service.NotificationScheduleService;
import com.dodo.backend.report.entity.ReportStatus;
import com.dodo.backend.user.entity.UserStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 관리자 API 요청을 처리하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@Tag(name = "Admin API", description = "관리자 기능 API")
public class AdminController {

    private final AdminService adminService;
    private final NotificationScheduleService notificationScheduleService;

    /**
     * 특정 게시글의 신고 상세 내역을 조회합니다.
     *
     * @param boardId 신고 상세 내역을 조회할 게시글 ID
     * @return 게시글 신고 상세 내역
    */
    @Operation(summary = "게시글 신고 상세 조회", description = "특정 게시글에 접수된 신고 상세 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 신고 상세 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = BoardReportDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "신고당한 게시글이 존재하지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "신고당한 게시글이 존재하지 않습니다.", value = "{\"status\": 404, \"message\": \"신고당한 게시글이 존재하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/reports/board/{boardId}")
    public ResponseEntity<BoardReportDetailResponse> getBoardReportDetail(@PathVariable Long boardId) {
        log.info("관리자 게시글 신고 상세 조회 요청 - BoardId: {}", boardId);
        return ResponseEntity.ok(adminService.getBoardReportDetail(boardId));
    }

    /**
     * 특정 유저의 신고 상세 내역을 조회합니다.
     *
     * @param userId 신고 상세 내역을 조회할 유저 ID
     * @return 유저 신고 상세 내역
    */
    @Operation(summary = "유저 신고 상세 조회", description = "특정 유저에 접수된 신고 상세 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 신고 상세 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = UserReportDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "신고당한 유저가 존재하지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "신고당한 유저가 존재하지 않습니다.", value = "{\"status\": 404, \"message\": \"신고당한 유저가 존재하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/reports/user/{userId}")
    public ResponseEntity<UserReportDetailResponse> getUserReportDetail(@PathVariable UUID userId) {
        log.info("관리자 유저 신고 상세 조회 요청 - UserId: {}", userId);
        return ResponseEntity.ok(adminService.getUserReportDetail(userId));
    }

    /**
     * 특정 댓글의 신고 상세 내역을 조회합니다.
     *
     * @param commentId 신고 상세 내역을 조회할 댓글 ID
     * @return 댓글 신고 상세 내역
    */
    @Operation(summary = "댓글 신고 상세 조회", description = "특정 댓글에 접수된 신고 상세 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 신고 상세 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = CommentReportDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "신고당한 댓글이 존재하지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "신고당한 댓글이 존재하지 않습니다.", value = "{\"status\": 404, \"message\": \"신고당한 댓글이 존재하지 않습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/reports/comment/{commentId}")
    public ResponseEntity<CommentReportDetailResponse> getCommentReportDetail(@PathVariable Long commentId) {
        log.info("관리자 댓글 신고 상세 조회 요청 - CommentId: {}", commentId);
        return ResponseEntity.ok(adminService.getCommentReportDetail(commentId));
    }

    /**
     * 신고 목록을 조회합니다.
     *
     * @param reportType 조회할 신고 대상 유형
     * @param reportStatus 조회할 신고 처리 상태
     * @param page 조회할 페이지 번호
     * @param size 페이지당 신고 목록 개수
     * @param sort 정렬 조건
     * @return 신고 목록 조회 결과
    */
    @Operation(summary = "신고 목록 조회", description = "신고 유형과 처리 상태에 따라 신고 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신고 목록 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = ReportListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/reports")
    public ResponseEntity<ReportListResponse> getReportList(
            @RequestParam AdminReportType reportType,
            @RequestParam(required = false) ReportStatus reportStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        log.info("관리자 신고 목록 조회 요청 - Type: {}, Status: {}, Page: {}, Size: {}, Sort: {}",
                reportType, reportStatus, page, size, sort);
        return ResponseEntity.ok(adminService.getReportList(reportType, reportStatus, page, size, sort));
    }

    /**
     * 일반 유저 목록을 조회하고 이메일, 이름, 닉네임으로 검색합니다.
     *
     * @param keyword 이메일, 이름, 닉네임 검색어
     * @param status 계정 상태 필터
     * @param page 조회할 페이지 번호
     * @param size 페이지당 유저 수
     * @param sort 정렬 조건
     * @return 유저 목록 조회 결과
    */
    @Operation(summary = "유저 목록 조회", description = "일반 유저 목록을 조회하고 이메일, 이름, 닉네임 및 계정 상태로 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 목록 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = UserListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/users")
    public ResponseEntity<UserListResponse> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "userCreatedAt,desc") String sort
    ) {
        log.info("관리자 유저 목록 조회 요청 - Keyword: {}, Status: {}, Page: {}, Size: {}, Sort: {}",
                keyword, status, page, size, sort);
        return ResponseEntity.ok(adminService.getUserList(keyword, status, page, size, sort));
    }

    /**
     * 유저 계정 상태를 변경합니다.
     *
     * @param userId 상태를 변경할 유저 ID
     * @param request 변경할 유저 상태 요청
     * @return 상태 변경 성공 메시지
    */
    @Operation(summary = "유저 계정 상태 변경", description = "관리자가 유저 계정 상태를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 계정 상태를 성공적으로 변경했습니다.",
                    content = @Content(schema = @Schema(implementation = AdminSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 유저입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 유저입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminSimpleResponse> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        log.info("관리자 유저 상태 변경 요청 - UserId: {}, Status: {}", userId, request.getStatus());
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request));
    }

    /**
     * 게시글을 강제로 삭제합니다.
     *
     * @param boardId 삭제할 게시글 ID
     * @return 게시글 삭제 성공 메시지
     */
    @Operation(summary = "게시글 강제 삭제", description = "관리자가 게시글을 삭제 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글이 성공적으로 강제 삭제되었습니다.",
                    content = @Content(schema = @Schema(implementation = AdminSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 게시글입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 게시글입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 게시글입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<AdminSimpleResponse> deleteBoard(@PathVariable Long boardId) {
        log.info("관리자 게시글 강제 삭제 요청 - BoardId: {}", boardId);
        adminService.deleteBoard(boardId);
        return ResponseEntity.ok(AdminSimpleResponse.toDto("게시글이 성공적으로 강제 삭제되었습니다."));
    }

    /**
     * 댓글을 강제로 삭제합니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @return 댓글 삭제 성공 메시지
     */
    @Operation(summary = "댓글 강제 삭제", description = "관리자가 댓글을 강제로 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글이 성공적으로 강제 삭제되었습니다.",
                    content = @Content(schema = @Schema(implementation = AdminSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 댓글입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 댓글입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 댓글입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<AdminSimpleResponse> deleteComment(@PathVariable Long commentId) {
        log.info("관리자 댓글 강제 삭제 요청 - CommentId: {}", commentId);
        adminService.deleteComment(commentId);
        return ResponseEntity.ok(AdminSimpleResponse.toDto("댓글이 성공적으로 강제 삭제되었습니다."));
    }

    /**
     * 신고 처리 상태를 변경합니다.
     *
     * @param reportId 상태를 변경할 신고 ID
     * @param request 변경할 신고 처리 상태 요청
     * @return 상태 변경 성공 메시지
    */
    @Operation(summary = "신고 처리 상태 변경", description = "관리자가 신고 처리 상태를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신고 처리 상태를 성공적으로 변경했습니다.",
                    content = @Content(schema = @Schema(implementation = AdminSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 신고 내역입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 신고 내역입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 신고 내역입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/reports/{reportId}/status")
    public ResponseEntity<AdminSimpleResponse> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusUpdateRequest request
    ) {
        log.info("관리자 신고 상태 변경 요청 - ReportId: {}, Status: {}", reportId, request.getStatus());
        return ResponseEntity.ok(adminService.updateReportStatus(reportId, request));
    }

    /**
     * 공지를 작성합니다.
     *
     * @param request 공지 작성 요청
     * @param userDetails 인증된 관리자 정보
     * @return 공지 작성 성공 메시지
    */
    @Operation(summary = "공지 작성", description = "관리자가 공지를 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지가 성공적으로 작성되었습니다.",
                    content = @Content(schema = @Schema(implementation = AdminSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 유저입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 유저입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/announcements")
    public ResponseEntity<AdminSimpleResponse> createAnnouncement(
            @Valid @RequestBody AnnouncementCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        log.info("관리자 공지 작성 요청 - AdminId: {}", adminId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(adminService.createAnnouncement(adminId, request));
    }

    @Operation(summary = "알림 스케줄 등록", description = "관리자가 지정 시간에 발송될 알림 스케줄을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 스케줄 등록 성공",
                    content = @Content(schema = @Schema(implementation = NotificationScheduleCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/notification-schedules")
    public ResponseEntity<NotificationScheduleCreateResponse> createNotificationSchedule(
            @Valid @RequestBody NotificationScheduleCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        log.info("관리자 알림 스케줄 등록 요청 - AdminId: {}, ScheduledAt: {}", adminId, request.getScheduledAt());
        return ResponseEntity.ok(notificationScheduleService.createSchedule(adminId, request));
    }

    @Operation(summary = "알림 스케줄 목록 조회", description = "관리자가 등록된 알림 스케줄 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 스케줄 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = NotificationScheduleListResponse.class)))
    })
    @GetMapping("/notification-schedules")
    public ResponseEntity<NotificationScheduleListResponse> getNotificationSchedules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotificationScheduleStatus status,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        log.info("관리자 알림 스케줄 목록 조회 요청 - AdminId: {}, Page: {}, Size: {}, Status: {}",
                adminId, page, size, status);
        return ResponseEntity.ok(notificationScheduleService.getSchedules(adminId, page, size, status));
    }

    @Operation(summary = "알림 스케줄 취소", description = "관리자가 등록된 알림 스케줄을 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 스케줄 취소 성공",
                    content = @Content(schema = @Schema(implementation = NotificationSimpleResponse.class)))
    })
    @DeleteMapping("/notification-schedules/{scheduleId}")
    public ResponseEntity<NotificationSimpleResponse> cancelNotificationSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        log.info("관리자 알림 스케줄 취소 요청 - AdminId: {}, ScheduleId: {}", adminId, scheduleId);
        return ResponseEntity.ok(notificationScheduleService.cancelSchedule(adminId, scheduleId));
    }

    /**
     * 공지를 삭제합니다.
     *
     * @param boardId 삭제할 공지 게시글 ID
     * @return 공지 삭제 성공 메시지
     */
    @Operation(summary = "공지 삭제", description = "관리자가 공지를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지가 성공적으로 삭제되었습니다.",
                    content = @Content(schema = @Schema(implementation = AdminSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공지입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 공지입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 공지입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/announcements/{boardId}")
    public ResponseEntity<AdminSimpleResponse> deleteAnnouncement(@PathVariable Long boardId) {
        log.info("관리자 공지 삭제 요청 - BoardId: {}", boardId);
        adminService.deleteAnnouncement(boardId);
        return ResponseEntity.ok(AdminSimpleResponse.toDto("공지가 성공적으로 삭제되었습니다."));
    }

    /**
     * 공지를 수정합니다.
     *
     * @param boardId 수정할 공지 게시글 ID
     * @param request 공지 수정 요청
     * @return 공지 수정 성공 메시지
     */
    @Operation(summary = "공지 수정", description = "관리자가 공지를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지가 성공적으로 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = AdminSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공지입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 공지입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 공지입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/announcements/{boardId}")
    public ResponseEntity<AdminSimpleResponse> updateAnnouncement(
            @PathVariable Long boardId,
            @RequestBody AnnouncementUpdateRequest request
    ) {
        log.info("관리자 공지 수정 요청 - BoardId: {}", boardId);
        adminService.updateAnnouncement(boardId, request);
        return ResponseEntity.ok(AdminSimpleResponse.toDto("공지가 성공적으로 수정되었습니다."));
    }

    /**
     * 공지 목록을 조회합니다.
     *
     * @param page 조회할 페이지 번호
     * @param size 페이지당 공지 개수
     * @param sort 정렬 조건
     * @return 공지 목록 조회 결과
    */
    @Operation(summary = "공지 목록 조회", description = "공지 목록을 페이지 단위로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지 목록 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = AnnouncementListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/announcements")
    public ResponseEntity<AnnouncementListResponse> getAnnouncementList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "registrationUpdatedAt,desc") String sort
    ) {
        log.info("관리자 공지 목록 조회 요청 - Page: {}, Size: {}, Sort: {}", page, size, sort);
        return ResponseEntity.ok(adminService.getAnnouncementList(page, size, sort));
    }

    /**
     * 공지 상세를 조회합니다.
     *
     * @param boardId 조회할 공지 게시글 ID
     * @return 공지 상세 정보
    */
    @Operation(summary = "공지 상세 조회", description = "공지 상세 내용을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지 상세 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = AnnouncementDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청입니다.", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "로그인이 필요한 기능입니다.", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "접근 권한이 없습니다.", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공지입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "존재하지 않는 공지입니다.", value = "{\"status\": 404, \"message\": \"존재하지 않는 공지입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "서버 내부 오류가 발생했습니다.", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/announcements/{boardId}")
    public ResponseEntity<AnnouncementDetailResponse> getAnnouncementDetail(@PathVariable Long boardId) {
        log.info("관리자 공지 상세 조회 요청 - BoardId: {}", boardId);
        return ResponseEntity.ok(adminService.getAnnouncementDetail(boardId));
    }
}
