package com.dodo.backend.pet.service;

import com.dodo.backend.pet.dto.request.PetRequest.*;
import com.dodo.backend.pet.dto.response.PetResponse.*;
import com.dodo.backend.pet.entity.Pet;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * 펫 등록, 조회, 수정 등 반려동물과 관련된 핵심 비즈니스 로직을 정의하는 인터페이스입니다.
 */
public interface PetService {

    /**
     * 사용자의 요청 정보를 바탕으로 새로운 펫을 생성하고, 해당 사용자와의 소유 관계를 설정합니다.
     *
     * @param userId  펫을 등록하려는 사용자의 고유 식별자 (UUID)
     * @param request 펫 등록에 필요한 상세 정보를 담은 요청 객체
     * @return 등록 완료된 펫의 식별자를 포함한 응답 객체
     */
    PetRegisterResponse registerPet(UUID userId, PetRegisterRequest request);

    /**
     * 반려동물의 정보를 선택적으로 수정합니다.
     *
     * @param petId   수정할 반려동물의 고유 ID
     * @param request 수정할 정보를 담은 요청 객체
     * @return 수정이 완료된 후의 최신 반려동물 상세 정보 응답 객체
     */
    PetUpdateResponse updatePet(Long petId, PetUpdateRequest request, UUID userId);

    /**
     * 반려동물 가족 초대를 위한 코드를 발급합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param petId  반려동물 ID
     * @return 발급된 초대 코드와 만료 시간 정보
     */
    PetInvitationResponse issueInvitationCode(UUID userId, Long petId);

    /**
     * 사용자가 입력한 초대 코드를 통해 반려동물 가족 그룹 참여를 신청합니다.
     *
     * @param userId  요청한 사용자의 ID
     * @param request 초대 코드가 포함된 요청 DTO
     * @return 신청된 펫 ID와 처리 결과 메시지
     */
    PetFamilyJoinRequestResponse applyForFamily(UUID userId, PetFamilyJoinRequest request);

    /**
     * 사용자의 반려동물 목록을 페이징하여 조회합니다.
     *
     * @param userId   조회할 사용자의 ID
     * @param pageable 페이징 요청 정보
     * @return 페이징 처리된 반려동물 목록 응답 DTO
     */
    PetListResponse getPetList(UUID userId, Pageable pageable);

    /**
     * 대기 중인 가족 등록 요청을 승인하거나 거절합니다.
     *
     * @param requesterId  요청을 수행하는 관리자(기존 가족) ID
     * @param petId        반려동물 ID
     * @param targetUserId 승인/거절 대상 유저 ID
     * @param action       처리할 상태 문자열 ("APPROVED" 또는 "REJECTED")
     * @return 펫 ID와 처리 결과 메시지
     */
    PetFamilyApprovalResponse manageFamily(UUID requesterId, Long petId, UUID targetUserId, String action);

    /**
     * 차단된 가족 신청자를 차단 해제합니다.
     *
     * @param requesterId  요청을 수행하는 관리자(기존 가족) ID
     * @param petId        반려동물 ID
     * @param targetUserId 차단 해제 대상 유저 ID
     * @return 펫 ID와 처리 결과 메시지
     */
    PetFamilyApprovalResponse unblockFamily(UUID requesterId, Long petId, UUID targetUserId);

    /**
     * 특정 반려동물에게 들어온 가족 신청 대기자 목록을 페이징하여 조회합니다.
     *
     * @param managerId 요청자(관리자)의 UUID
     * @param pageable  페이징 정보
     * @return 페이징된 대기자 목록 응답 DTO
     */
    PendingUserListResponse getAllPendingUsers(UUID managerId, Pageable pageable, String status);

    /**
     * 차단된 가족 신청자 목록을 페이징하여 조회합니다.
     *
     * @param managerId 요청자(관리자)의 UUID
     * @param pageable  페이징 정보
     * @return 페이징된 차단 유저 목록 응답 DTO
     */
    BlockedUserListResponse getAllBlockedUsers(UUID managerId, Pageable pageable);

    /**
     * 내가 신청했지만 아직 승인 대기 중인 반려동물 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자의 UUID
     * @param pageable 페이징 정보
     * @return 페이징된 신청 내역 목록 응답 DTO
     */
    PetApplicationListResponse getMyPendingApplications(UUID userId, Pageable pageable, String status);

