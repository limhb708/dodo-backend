package com.dodo.backend.pet.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.request.PetRequest.*;
import com.dodo.backend.pet.dto.response.PetResponse.*;
import com.dodo.backend.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 반려동물 도메인의 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * <p>
 * 클라이언트로부터 펫 등록, 조회, 가족 초대 및 승인 등의 요청을 받아 서비스 계층으로 전달하고,
 * 처리 결과를 응답으로 반환합니다.
 */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets API", description = "반려동물 관련 API")
@Slf4j
public class PetController {

    private final PetService petService;

    /**
     * 새로운 반려동물을 등록하고 사용자와 연결합니다.
     * <p>
     * 인증된 사용자 정보를 기반으로 펫 등록을 수행하며,
     * 성공 시 201 Created 상태 코드와 함께 생성된 펫의 ID를 반환합니다.
     *
     * @param request     펫 등록에 필요한 상세 정보 (이름, 품종, 등록번호 등)
     * @param userDetails Spring Security를 통해 인증된 사용자의 세부 정보
     * @return 등록된 펫의 ID를 포함한 응답 객체 (HTTP 201)
     */
    @Operation(summary = "펫 생성", description = "새로운 반려동물을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "새 반려동물을 등록 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 등록번호입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 존재하는 등록번호입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<PetRegisterResponse> createPet(
            @Valid @RequestBody PetRegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("펫 등록 요청 수신 - User: {}, PetName: {}", userId, request.getPetName());

        PetRegisterResponse response = petService.registerPet(userId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 기존 반려동물의 정보를 수정합니다.
     * <p>
     * 변경이 필요한 필드만 선택적으로 수정할 수 있으며,
     * 요청한 사용자가 해당 반려동물의 소유자인지 확인 후 처리를 완료합니다.
     *
     * @param petId       수정할 반려동물의 고유 ID
     * @param request     수정할 반려동물 정보 (성별, 나이, 이름 등)
     * @param userDetails Spring Security를 통해 인증된 사용자의 세부 정보
     * @return 수정 완료 메시지와 최신 정보가 담긴 응답 객체 (HTTP 200)
     */
    @Operation(summary = "펫 정보 수정", description = "기존에 등록된 반려동물의 프로필을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "반려동물 정보가 성공적으로 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = PetUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "자신이 등록한 반려동물만 수정할 수 있습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"자신이 등록한 반려동물만 수정할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{petId}")
    public ResponseEntity<PetUpdateResponse> updatePet(
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("펫 정보 수정 요청 수신 - PetId: {}, User: {}", petId, userId);

        PetUpdateResponse response = petService.updatePet(petId, request, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 가족 초대 코드를 생성합니다.
     * <p>
     * 해당 반려동물에 가족 구성원을 초대하기 위한 6자리 코드를 발급합니다.
     * 발급된 코드는 <b>15분간 유효</b>하며, 유효 기간 내에는 <b>중복 발급되지 않습니다.</b>
     *
     * @param petId       초대 코드를 생성할 반려동물의 ID
     * @param userDetails 인증된 사용자 정보
     * @return 생성된 초대 코드와 만료 시간(초 단위) 정보를 담은 응답 객체
     */
    @Operation(summary = "가족 초대 코드 생성",
            description = "반려동물에 가족을 초대하기 위한 코드를 생성하고" +
                    "생성된 코드는 15분간 유효하며, 유효 기간 내에는 중복 발급되지 않습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초대 코드가 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = PetInvitationResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "자신이 등록하거나 속해있는 반려동물만 초대할 수 있습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"자신이 등록하거나 속해있는 반려동물만 초대할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 반려동물을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 유효한 초대 코드가 존재합니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 유효한 초대 코드가 존재합니다. 만료 후 다시 시도해주세요.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/{petId}/invitation-code")
    public ResponseEntity<PetInvitationResponse> createInvitationCode(
            @PathVariable Long petId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        return ResponseEntity.ok(petService.issueInvitationCode(userId, petId));
    }

    /**
     * 초대 코드를 입력하여 반려동물 가족 그룹에 참여를 신청합니다.
     * <p>
     * 유효한 코드인 경우 대기(PENDING) 상태로 등록되며,
     * 신청된 <b>펫 ID</b>와 <b>승인 대기 메시지</b>를 반환합니다.
     *
     * @param request     6자리 초대 코드가 담긴 요청 객체
     * @param userDetails 인증된 사용자 정보
     * @return 펫 ID와 처리 결과 메시지 (HTTP 200)
     */
    @Operation(summary = "가족 초대 수락 신청", description = "초대 코드를 입력하여 해당 반려동물의 가족으로 등록을 신청합니다 (승인 대기)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신청 성공",
                    content = @Content(schema = @Schema(implementation = PetFamilyJoinRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "가족 등록 신청이 차단되었습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"가족 등록 신청이 차단되었습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "만료되었거나 존재하지 않는 초대 코드입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"만료되었거나 존재하지 않는 초대 코드입니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "가족 신청 상태 충돌입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "Already Family Member", value = "{\"status\": 409, \"message\": \"이미 가족으로 등록되어있습니다.\"}"),
                                    @ExampleObject(name = "Pending Family Request", value = "{\"status\": 409, \"message\": \"이미 가족 등록 신청이 대기 중입니다.\"}"),
                                    @ExampleObject(name = "Rejected Family Request Cooldown", value = "{\"status\": 409, \"message\": \"가족 등록 신청이 거절되었습니다. 15분 후 다시 신청해주세요.\"}")
                            })),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/family")
    public ResponseEntity<PetFamilyJoinRequestResponse> joinFamily(
            @Valid @RequestBody PetFamilyJoinRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("가족 초대 수락 요청 - User: {}, Code: {}", userId, request.getCode());

        return ResponseEntity.ok(petService.applyForFamily(userId, request));
    }

    /**
     * 대기 중인 가족 등록 요청을 승인, 거절 또는 차단합니다.
     * <p>
     * 관리자(기존 가족 구성원)는 대기(PENDING) 상태인 요청을 승인하여 정식 구성원으로 등록하거나,
     * 거절 또는 차단할 수 있습니다.
     *
     * @param request     승인/거절/차단 처리에 필요한 요청 정보
     * @param userDetails 인증된 사용자 정보
     * @return 처리 결과 메시지 JSON
     */
    @Operation(summary = "가족 요청 승인/거절/차단", description = "대기 중인 가족 등록 요청을 승인, 거절 또는 차단합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공",
                    content = @Content(schema = @Schema(implementation = PetFamilyApprovalResponse.class),
                            examples = {
                                    @ExampleObject(name = "Approve", value = "{\"petId\": 1, \"message\": \"가족 신청을 승인했습니다.\"}"),
                                    @ExampleObject(name = "Reject", value = "{\"petId\": 1, \"message\": \"가족 신청을 거절했습니다.\"}"),
                                    @ExampleObject(name = "Block", value = "{\"petId\": 1, \"message\": \"가족 신청을 차단했습니다.\"}")
                            })),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "처리 권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"자신이 등록하거나 속해있는 반려동물만 초대할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "대상 유저 또는 요청을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"대상 유저 또는 요청을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/family/approval")
    public ResponseEntity<PetFamilyApprovalResponse> handleFamilyApproval(
            @Valid @RequestBody PetFamilyApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID requesterId = UUID.fromString(userDetails.getUsername());

        return ResponseEntity.ok(petService.manageFamily(
                requesterId,
                request.getPetId(),
                request.getTargetUserId(),
                request.getAction()
        ));
    }

    /**
     * 차단된 가족 신청자의 차단 상태를 해제합니다.
     * <p>
     * 차단 해제 시 해당 UserPet 관계를 삭제하여, 대상 유저가 이후 초대 코드를 통해 다시 신청할 수 있도록 합니다.
     *
     * @param request     차단 해제할 반려동물 ID와 대상 유저 ID
     * @param userDetails 인증된 사용자 정보
     * @return 처리 결과 메시지 JSON
     */
    @Operation(summary = "가족 신청 차단 해제", description = "차단된 가족 신청자의 차단 상태를 해제하여 다시 신청할 수 있도록 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "차단 해제 성공",
                    content = @Content(schema = @Schema(implementation = PetFamilyApprovalResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "차단 해제 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"자신이 등록하거나 속해있는 반려동물만 초대할 수 있습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "차단된 신청 내역을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"초대하려는 사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/family/block")
    public ResponseEntity<PetFamilyApprovalResponse> unblockFamily(
            @Valid @RequestBody PetFamilyBlockReleaseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID requesterId = UUID.fromString(userDetails.getUsername());

        return ResponseEntity.ok(petService.unblockFamily(
                requesterId,
                request.getPetId(),
                request.getTargetUserId()
        ));
    }

    /**
     * 로그인한 사용자의 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * 각 반려동물의 기본 정보(이름, 종, 품종 등)와 최신 체중 데이터를 포함합니다.
     * 페이지 번호(page)는 0부터 시작하며, 한 페이지당 기본 10개의 데이터를 반환합니다.
     *
     * @param pageable    페이징 정보 (page, size, sort)
     * @param userDetails 인증된 사용자 정보
     * @return 페이징된 반려동물 목록과 페이지 메타데이터 (HTTP 200)
     */
    @Operation(summary = "반려동물 목록 조회", description = "로그인한 사용자의 반려동물 목록을 페이징하여 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "조회할 페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, example = "0"),
            @Parameter(name = "size", description = "한 페이지에 보여줄 데이터 수", in = ParameterIn.QUERY, example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (가능 값: registrationCreatedAt, registrationUpdatedAt / 예: registrationCreatedAt,desc)", in = ParameterIn.QUERY, example = "registrationCreatedAt,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회를 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = PetListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/list")
    public ResponseEntity<PetListResponse> getPetList(
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        PetListResponse response = petService.getPetList(userId, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * 내 모든 반려동물에게 들어온 가족 신청(대기자) 목록을 페이징하여 조회합니다.
     * <p>
     * 내가 소유(APPROVED)하고 있는 모든 반려동물에 대해,
     * 가족 신청을 보낸 유저 중 대기(PENDING) 또는 거절(REJECTED) 상태인 유저들을 한 번에 모아서 조회합니다.
     * 거절된 신청은 거절 일시(rejectedAt)를 함께 반환합니다.
     *
     * @param pageable    페이징 정보 (page, size, sort)
     * @param userDetails 인증된 사용자 정보
     * @return 페이징된 대기자 목록 응답 객체 (HTTP 200)
     */
    @Operation(summary = "가족 신청 대기/거절 유저 전체 조회", description = "내가 관리하는 모든 반려동물에게 들어온 PENDING, REJECTED 가족 신청 목록을 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "조회할 페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, example = "0"),
            @Parameter(name = "size", description = "한 페이지에 보여줄 데이터 수", in = ParameterIn.QUERY, example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (가능 값: registrationCreatedAt, registrationUpdatedAt / 예: registrationCreatedAt,desc)", in = ParameterIn.QUERY, example = "registrationCreatedAt,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회를 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = PendingUserListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/family/pending-users")
    public ResponseEntity<PendingUserListResponse> getAllPendingUsers(
            @Parameter(name = "status", description = "신청 상태 필터 (PENDING 또는 REJECTED). 미전달 시 전체 조회", in = ParameterIn.QUERY, example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID managerId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.getAllPendingUsers(managerId, pageable, status));
    }

    /**
     * 내 모든 반려동물에서 차단된 가족 신청자 목록을 페이징하여 조회합니다.
     * <p>
     * 내가 소유(APPROVED)하고 있는 모든 반려동물에 대해,
     * 차단(BLOCKED) 상태인 유저들을 한 번에 모아서 조회합니다.
     *
     * @param pageable    페이징 정보 (page, size, sort)
     * @param userDetails 인증된 사용자 정보
     * @return 페이징된 차단 유저 목록 응답 객체 (HTTP 200)
     */
    @Operation(summary = "가족 신청 차단 유저 전체 조회", description = "내가 관리하는 모든 반려동물에서 차단된 가족 신청자 목록을 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "조회할 페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, example = "0"),
            @Parameter(name = "size", description = "한 페이지에 보여줄 데이터 수", in = ParameterIn.QUERY, example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (가능 값: registrationCreatedAt, registrationUpdatedAt / 예: registrationUpdatedAt,desc)", in = ParameterIn.QUERY, example = "registrationUpdatedAt,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회를 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = BlockedUserListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/family/blocked-users")
    public ResponseEntity<BlockedUserListResponse> getAllBlockedUsers(
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID managerId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.getAllBlockedUsers(managerId, pageable));
    }

    /**
     * 내가 가족 신청을 보낸 후 대기 또는 거절 상태인 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * 아직 승인되지 않은(PENDING) 신청과 거절된(REJECTED) 신청 내역을 조회하며,
     * 거절된 신청은 거절 일시(rejectedAt)를 함께 반환합니다.
     *
     * @param pageable    페이징 정보 (page, size, sort)
     * @param userDetails 인증된 사용자 정보
     * @return 페이징된 나의 신청 내역 목록 응답 객체 (HTTP 200)
     */
    @Operation(summary = "내 신청 내역 조회", description = "내가 가족 신청을 했으나 아직 승인되지 않았거나 거절된(PENDING, REJECTED) 펫 목록을 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "조회할 페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, example = "0"),
            @Parameter(name = "size", description = "한 페이지에 보여줄 데이터 수", in = ParameterIn.QUERY, example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (가능 값: registrationCreatedAt, registrationUpdatedAt / 예: registrationCreatedAt,desc)", in = ParameterIn.QUERY, example = "registrationCreatedAt,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회를 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = PetApplicationListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/family/applications")
    public ResponseEntity<PetApplicationListResponse> getMyPendingApplications(
            @Parameter(name = "status", description = "신청 상태 필터 (PENDING 또는 REJECTED). 미전달 시 전체 조회", in = ParameterIn.QUERY, example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.getMyPendingApplications(userId, pageable, status));
    }

    /**
     * 반려동물 가족 그룹에서 나갑니다.
     * <p>
     * 해당 사용자와 반려동물의 연결을 해제합니다.
     * 만약 해당 반려동물의 마지막 구성원이라면 펫 정보도 영구적으로 삭제됩니다.
     *
     * @param petId       나가려는 반려동물의 ID
     * @param userDetails 인증된 사용자 정보
     * @return 결과 메시지 (HTTP 200)
     */
    @Operation(summary = "펫 가족 나가기 (삭제)", description = "반려동물 가족 목록에서 나가게 되는데 사용자 외에 다른 가족이 남아있다면 펫 정보는 유지됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "반려동물 목록에서 삭제되었습니다.",
                    content = @Content(schema = @Schema(implementation = PetDeleteResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/{petId}")
    public ResponseEntity<PetDeleteResponse> deletePet(
            @PathVariable Long petId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        petService.deletePet(userId, petId);

        return ResponseEntity.ok(PetDeleteResponse.toDto("반려동물 목록에서 삭제되었습니다."));
    }

    /**
     * 반려동물의 디바이스를 재등록(변경)합니다.
     * <p>
     * 기존에 등록된 IoT 디바이스 ID를 새로운 ID로 교체합니다.
     * 해당 반려동물의 등록자(소유자)만 변경할 수 있으며,
     * 이미 다른 반려동물에 사용 중인 디바이스 ID로는 변경할 수 없습니다.
     *
     * @param petId       디바이스를 변경할 펫 ID
     * @param request     새로운 디바이스 ID 정보
     * @param userDetails 인증된 사용자 정보
     * @return 변경된 디바이스 정보 (HTTP 200)
     */
    @Operation(summary = "펫 디바이스 재등록", description = "반려동물의 IoT 디바이스 ID를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "디바이스가 성공적으로 재등록되었습니다.",
                    content = @Content(schema = @Schema(implementation = PetDeviceUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "409", description = "이미 다른 반려동물에 등록된 디바이스 ID입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "409 Conflict", value = "{\"status\": 409, \"message\": \"이미 다른 반려동물에 등록된 디바이스 ID입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PutMapping("/{petId}/device")
    public ResponseEntity<PetDeviceUpdateResponse> updateDevice(
            @PathVariable Long petId,
            @Valid @RequestBody PetDeviceUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        PetDeviceUpdateResponse response = petService.updateDevice(userId, petId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 디바이스 ID 중복 여부를 확인합니다.
     *
     * @param request     확인할 디바이스 ID
     * @return 디바이스 ID 사용 가능 여부 응답
     */
    @Operation(summary = "디바이스 ID 중복 확인", description = "펫 등록 전에 디바이스 ID가 이미 다른 반려동물에 등록되어 있는지 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "디바이스 ID 중복 확인 결과",
                    content = @Content(schema = @Schema(implementation = PetDeviceCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/device/check")
    public ResponseEntity<PetDeviceCheckResponse> checkDeviceId(
            @Valid @RequestBody PetDeviceCheckRequest request) {

        log.info("디바이스 ID 중복 확인 요청 - DeviceId: {}", request.getDeviceId());

        return ResponseEntity.ok(petService.checkDeviceIdAvailability(request));
    }

    /**
     * 반려동물 상세 정보를 조회합니다.
     *
     * @param petId       조회할 반려동물 ID
     * @param userDetails 인증된 사용자 정보
     * @return 반려동물 상세 정보 응답
     */
    @Operation(summary = "펫 상세 조회", description = "반려동물 기본 정보, 가족 구성원, 최근 활동, 특이사항, 체중 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "반려동물 정보 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = PetDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 조회 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 조회 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 ID의 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{petId}")
    public ResponseEntity<PetDetailResponse> getPetDetail(
            @PathVariable Long petId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.getPetDetail(userId, petId));
    }

    /**
     * 반려동물 특이사항을 생성합니다.
     *
     * @param request     특이사항 생성 요청 정보
     * @param userDetails 인증된 사용자 정보
     * @return 생성 결과 메시지와 생성된 특이사항 ID
     */
    @Operation(summary = "펫 특이사항 생성", description = "반려동물의 특이사항 메모를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "펫 특이사항 등록을 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetSignificantCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 반려동물입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 반려동물입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/significant")
    public ResponseEntity<PetSignificantCreateResponse> createPetSignificant(
            @Valid @RequestBody PetSignificantCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.createPetSignificant(userId, request));
    }

    /**
     * 반려동물 특이사항 목록을 페이지네이션으로 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @param page 페이지 번호(0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 조건(property,direction)
     * @param userDetails 인증된 사용자 정보
     * @return 특이사항 목록 페이징 응답
     */
    @Operation(summary = "펫 특이사항 목록 조회", description = "반려동물 특이사항 목록을 페이지네이션으로 조회합니다.")
    @Parameters(value = {
            @Parameter(name = "petId", description = "특이사항을 조회할 반려동물 ID", required = true, in = ParameterIn.PATH, example = "1"),
            @Parameter(name = "page", description = "페이지 번호(0부터 시작)", required = false, in = ParameterIn.QUERY, example = "0"),
            @Parameter(name = "size", description = "페이지 크기", required = false, in = ParameterIn.QUERY, example = "10"),
            @Parameter(name = "sort", description = "정렬 조건(property,direction)", required = false, in = ParameterIn.QUERY, example = "createdAt,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "펫 특이사항 목록 조회를 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetSignificantListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 반려동물입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 반려동물입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{petId}/significant")
    public ResponseEntity<PetSignificantListResponse> getPetSignificantList(
            @PathVariable Long petId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.getPetSignificantList(userId, petId, page, size, sort));
    }

    /**
     * 반려동물 특이사항을 수정합니다.
     *
     * @param noteId      수정할 특이사항 ID
     * @param request     특이사항 수정 요청 정보
     * @param userDetails 인증된 사용자 정보
     * @return 수정 결과 메시지와 특이사항 ID
     */
    @Operation(summary = "펫 특이사항 수정", description = "반려동물 특이사항의 내용 또는 타입을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "펫 특이사항 수정을 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetSignificantUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 특이사항입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 특이사항입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/significant/{noteId}")
    public ResponseEntity<PetSignificantUpdateResponse> updatePetSignificant(
            @PathVariable Long noteId,
            @RequestBody PetSignificantUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.updatePetSignificant(userId, noteId, request));
    }

    /**
     * 반려동물 특이사항을 삭제합니다.
     *
     * @param noteId      삭제할 특이사항 ID
     * @param userDetails 인증된 사용자 정보
     * @return 삭제 결과 메시지와 특이사항 ID
     */
    @Operation(summary = "펫 특이사항 삭제", description = "반려동물 특이사항을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "펫 특이사항 삭제를 완료했습니다.",
                    content = @Content(schema = @Schema(implementation = PetSignificantDeleteResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "해당 반려동물에 대한 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"해당 반려동물에 대한 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 특이사항입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"존재하지 않는 특이사항입니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @DeleteMapping("/significant/{noteId}")
    public ResponseEntity<PetSignificantDeleteResponse> deletePetSignificant(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(petService.deletePetSignificant(userId, noteId));
    }
}
