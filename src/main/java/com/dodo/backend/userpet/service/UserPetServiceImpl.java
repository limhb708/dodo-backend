package com.dodo.backend.userpet.service;

import com.dodo.backend.common.util.VerificationCodeGenerator;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import com.dodo.backend.userpet.exception.UserPetErrorCode;
import com.dodo.backend.userpet.exception.UserPetException;
import com.dodo.backend.userpet.mapper.UserPetMapper;
import com.dodo.backend.userpet.repository.UserPetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.dodo.backend.user.exception.UserErrorCode.USER_NOT_FOUND;
import static com.dodo.backend.userpet.exception.UserPetErrorCode.*;

/**
 * {@link UserPetService}의 구현체로, 가족 및 멤버십 도메인의 비즈니스 로직을 수행합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPetServiceImpl implements UserPetService {

    private final UserPetRepository userPetRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserPetMapper userPetMapper;

    private static final long EXPIRATION_MINUTES = 15;
    private static final long REAPPLY_COOLDOWN_MINUTES = 15;
    private static final String REDIS_CODE_KEY_PREFIX = "invitation:code:";
    private static final String REDIS_PET_KEY_PREFIX = "invitation:pet:";

    /**
     * 사용자와 반려동물 간의 관계를 생성하고 저장합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>{@link UserRepository}를 사용하여 User 엔티티를 직접 조회합니다. (존재하지 않을 경우 예외 발생)</li>
     * <li>User ID와 Pet ID를 조합하여 복합키({@link UserPetId})를 생성합니다.</li>
     * <li>전달받은 상태값(APPROVED, PENDING 등)을 포함하여 {@link UserPet} 엔티티를 빌드하고 저장합니다.</li>
     * </ol>
     *
     * @param userId 관계를 맺을 사용자의 UUID
     * @param pet    관계를 맺을 반려동물 엔티티 (또는 Proxy 객체)
     * @param status 초기 등록 상태
     * @throws UserException 해당 사용자가 존재하지 않을 경우
     */
    @Transactional
    @Override
    public void registerUserPet(UUID userId, Pet pet, RegistrationStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        UserPetId userPetId = new UserPetId(user.getUsersId(), pet.getPetId());

        UserPet userPet = UserPet.builder()
                .id(userPetId)
                .user(user)
                .pet(pet)
                .registrationStatus(status)
                .build();

        userPetRepository.save(userPet);
    }

    /**
     * 가족 초대를 위한 코드를 생성하고 Redis에 저장합니다.
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>요청한 사용자가 해당 펫의 가족 구성원이며, '승인된(APPROVED)' 상태인지 검증합니다.</li>
     * <li>해당 펫에 대해 이미 발급된 유효한 초대 코드가 있는지 Redis를 통해 확인하여 중복 발급을 방지합니다.</li>
     * <li>6자리의 랜덤 초대 코드를 생성합니다.</li>
     * <li>초대 코드 검증을 위한 키(code:petId)와 중복 방지용 키(petId:code)를 Redis에 저장합니다. (유효기간 15분)</li>
     * </ol>
     *
     * @param userId 요청한 사용자의 UUID
     * @param petId  초대 코드를 생성할 반려동물 ID
     * @return 생성된 코드와 만료 시간이 담긴 Map
     * @throws UserPetException 권한이 없거나 이미 유효한 코드가 존재하는 경우
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> generateInvitationCode(UUID userId, Long petId) {
        UserPet userPet = userPetRepository.findById(new UserPet.UserPetId(userId, petId))
                .filter(up -> up.getRegistrationStatus() == RegistrationStatus.APPROVED)
                .orElseThrow(() -> new UserPetException(INVITE_PERMISSION_DENIED));

        String petKey = REDIS_PET_KEY_PREFIX + petId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(petKey))) {
            throw new UserPetException(INVITATION_ALREADY_EXISTS);
        }

        String code = VerificationCodeGenerator.generateInvitationCode();
        long expiresIn = EXPIRATION_MINUTES * 60;

        redisTemplate.opsForValue().set(
                REDIS_CODE_KEY_PREFIX + code,
                String.valueOf(petId),
                Duration.ofMinutes(EXPIRATION_MINUTES)
        );

        redisTemplate.opsForValue().set(
                petKey,
                code,
                Duration.ofMinutes(EXPIRATION_MINUTES)
        );

        log.info("가족 초대 코드 생성 완료 - PetId: {}, User: {}, Code: {}", petId, userId, code);

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("expiresIn", expiresIn);

        return result;
    }

    /**
     * 초대 코드를 확인하고 가족 등록(PENDING)을 수행합니다.
     * <p>
     * <ol>
     * <li>Redis에서 입력된 코드를 조회하여 유효성을 검증하고, 매핑된 Pet ID를 가져옵니다. (없으면 예외 발생)</li>
     * <li>사용자가 이미 해당 펫의 가족으로 등록되어 있는지 중복 여부를 확인합니다.</li>
     * <li>Pet 엔티티 조회를 생략하고, ID만 포함된 Proxy Pet 객체를 생성합니다.</li>
     * <li>{@link #registerUserPet}을 호출하여 사용자를 대기(PENDING) 상태로 등록합니다.</li>
     * </ol>
     */
    @Transactional
    @Override
    public Long registerByInvitation(UUID userId, String code) {
        String petIdStr = (String) redisTemplate.opsForValue().get(REDIS_CODE_KEY_PREFIX + code);

        if (petIdStr == null) {
            throw new UserPetException(INVITATION_NOT_FOUND);
        }

        Long petId = Long.valueOf(petIdStr);

        Optional<UserPet> existingUserPet = userPetRepository.findById(new UserPetId(userId, petId));
        if (existingUserPet.isPresent()) {
            UserPet userPet = existingUserPet.get();
            if (userPet.getRegistrationStatus() == RegistrationStatus.PENDING) {
                throw new UserPetException(FAMILY_REQUEST_PENDING);
            }
            if (userPet.getRegistrationStatus() == RegistrationStatus.APPROVED) {
                throw new UserPetException(ALREADY_FAMILY_MEMBER);
            }
            if (userPet.getRegistrationStatus() == RegistrationStatus.BLOCKED) {
                throw new UserPetException(FAMILY_REQUEST_BLOCKED);
            }
            if (userPet.getRegistrationStatus() == RegistrationStatus.REJECTED) {
                validateRejectedReapplyCooldown(userPet);
                userPetMapper.updateRegistrationStatus(userId, petId, RegistrationStatus.PENDING.name());
                log.info("가족 초대 재신청 (대기) - User: {}, PetId: {}", userId, petId);
                return petId;
            }
        }

        Pet petRef = Pet.builder().petId(petId).build();

        this.registerUserPet(userId, petRef, RegistrationStatus.PENDING);
        log.info("가족 초대 요청 (대기) - User: {}, PetId: {}", userId, petId);

        return petId;
    }

    private void validateRejectedReapplyCooldown(UserPet userPet) {
        LocalDateTime rejectedAt = userPet.getRegistrationUpdatedAt();
        if (rejectedAt == null) {
            rejectedAt = userPet.getRegistrationCreatedAt();
        }
        if (rejectedAt == null) {
            return;
        }

        LocalDateTime reapplyAvailableAt = rejectedAt.plusMinutes(REAPPLY_COOLDOWN_MINUTES);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(reapplyAvailableAt)) {
            long remainingSeconds = Duration.between(now, reapplyAvailableAt).getSeconds();
            long remainingMinutes = Math.max(1, (remainingSeconds + 59) / 60);
            throw new UserPetException(
                    FAMILY_REQUEST_REJECTED,
                    String.format("가족 등록 신청이 거절되었습니다. %d분 후 다시 신청해주세요.", remainingMinutes)
            );
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>리포지토리를 호출하여 특정 사용자와 연결된 {@link UserPet} 목록을 페이징 조회합니다.</li>
     * <li>이때, 성능 최적화를 위해 연관된 {@link com.dodo.backend.pet.entity.Pet} 엔티티를 함께 로딩(Fetch Join)합니다.</li>
     * <li>조회된 {@code Page<UserPet>} 결과를 Map에 "userPetPage"라는 키로 담아 반환합니다.</li>
     * </ol>
     *
     * @param userId   조회할 사용자의 고유 식별자(UUID)
     * @param pageable 페이지 번호, 크기 정렬 방식 등을 포함한 페이징 요청 정보
     * @return 페이징된 {@code Page<UserPet>} 객체를 "userPetPage" 키로 포함하는 Map 객체
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getUserPets(UUID userId, Pageable pageable) {
        Page<UserPet> userPetPage = userPetRepository.findAllByUser_UsersIdAndRegistrationStatus(
                userId,
                RegistrationStatus.APPROVED,
                pageable
        );

        Map<String, Object> result = new HashMap<>();
        result.put("userPetPage", userPetPage);

        return result;
    }

    /**
     * 대기 중인(PENDING) 가족 등록 요청을 승인, 거절 또는 차단합니다.
     * <p>
     * <ol>
     * <li><b>권한 및 대상 검증:</b> {@code filter}를 사용하여 요청자({@code requesterId})가 승인된(APPROVED) 가족인지,
     * 대상({@code targetUserId})이 대기(PENDING) 상태인지 검증합니다. (조건 불만족 시 예외 발생)</li>
     * <li><b>요청 처리:</b> 입력된 {@code action} 문자열에 따라 분기 처리합니다.
     * <ul>
     * <li>{@code "APPROVED"}: 상태를 승인으로 변경하여 저장하고 승인 메시지를 반환합니다.</li>
     * <li>{@code "REJECTED"}: 상태를 거절로 변경하여 저장하고 거절 메시지를 반환합니다.</li>
     * <li>{@code "BLOCKED"}: 상태를 차단으로 변경하여 저장하고 차단 메시지를 반환합니다.</li>
     * <li>그 외: 유효하지 않은 요청으로 간주하여 예외를 발생시킵니다.</li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param requesterId 요청을 수행하는 관리자(기존 가족 구성원)의 UUID
     * @param petId       반려동물 식별자(ID)
     * @param targetUserId 승인, 거절 또는 차단할 대상 유저의 UUID
     * @param action      처리할 상태 문자열 ("APPROVED", "REJECTED" 또는 "BLOCKED")
     * @return 처리 결과 메시지
     * @throws UserPetException 권한이 없거나, 대상이 없거나, 유효하지 않은 요청 상태일 경우 발생
     */
    @Transactional
    @Override
    public String approveOrRejectFamilyMember(UUID requesterId, Long petId, UUID targetUserId, String action) {

        userPetRepository.findById(new UserPetId(requesterId, petId))
                .filter(up -> up.getRegistrationStatus() == RegistrationStatus.APPROVED)
                .orElseThrow(() -> new UserPetException(INVITE_PERMISSION_DENIED));

        userPetRepository.findById(new UserPetId(targetUserId, petId))
                .filter(up -> up.getRegistrationStatus() == RegistrationStatus.PENDING)
                .orElseThrow(() -> new UserPetException(INVITEE_NOT_FOUND));

        String message;
        if ("APPROVED".equals(action)) {
            message = "가족 신청을 승인했습니다.";
        } else if ("REJECTED".equals(action)) {
            message = "가족 신청을 거절했습니다.";
        } else if ("BLOCKED".equals(action)) {
            message = "가족 신청을 차단했습니다.";
        } else {
            throw new UserPetException(INVALID_REQUEST);
        }

        userPetMapper.updateRegistrationStatus(targetUserId, petId, action);

        log.info("가족 요청 처리 완료 - PetId: {}, TargetUser: {}, Status: {}", petId, targetUserId, action);

        return message;
    }

    @Transactional
    @Override
    public String unblockFamilyMember(UUID requesterId, Long petId, UUID targetUserId) {

        userPetRepository.findById(new UserPetId(requesterId, petId))
                .filter(up -> up.getRegistrationStatus() == RegistrationStatus.APPROVED)
                .orElseThrow(() -> new UserPetException(INVITE_PERMISSION_DENIED));

        UserPet blockedUserPet = userPetRepository.findById(new UserPetId(targetUserId, petId))
                .filter(up -> up.getRegistrationStatus() == RegistrationStatus.BLOCKED)
                .orElseThrow(() -> new UserPetException(INVITEE_NOT_FOUND));

        userPetRepository.delete(blockedUserPet);

        log.info("가족 신청 차단 해제 완료 - PetId: {}, TargetUser: {}", petId, targetUserId);

        return "가족 신청 차단을 해제했습니다.";
    }

    /**
     * [관리자용] 관리자가 소유한 모든 반려동물에 대해 들어온 승인 대기(PENDING) 유저 목록을 조회합니다.
     * <p>
     * <ol>
     * <li>별도의 petId 검증 없이, 리포지토리 쿼리를 통해 '내가 관리자인 펫'들의 요청만 필터링하여 조회합니다.</li>
     * <li>조회된 Entity Page를 Map에 담아 반환합니다.</li>
     * </ol>
     *
     * @param managerId 요청을 수행하는 관리자(기존 가족)의 UUID
     * @param pageable  페이징 정보
     * @return "pendingUserPage" 키에 {@code Page<UserPet>} 엔티티가 담긴 Map 객체
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllPendingUsers(UUID managerId, Pageable pageable, String status) {

        Page<UserPet> pendingUserPage = userPetRepository.findAllPendingRequestsByManager(
                managerId,
                resolveApplicationStatuses(status),
                pageable
        );

        Map<String, Object> result = new HashMap<>();
        result.put("pendingUserPage", pendingUserPage);

        return result;
    }

    /**
     * [관리자용] 관리자가 소유한 모든 반려동물에 대해 차단된(BLOCKED) 유저 목록을 조회합니다.
     * <p>
     * 별도의 petId 검증 없이, 리포지토리 쿼리를 통해 요청자가 승인된 가족으로 속한
     * 반려동물들의 차단 내역만 필터링하여 조회합니다.
     *
     * @param managerId 요청을 수행하는 관리자(기존 가족)의 UUID
     * @param pageable  페이징 요청 정보
     * @return "blockedUserPage" 키에 {@code Page<UserPet>} 엔티티가 담긴 Map 객체
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getAllBlockedUsers(UUID managerId, Pageable pageable) {

        Page<UserPet> blockedUserPage = userPetRepository.findAllBlockedRequestsByManager(managerId, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("blockedUserPage", blockedUserPage);

        return result;
    }

    /**
     * 사용자의 가족 신청 내역(PENDING, REJECTED)을 조회합니다.
     * <p>
     * <ol>
     * <li>리포지토리를 통해 해당 유저의 PENDING, REJECTED 상태인 {@link UserPet} 목록을 페이징 조회합니다.</li>
     * <li>조회된 Entity Page를 Map에 담아 반환합니다.</li>
     * </ol>
     *
     * @param userId   조회할 사용자의 UUID
     * @param pageable 페이징 정보
     * @return "pendingPetPage" 키에 {@code Page<UserPet>} 엔티티가 담긴 Map 객체
     */
    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> getMyPendingPets(UUID userId, Pageable pageable, String status) {

        Page<UserPet> pendingPetPage = userPetRepository.findAllByUser_UsersIdAndRegistrationStatusIn(
                userId,
                resolveApplicationStatuses(status),
                pageable
        );

        Map<String, Object> result = new HashMap<>();
        result.put("pendingPetPage", pendingPetPage);

        return result;
    }

    private List<RegistrationStatus> resolveApplicationStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of(RegistrationStatus.PENDING, RegistrationStatus.REJECTED);
        }

        try {
            RegistrationStatus registrationStatus = RegistrationStatus.valueOf(status.trim().toUpperCase());
            if (registrationStatus == RegistrationStatus.PENDING || registrationStatus == RegistrationStatus.REJECTED) {
                return List.of(registrationStatus);
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to the common bad request response.
        }

        throw new UserPetException(INVALID_REQUEST);
    }

    /**
     * 유저가 해당 펫의 소유자인지(APPROVED 상태인지) 검증합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public boolean isUserPetOwner(UUID userId, Long petId) {
        return userPetRepository.existsByUser_UsersIdAndPet_PetIdAndRegistrationStatus(
                userId,
                petId,
                RegistrationStatus.APPROVED
        );
    }

    /**
     * 특정 유저와 펫의 연결 관계(UserPet)를 삭제합니다. (가족 나가기)
     * <p>
     * 해당 유저가 가족 구성원 목록에서 제거되며, 더 이상 해당 펫에 접근할 수 없게 됩니다.
     *
     * @param userId 삭제할 유저 ID
     * @param petId  삭제할 펫 ID
     * @throws UserPetException 해당 관계가 존재하지 않을 경우 (NOT_FAMILY_MEMBER)
     */
    @Transactional
    @Override
    public void deleteUserPetRelation(UUID userId, Long petId) {
        UserPet userPet = userPetRepository.findByUser_UsersIdAndPet_PetId(userId, petId)
                .orElseThrow(() -> new UserPetException(PET_NOT_FOUND));

        userPetRepository.delete(userPet);
        log.info("UserPet 관계 삭제 완료 (가족 나가기) - User: {}, Pet: {}", userId, petId);
    }

    /**
     * 해당 펫에 등록된 가족(APPROVED 상태 등)이 한 명이라도 남아있는지 확인합니다.
     * <p>
     * 주로 펫의 마지막 가족이 탈퇴했을 때, 펫 정보를 완전히 삭제할지 결정하기 위해 사용됩니다.
     *
     * @param petId 확인할 펫 ID
     * @return 가족이 남아있다면 true, 아무도 없다면 false
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsFamilyMember(Long petId) {
        return userPetRepository.existsByPet_PetId(petId);
    }

    /**
     * ID 기반으로 해당 유저가 특정 펫의 '승인된(APPROVED)' 주인인지 효율적으로 검증합니다.
     * <p>
     * 엔티티 조회 없이 ID만으로 존재 여부를 확인하므로 성능상 이점이 있습니다.
     * 주로 다른 도메인 서비스(ActivityHistory 등)에서 권한 체크 용도로 호출합니다.
     * </p>
     *
     * @param userId 확인할 유저 ID
     * @param petId  확인할 펫 ID
     * @return 승인된 주인이라면 true, 그렇지 않다면 false
     */
    @Transactional(readOnly = true)
    @Override
    public boolean isApprovedPetOwner(UUID userId, Long petId) {
        return userPetRepository.existsByUser_UsersIdAndPet_PetIdAndRegistrationStatus(
                userId,
                petId,
                RegistrationStatus.APPROVED
        );
    }

    /**
     * 유저의 존재 여부를 확인합니다.
     * <p>
     * 단순히 존재 여부만 반환하며, 예외를 발생시키지 않습니다.
     * 호출하는 쪽에서 true/false에 따라 예외 처리를 수행해야 합니다.
     *
     * @param userId 검증할 유저의 UUID
     * @return 유저가 존재하면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsUser(UUID userId) {
        return userRepository.existsById(userId);
    }

    /**
     * 요청 사용자의 승인된(APPROVED) 반려동물 ID 목록을 조회합니다.
     * <p>
     * 사용자-반려동물 연결 테이블에서 승인 상태만 필터링하여
     * 반려동물 식별자 목록을 반환합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 승인된 반려동물 ID 목록
     */
    @Transactional(readOnly = true)
    @Override
    public List<Long> getApprovedPetIds(UUID userId) {
        return userPetRepository.findPetIdsByUserIdAndRegistrationStatus(userId, RegistrationStatus.APPROVED);
    }

    /**
     * 특정 반려동물의 승인된 가족 구성원 목록을 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @return 승인된 가족 구성원 목록
     */
    @Transactional(readOnly = true)
    @Override
    public List<UserPet> getApprovedFamilyMembers(Long petId) {
        return userPetRepository.findAllByPet_PetIdAndRegistrationStatus(petId, RegistrationStatus.APPROVED);
    }

}
