package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 가족 및 멤버십(UserPet) 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface UserPetService {

    /**
     * 사용자와 펫 사이의 관계(멤버십)를 생성하고 저장합니다.
     *
     * @param userId   관계를 맺을 사용자 엔티티
     * @param pet    관계를 맺을 펫 엔티티
     * @param status 등록 상태 (예: APPROVED, PENDING)
     */
    void registerUserPet(UUID userId, Pet pet, RegistrationStatus status);


    /**
     * 가족 초대를 위한 코드를 생성합니다.
     * <p>
     * 요청한 사용자가 해당 반려동물의 승인된(APPROVED) 가족 구성원인지 검증한 후,
     * 초대 코드를 생성하여 Redis에 저장하고 반환합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param petId  반려동물 ID
     * @return 코드("code")와 만료시간("expiredAt")이 담긴 Map 객체
     */
    Map<String, Object> generateInvitationCode(UUID userId, Long petId);

    /**
     * 초대 코드를 검증하고, 해당 사용자를 가족 구성원(PENDING)으로 등록합니다.
     *
     * @param userId 초대 코드를 입력한 사용자의 고유 식별자(UUID)
     * @param code   사용자가 입력한 6자리 초대 코드
     * @return 등록 신청된 반려동물의 ID (Long)
     */
    Long registerByInvitation(UUID userId, String code);

    /**
     * 사용자가 속한 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * 결과는 {@code Map<String, Object>} 형태로 반환되며,
     * 내부에는 페이징된 데이터 객체("userPetPage")가 포함됩니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징 결과가 담긴 Map
     */
    Map<String, Object> getUserPets(UUID userId, Pageable pageable);

    /**
     * 대기 중인(PENDING) 가족 등록 요청을 승인하거나 거절합니다.
     *
     * @param requesterId 요청을 수행하는 관리자(기존 가족) ID
     * @param petId       반려동물 ID
     * @param targetUserId 승인/거절 대상 유저 ID
     * @param action      처리할 상태 문자열 ("APPROVED" 또는 "REJECTED")
     * @return 처리 결과 메시지
     */
    String approveOrRejectFamilyMember(UUID requesterId, Long petId, UUID targetUserId, String action);

    /**
     * 차단된 가족 신청자를 차단 해제합니다.
     *
     * @param requesterId  요청을 수행하는 관리자(기존 가족) ID
     * @param petId        반려동물 ID
     * @param targetUserId 차단 해제 대상 유저 ID
     * @return 처리 결과 메시지
     */
    String unblockFamilyMember(UUID requesterId, Long petId, UUID targetUserId);

    /**
     * 특정 반려동물에게 신청된 승인 대기(PENDING) 상태의 유저 목록을 조회합니다.
     *
     * @param managerId 요청을 수행하는 관리자(기존 가족)의 UUID
     * @param pageable  페이징 정보
     * @return "pendingUserPage" 키에 Page&lt;UserPet&gt; 엔티티가 담긴 Map 객체
     */
    Map<String, Object> getAllPendingUsers(UUID managerId, Pageable pageable, String status);

    /**
     * 특정 반려동물에게 차단된(BLOCKED) 상태의 유저 목록을 조회합니다.
     *
     * @param managerId 요청을 수행하는 관리자(기존 가족)의 UUID
     * @param pageable  페이징 정보
     * @return "blockedUserPage" 키에 Page&lt;UserPet&gt; 엔티티가 담긴 Map 객체
     */
    Map<String, Object> getAllBlockedUsers(UUID managerId, Pageable pageable);

    /**
     * 사용자가 신청했으나 아직 승인되지 않은(PENDING) 반려동물 목록을 조회합니다.
     *
     * @param userId   조회할 사용자의 UUID
     * @param pageable 페이징 정보
     * @return "pendingPetPage" 키에 Page&lt;UserPet&gt; 엔티티가 담긴 Map 객체
     */
    Map<String, Object> getMyPendingPets(UUID userId, Pageable pageable, String status);

    /**
     * 해당 유저가 특정 반려동물의 정식 소유자(APPROVED)인지 확인합니다.
     * 보안 검증(IDOR 방지)을 위해 사용됩니다.
     *
     * @param userId 확인할 유저 ID
     * @param petId 확인할 펫 ID
     * @return 소유자라면 true, 아니면 false
     */
    boolean isUserPetOwner(UUID userId, Long petId);

    /**
     * 특정 유저와 펫의 연결 관계(UserPet)를 삭제합니다. (가족 나가기)
     *
     * @param userId 삭제할 유저 ID
     * @param petId  삭제할 펫 ID
     */
    void deleteUserPetRelation(UUID userId, Long petId);

    /**
     * 해당 펫에 등록된 가족이 한 명이라도 남아있는지 확인합니다.
     *
     * @param petId 확인할 펫 ID
     * @return 가족이 남아있다면 true
     */
    boolean existsFamilyMember(Long petId);

    /**
     * 해당 유저가 특정 펫의 '승인된(APPROVED)' 주인인지 확인합니다.
     * <p>
     * ID 기반의 효율적인 조회를 통해 소유권을 검증합니다.
     * </p>
     *
     * @param userId 확인할 유저 ID
     * @param petId  확인할 펫 ID
     * @return 승인된 주인이라면 true, 그렇지 않다면 false
     */
    boolean isApprovedPetOwner(UUID userId, Long petId);

    /**
     * 유저의 존재 여부를 확인합니다.
     * <p>
     * PetService 등에서 유저 검증이 필요할 때 호출하여 사용합니다.
     *
     * @param userId 검증할 유저의 UUID
     * @return 유저가 존재하면 true, 아니면 false
     */
    boolean existsUser(UUID userId);

    /**
     * 특정 사용자의 승인(APPROVED)된 반려동물 ID 목록을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 승인된 반려동물 ID 목록
     */
    List<Long> getApprovedPetIds(UUID userId);

    /**
     * 특정 반려동물의 승인(APPROVED) 가족 구성원 목록을 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @return 승인된 가족 구성원 목록
     */
    List<UserPet> getApprovedFamilyMembers(Long petId);
}
