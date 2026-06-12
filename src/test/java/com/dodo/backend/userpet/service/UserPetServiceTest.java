package com.dodo.backend.userpet.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import com.dodo.backend.userpet.exception.UserPetErrorCode;
import com.dodo.backend.userpet.exception.UserPetException;
import com.dodo.backend.userpet.mapper.UserPetMapper;
import com.dodo.backend.userpet.repository.UserPetRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link UserPetService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class UserPetServiceTest {

    @InjectMocks
    private UserPetServiceImpl userPetService;

    @Mock
    private UserPetRepository userPetRepository;

    @Mock
    private UserPetMapper userPetMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    /**
     * UserPet 관계 등록 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("UserPet 등록 성공: 유저와 펫의 관계 엔티티가 올바르게 생성되어 저장된다.")
    void registerUserPet_Success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "usersId", userId);

        Pet pet = Pet.builder().build();
        ReflectionTestUtils.setField(pet, "petId", 100L);

        RegistrationStatus status = RegistrationStatus.APPROVED;

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userPetService.registerUserPet(userId, pet, status);

        // then
        ArgumentCaptor<UserPet> captor = ArgumentCaptor.forClass(UserPet.class);
        verify(userPetRepository).save(captor.capture());

        UserPet savedUserPet = captor.getValue();
        assertEquals(user, savedUserPet.getUser());
        assertEquals(pet, savedUserPet.getPet());
        assertEquals(status, savedUserPet.getRegistrationStatus());
    }

    /**
     * 가족 초대 코드 발급 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 성공: 승인된 멤버라면 코드가 생성되고 Redis에 저장된다.")
    void generateInvitationCode_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 100L;
        UserPet userPet = UserPet.builder()
                .registrationStatus(RegistrationStatus.APPROVED)
                .build();

        given(userPetRepository.findById(any(UserPetId.class))).willReturn(Optional.of(userPet));
        given(redisTemplate.hasKey(anyString())).willReturn(false);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        Map<String, Object> result = userPetService.generateInvitationCode(userId, petId);

        // then
        assertNotNull(result.get("code"));
        assertNotNull(result.get("expiresIn"));

        verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));
    }

    /**
     * 초대 코드 입력 및 가족 신청(대기) 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("초대 수락 성공: 유효한 코드 입력 시 대기(PENDING) 상태로 등록하고 펫 ID를 반환한다.")
    void registerByInvitation_Success() {
        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        String petIdStr = "100";
        Long petId = 100L;

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "usersId", userId);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(petIdStr);

        given(userPetRepository.findById(any(UserPetId.class))).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        Long resultPetId = userPetService.registerByInvitation(userId, invitationCode);

        // then
        assertEquals(petId, resultPetId);

        ArgumentCaptor<UserPet> captor = ArgumentCaptor.forClass(UserPet.class);
        verify(userPetRepository).save(captor.capture());
        assertEquals(RegistrationStatus.PENDING, captor.getValue().getRegistrationStatus());
    }

    @Test
    @DisplayName("초대 수락 실패: 이전 신청이 거절(REJECTED)된 후 15분이 지나지 않은 경우 남은 시간 메시지 에러를 반환한다.")
    void registerByInvitation_Fail_RejectedRequestInCooldown() {
        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        Long petId = 100L;
        UserPet rejectedUserPet = UserPet.builder()
                .registrationStatus(RegistrationStatus.REJECTED)
                .registrationUpdatedAt(LocalDateTime.now())
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(String.valueOf(petId));
        given(userPetRepository.findById(new UserPetId(userId, petId))).willReturn(Optional.of(rejectedUserPet));

        // when
        UserPetException exception = assertThrows(
                UserPetException.class,
                () -> userPetService.registerByInvitation(userId, invitationCode)
        );

        // then
        assertEquals(UserPetErrorCode.FAMILY_REQUEST_REJECTED, exception.getErrorCode());
        assertTrue(exception.getCustomMessage().contains("분 후 다시 신청해주세요."));
    }

    @Test
    @DisplayName("초대 수락 성공: 이전 신청이 거절(REJECTED)된 후 15분이 지난 경우 대기(PENDING) 상태로 재신청한다.")
    void registerByInvitation_Success_RejectedRequestAfterCooldown() {
        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        Long petId = 100L;
        UserPet rejectedUserPet = UserPet.builder()
                .registrationStatus(RegistrationStatus.REJECTED)
                .registrationUpdatedAt(LocalDateTime.now().minusMinutes(16))
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(String.valueOf(petId));
        given(userPetRepository.findById(new UserPetId(userId, petId))).willReturn(Optional.of(rejectedUserPet));

        // when
        Long resultPetId = userPetService.registerByInvitation(userId, invitationCode);

        // then
        assertEquals(petId, resultPetId);
        verify(userPetMapper).updateRegistrationStatus(userId, petId, RegistrationStatus.PENDING.name());
    }

    @Test
    @DisplayName("초대 수락 실패: 이미 신청 대기(PENDING) 중인 경우 대기 중 메시지 에러를 반환한다.")
    void registerByInvitation_Fail_PendingRequest() {
        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        Long petId = 100L;
        UserPet pendingUserPet = UserPet.builder()
                .registrationStatus(RegistrationStatus.PENDING)
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(String.valueOf(petId));
        given(userPetRepository.findById(new UserPetId(userId, petId))).willReturn(Optional.of(pendingUserPet));

        // when
        UserPetException exception = assertThrows(
                UserPetException.class,
                () -> userPetService.registerByInvitation(userId, invitationCode)
        );

        // then
        assertEquals(UserPetErrorCode.FAMILY_REQUEST_PENDING, exception.getErrorCode());
    }

    /**
     * 관리자가 소유한 모든 펫의 대기자 목록 조회 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("전체 대기자 조회 성공: 내가 관리하는 펫들에 대한 요청만 페이징되어 반환된다.")
    void getAllPendingUsers_Success() {
        // given
        UUID managerId = UUID.randomUUID();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        org.springframework.data.domain.Page<UserPet> mockPage =
                new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, 0);
        List<RegistrationStatus> statuses = List.of(RegistrationStatus.PENDING, RegistrationStatus.REJECTED);

        given(userPetRepository.findAllPendingRequestsByManager(managerId, statuses, pageable))
                .willReturn(mockPage);

        // when
        Map<String, Object> result = userPetService.getAllPendingUsers(managerId, pageable, null);

        // then
        assertNotNull(result);
        assertTrue(result.containsKey("pendingUserPage"));
        assertEquals(mockPage, result.get("pendingUserPage"));

        verify(userPetRepository).findAllPendingRequestsByManager(managerId, statuses, pageable);
    }

    @Test
    @DisplayName("전체 차단 유저 조회 성공: 내가 관리하는 펫들에 대한 차단 목록만 페이징되어 반환된다.")
    void getAllBlockedUsers_Success() {
        // given
        UUID managerId = UUID.randomUUID();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        org.springframework.data.domain.Page<UserPet> mockPage =
                new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), pageable, 0);

        given(userPetRepository.findAllBlockedRequestsByManager(managerId, pageable))
                .willReturn(mockPage);

        // when
        Map<String, Object> result = userPetService.getAllBlockedUsers(managerId, pageable);

        // then
        assertNotNull(result);
        assertTrue(result.containsKey("blockedUserPage"));
        assertEquals(mockPage, result.get("blockedUserPage"));

        verify(userPetRepository).findAllBlockedRequestsByManager(managerId, pageable);
    }

    /**
     * 가족 승인 요청 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("가족 요청 처리 성공: 승인(APPROVED) 요청 시 Mapper를 통해 상태가 업데이트된다.")
    void approveFamilyMember_Success() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Long petId = 100L;
        String action = "APPROVED";

        UserPet requester = UserPet.builder().registrationStatus(RegistrationStatus.APPROVED).build();
        UserPet target = UserPet.builder().registrationStatus(RegistrationStatus.PENDING).build();

        given(userPetRepository.findById(new UserPetId(requesterId, petId))).willReturn(Optional.of(requester));
        given(userPetRepository.findById(new UserPetId(targetUserId, petId))).willReturn(Optional.of(target));

        // when
        String result = userPetService.approveOrRejectFamilyMember(requesterId, petId, targetUserId, action);

        // then
        assertEquals("가족 신청을 승인했습니다.", result);
        verify(userPetMapper).updateRegistrationStatus(targetUserId, petId, "APPROVED");
    }

    @Test
    @DisplayName("가족 신청 차단 해제 성공: 요청자가 승인된 가족이고 대상이 차단 상태이면 관계를 삭제한다.")
    void unblockFamilyMember_Success() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Long petId = 100L;

        UserPet requester = UserPet.builder().registrationStatus(RegistrationStatus.APPROVED).build();
        UserPet blockedTarget = UserPet.builder().registrationStatus(RegistrationStatus.BLOCKED).build();

        given(userPetRepository.findById(new UserPetId(requesterId, petId))).willReturn(Optional.of(requester));
        given(userPetRepository.findById(new UserPetId(targetUserId, petId))).willReturn(Optional.of(blockedTarget));

        // when
        String result = userPetService.unblockFamilyMember(requesterId, petId, targetUserId);

        // then
        assertEquals("가족 신청 차단을 해제했습니다.", result);
        verify(userPetRepository).delete(blockedTarget);
    }
}