    /**
     * 반려동물 상세 정보를 조회합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param petId  조회할 반려동물 ID
     * @return 반려동물 상세 정보 응답 DTO
     */
    PetDetailResponse getPetDetail(UUID userId, Long petId);

    /**
     * 반려동물 정보를 삭제합니다.
     *
     * @param userId 요청한 사용자의 ID (권한 검증용)
     * @param petId  삭제할 반려동물 ID
     */
    void deletePet(UUID userId, Long petId);

    /**
     * 반려동물의 디바이스를 재등록(수정)합니다.
     *
     * @param userId  요청한 사용자 ID
     * @param petId   반려동물 ID
     * @param request 디바이스 변경 요청 정보
     * @return 변경된 디바이스 정보 응답
     */
    PetDeviceUpdateResponse updateDevice(UUID userId, Long petId, PetDeviceUpdateRequest request);

    /**
     * 디바이스 ID 중복 여부를 확인합니다.
     *
     * @param request 확인할 디바이스 ID가 포함된 요청 DTO
     * @return 디바이스 ID 사용 가능 여부 응답
     */
    PetDeviceCheckResponse checkDeviceIdAvailability(PetDeviceCheckRequest request);

    /**
     * 반려동물 특이사항을 생성합니다.
     *
     * @param userId  요청한 사용자 ID
     * @param request 특이사항 생성 요청 DTO
     * @return 특이사항 생성 응답 DTO
     */
    PetSignificantCreateResponse createPetSignificant(UUID userId, PetSignificantCreateRequest request);

    /**
     * 반려동물 특이사항을 수정합니다.
     *
     * @param userId  요청한 사용자 ID
     * @param noteId  수정할 특이사항 ID
     * @param request 특이사항 수정 요청 DTO
     * @return 특이사항 수정 응답 DTO
     */
    PetSignificantUpdateResponse updatePetSignificant(UUID userId, Long noteId, PetSignificantUpdateRequest request);

    /**
     * 반려동물 특이사항을 삭제합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param noteId 삭제할 특이사항 ID
     * @return 특이사항 삭제 응답 DTO
     */
    PetSignificantDeleteResponse deletePetSignificant(UUID userId, Long noteId);

    /**
     * 반려동물 특이사항 목록을 페이지네이션으로 조회합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param petId 조회할 반려동물 ID
     * @param page 페이지 번호(0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 조건 문자열(property,direction)
     * @return 특이사항 목록 페이징 응답 DTO
     */
    PetSignificantListResponse getPetSignificantList(UUID userId, Long petId, int page, int size, String sort);

    /**
     * 디바이스 ID로 등록된 반려동물 ID를 조회합니다.
     *
     * @param deviceId 조회할 디바이스 고유 ID
     * @return 펫 ID (존재하지 않으면 Optional.empty())
     */
    Optional<Long> findPetIdByDeviceId(String deviceId);

    /**
     * ID로 펫을 조회합니다.
     * <p>
     * 존재하지 않을 경우 {@link com.dodo.backend.pet.exception.PetException} (PET_NOT_FOUND)를 던집니다.
     * </p>
     *
     * @param petId 조회할 펫 ID
     * @return 조회된 Pet 엔티티
     */
    Pet getPetById(Long petId);

    /**
     * 반려동물 ID 존재 여부를 확인합니다.
     *
     * @param petId 확인할 반려동물 ID
     * @return 존재하면 true, 아니면 false
     */
    boolean existsPetById(Long petId);

    /**
     * 특정 반려동물의 평균 심박수(BPM)를 조회합니다.
     * @param petId 반려동물 ID
     * @return 평균 심박수 (설정되지 않았으면 null 반환)
     */
    Integer getAverageHeartRate(Long petId);

    /**
     * 디바이스 토큰의 Principal(subject)과 반려동물 ID의 매핑 일치 여부를 검증합니다.
     *
     * @param principalName 인증 주체 문자열(UUID)
     * @param petId         반려동물 ID
     * @return 매핑이 일치하면 true, 아니면 false
     */
    boolean isDevicePrincipalMatchedPet(String principalName, Long petId);
}
