package com.dodo.backend.board.controller;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardTempSaveRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.dto.response.BoardResponse;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 게시글 생성, 조회, 수정, 삭제 및 임시 저장 API를 제공하는 컨트롤러입니다.
 * <p>
 * 인증된 사용자의 UUID는 {@link UserDetails#getUsername()}에서 추출하여 서비스 계층으로 전달합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
@Tag(name = "Board API", description = "게시글 관련 API")
@Slf4j
public class BoardController {

    private final BoardService boardService;

    /**
     * 새 게시글을 작성합니다.
     *
     * @param request     게시글 생성 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 생성된 게시글 ID와 성공 메시지
     */
    @Operation(summary = "게시글 작성", description = "새 게시글을 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글이 성공적으로 작성되었습니다.",
                    content = @Content(schema = @Schema(implementation = BoardResponse.BoardCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "게시글을 생성할 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<BoardResponse.BoardCreateResponse> createBoard(
            @RequestBody BoardCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("게시글 작성 요청 수신 - User: {}, Title: {}", userId, request.getBoardTitle());

        Long boardId = boardService.createBoard(userId, request);
        BoardResponse.BoardCreateResponse response =
                BoardResponse.BoardCreateResponse.toDto(boardId, "게시글이 성공적으로 작성되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 수정 중인 게시글 내용을 Redis에 임시 저장합니다.
     *
     * @param boardId     임시 저장 대상 게시글 ID
     * @param request     임시 저장 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 임시 저장 세션 키와 성공 메시지
     */
    @Operation(summary = "게시글 임시 저장", description = "수정 중인 게시글 내용을 Redis에 임시 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글이 성공적으로 임시 저장되었습니다.",
                    content = @Content(schema = @Schema(implementation = BoardResponse.BoardTempSaveResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "게시글을 수정할 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 게시글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/temp-save")
    public ResponseEntity<BoardResponse.BoardTempSaveResponse> tempSaveBoard(
            @RequestParam Long boardId,
            @RequestBody BoardTempSaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("게시글 임시 저장 요청 수신 - User: {}, BoardId: {}", userId, boardId);

        return ResponseEntity.ok(boardService.tempSaveBoard(userId, boardId, request));
    }

    /**
     * Redis에 임시 저장된 게시글 내용을 조회합니다.
     *
     * @param sessionKey  임시 저장 데이터의 세션 키
     * @param userDetails 인증된 사용자 정보
     * @return 임시 저장된 게시글 제목, 본문, 이미지 URL
     */
    @Operation(summary = "임시 저장 게시글 조회", description = "sessionKey 기준으로 Redis에 저장된 임시 게시글 내용을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "임시 저장된 게시글을 성공적으로 불러왔습니다.",
                    content = @Content(schema = @Schema(implementation = BoardResponse.BoardTempSaveDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "임시 저장된 게시글을 조회할 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 세션키를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/temp-save/{sessionKey}")
    public ResponseEntity<BoardResponse.BoardTempSaveDetailResponse> getTempSavedBoard(
            @PathVariable String sessionKey,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("임시 저장 게시글 조회 요청 수신 - User: {}, SessionKey: {}", userId, sessionKey);

        return ResponseEntity.ok(boardService.getTempSavedBoard(userId, sessionKey));
    }

    /**
     * 특정 게시글의 상세 정보를 조회합니다.
     *
     * @param boardId     조회할 게시글 ID
     * @param userDetails 인증된 사용자 정보
     * @return 게시글 상세 정보
     */
    @Operation(summary = "게시글 상세 조회", description = "boardId 기준으로 특정 게시글의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 상세 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = BoardResponse.BoardDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "특정 게시글을 조회할 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 게시글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse.BoardDetailResponse> getBoardDetail(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("게시글 상세 조회 요청 수신 - User: {}, BoardId: {}", userId, boardId);

        return ResponseEntity.ok(boardService.getBoardDetail(userId, boardId));
    }

    /**
     * 특정 게시글을 수정합니다.
     *
     * @param boardId     수정할 게시글 ID
     * @param request     게시글 수정 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 수정 성공 메시지
     */
    @Operation(summary = "게시글 수정", description = "boardId 기준으로 특정 게시글의 제목, 내용, 이미지를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글이 성공적으로 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = BoardResponse.BoardSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "게시글을 수정할 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 게시글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{boardId}")
    public ResponseEntity<BoardResponse.BoardSimpleResponse> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("게시글 수정 요청 수신 - User: {}, BoardId: {}", userId, boardId);

        return ResponseEntity.ok(boardService.updateBoard(userId, boardId, request));
    }

    /**
     * 특정 게시글을 삭제 상태로 변경합니다.
     *
     * @param boardId     삭제할 게시글 ID
     * @param userDetails 인증된 사용자 정보
     * @return 삭제 성공 메시지
     */
    @Operation(summary = "게시글 삭제", description = "boardId 기준으로 특정 게시글을 삭제 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글이 성공적으로 삭제되었습니다.",
                    content = @Content(schema = @Schema(implementation = BoardResponse.BoardSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "게시글을 삭제할 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 게시글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{boardId}")
    public ResponseEntity<BoardResponse.BoardSimpleResponse> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("게시글 삭제 요청 수신 - User: {}, BoardId: {}", userId, boardId);

        return ResponseEntity.ok(boardService.deleteBoard(userId, boardId));
    }
}
