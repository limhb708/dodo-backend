package com.dodo.backend.pet.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.dto.request.PetRequest.PetDeviceCheckRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetDeviceUpdateRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetFamilyJoinRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetSignificantCreateRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetSignificantUpdateRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse.*;
import com.dodo.backend.pet.dto.response.PetResponse.BlockedUserListResponse.BlockedUserResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PendingUserListResponse.PendingUserResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetApplicationListResponse.PetApplicationResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetListResponse.PetSummary;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.pet.mapper.PetMapper;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.petweight.service.PetWeightService;
import com.dodo.backend.petspecialnote.entity.PetSpecialNote;
import com.dodo.backend.petspecialnote.service.PetSpecialNoteService;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static com.dodo.backend.pet.exception.PetErrorCode.*;


/**
 * {@link PetService}의 구현체로, 펫 도메인의 비즈니스 로직을 수행합니다.
 * <p>
 * 펫 정보의 유효성 검사, 엔티티 생성 및 저장, 사용자-펫 관계 설정 등의
 * 트랜잭션 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {
    private static final Set<String> ALLOWED_NOTE_TYPES = Set.of(
            "HOSPITAL", "MEDICATION", "ALLERGY", "FOOD", "BEHAVIOR", "SYMPTOM", "ETC"
    );

    private final PetRepository petRepository;
    private final UserPetService userPetService;
    private final PetWeightService petWeightService;
    private final PetMapper petMapper;
    private final ImageFileService imageFileService;
    private final PetSpecialNoteService petSpecialNoteService;
    private final ActivityHistoryRepository activityHistoryRepository;

    /**
     * 사용자의 요청 정보를 기반으로 반려동물을 등록하고, 소유자 관계를 설정합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService#existsUser}를 호출하여 사용자 존재 여부를 확인합니다.</li>
     * <li>존재하지 않는 경우 {@link PetException} (USER_NOT_FOUND)을 발생시킵니다.</li>
     * <li>요청된 등록번호가 이미 존재하는지 중복 여부를 확인합니다.</li>
     * <li>요청 DTO를 {@link Pet} 엔티티로 변환하여 데이터베이스에 저장합니다.</li>
     * <li>{@link UserPetService}를 호출하여 등록한 사용자를 해당 펫의 소유자(APPROVED)로 설정합니다.</li>
     * </ol>
     */
    @Transactional
    @Override
    public PetRegisterResponse registerPet(UUID userId, PetRegisterRequest request) {

        log.info("반려동물 등록 시작 - userId: {}", userId);

        if (!userPetService.existsUser(userId)) {
            throw new PetException(USER_NOT_FOUND);
        }

        if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
            if (petRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                throw new PetException(REGISTRATION_NUMBER_DUPLICATED);
            }
        }

        if (petRepository.existsByDeviceId(request.getDeviceId())) {
            throw new PetException(DEVICE_ID_DUPLICATED);
        }

        Pet pet = request.toEntity();
        Pet savedPet = petRepository.save(pet);

        imageFileService.savePetProfileImage(savedPet, request.getImageFileUrl());

        userPetService.registerUserPet(userId, savedPet, RegistrationStatus.APPROVED);

        log.info("펫 등록 및 유저 관계 설정 완료 - User: {}, PetId: {}", userId, savedPet.getPetId());

        return PetRegisterResponse.toDto(savedPet.getPetId(), "새 반려동물을 등록 완료했습니다.");
    }

    /**
     * 기존 반려동물의 프로필 정보를 수정합니다.
     * <p>
     * <ol>
     * <li>저장소(Repository)를 통해 수정할 반려동물 엔티티를 직접 조회합니다.</li>
     * <li>등록번호 변경 요청이 있는 경우, 해당 번호의 중복 여부를 검사합니다.</li>
     * <li>MyBatis Mapper를 호출하여 값이 존재하는 필드만 동적으로 업데이트(Dynamic Update)합니다.</li>
     * <li>수정 완료된 최신 정보를 포함한 응답 객체를 반환합니다.</li>
     * </ol>
     *
     * @param petId   수정할 반려동물의 ID
     * @param request 변경할 필드(이름, 나이, 체중 등)만 포함된 수정 요청 객체
     * @return 수정된 반려동물의 상세 정보를 담은 응답 객체
     * @throws PetException 해당 ID의 반려동물이 없거나(PET_NOT_FOUND), 변경하려는 등록번호가 이미 존재하는 경우
     */
    @Transactional
    @Override
    public PetUpdateResponse updatePet(Long petId, PetUpdateRequest request, UUID userId) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));

        boolean isOwner = userPetService.isUserPetOwner(userId, petId);

        if (!isOwner) {
            throw new PetException(UPDATE_PERMISSION_DENIED);
        }
        if (request.getRegistrationNumber() != null && !Objects.equals(pet.getRegistrationNumber(), request.getRegistrationNumber())) {
            if (petRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                log.warn("등록번호 중복 발생 - PetId: {}, 번호: {}", petId, request.getRegistrationNumber());
                throw new PetException(REGISTRATION_NUMBER_DUPLICATED);
            }
        }

        if (hasPetTableUpdateFields(request)) {
            petMapper.updatePetProfileInfo(request, petId);
        }
        imageFileService.updatePetProfileImage(pet, request.getImageFileUrl());

        log.info("반려동물 프로필 수정 성공 - PetId: {}", petId);

        return PetUpdateResponse.toDto(
                petId,
                "반려동물 정보 수정을 완료했습니다.",
                request.getRegistrationNumber() != null ? request.getRegistrationNumber() : pet.getRegistrationNumber(),
                request.getSex() != null ? request.getSex() : pet.getSex().name(),
                request.getAge() != null ? request.getAge() : pet.getAge(),
                request.getPetName() != null ? request.getPetName() : pet.getPetName(),
                request.getBreed() != null ? request.getBreed() : pet.getBreed(),
                request.getReferenceHeartRate() != null ? request.getReferenceHeartRate() : pet.getReferenceHeartRate(),
                request.getDeviceId() != null ? request.getDeviceId() : pet.getDeviceId()
        );
    }

    /**
     * 가족 초대를 위한 1회용 인증 코드를 생성합니다.
     * <p>
     * <ol>
     * <li>대상 반려동물의 존재 여부를 우선 검증합니다. (엔티티 조회 없이 존재 여부만 확인하여 성능 최적화)</li>
     * <li>실제 코드 생성 및 Redis 저장 로직은 {@link UserPetService}에 위임합니다.</li>
     * <li>생성된 6자리 코드와 유효 시간을 반환받아 응답 객체로 변환합니다.</li>
     * </ol>
     *
     * @param userId 코드를 요청한 사용자의 ID (권한 검증용)
     * @param petId  초대할 반려동물의 ID
     * @return 생성된 초대 코드와 만료 시간(초 단위)
     * @throws PetException 반려동물이 존재하지 않는 경우 (PET_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    @Override
    public PetInvitationResponse issueInvitationCode(UUID userId, Long petId) {

        if (!petRepository.existsById(petId)) {
            throw new PetException(PET_NOT_FOUND);
        }

        Map<String, Object> result = userPetService.generateInvitationCode(userId, petId);

        return PetInvitationResponse.builder()
                .message("초대 코드가 생성되었습니다.")
                .code((String) result.get("code"))
                .expiresIn((Long) result.get("expiresIn"))
                .build();
    }

    /**
     * 초대 코드를 입력하여 가족 등록을 신청(승인 대기)합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService#registerByInvitation}를 호출하여 코드 검증 및 PENDING 상태 등록을 수행합니다.</li>
     * <li>등록된 펫 ID를 반환받아 신청 성공 메시지와 함께 응답 객체로 변환합니다.</li>
     * </ol>
     *
     * @param userId  요청한 사용자의 ID
     * @param request 초대 코드가 포함된 요청 DTO
     * @return 신청된 펫 ID와 처리 결과 메시지
     */
    @Transactional
    @Override
    public PetFamilyJoinRequestResponse applyForFamily(UUID userId, PetFamilyJoinRequest request) {

        Long petId = userPetService.registerByInvitation(userId, request.getCode());

        return PetFamilyJoinRequestResponse.toDto(petId, "가족 등록을 신청했습니다. 승인을 기다려주세요.");
    }

    /**
     * 사용자의 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService}를 통해 페이징 된 펫 목록({@code Page<UserPet>})을 조회합니다.</li>
     * <li>조회된 목록에서 펫 ID들을 추출합니다.</li>
     * <li>추출한 ID로 {@link PetWeightService}를 호출하여 최신 체중 정보를 일괄 조회합니다.</li>
     * <li>추출한 ID로 이미지 이름(예: pet_profile_{id})을 생성하고 {@link ImageFileService}를 통해 이미지 URL을 일괄 조회합니다.</li>
     * <li>Entity 목록을 순회하며 체중 및 이미지 URL을 매핑하여 {@link PetSummary} DTO로 변환합니다.</li>
     * <li>최종적으로 페이징 정보가 포함된 응답 객체를 반환합니다.</li>
     * </ol>
     *
     * @param userId   조회할 사용자의 ID
     * @param pageable 페이징 요청 정보
     * @return 페이징 처리된 반려동물 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public PetListResponse getPetList(UUID userId, Pageable pageable) {

        Map<String, Object> result = userPetService.getUserPets(userId, pageable);
        Page<UserPet> userPetPage = (Page<UserPet>) result.get("userPetPage");

        List<Long> petIds = userPetPage.getContent().stream()
                .map(userPet -> userPet.getPet().getPetId())
                .collect(Collectors.toList());

        Map<Long, Double> weightMap = petWeightService.getRecentWeights(petIds);
        Map<Long, String> imageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        Page<PetSummary> summaryPage = userPetPage.map(userPet -> {
            Pet pet = userPet.getPet();

            Double recentWeight = weightMap.get(pet.getPetId());
            String imageUrl = imageMap.get(pet.getPetId());

            return PetSummary.builder()
                    .petId(pet.getPetId())
                    .petName(pet.getPetName())
                    .species(pet.getSpecies().name())
                    .breed(pet.getBreed())
                    .sex(pet.getSex().name())
                    .age(pet.getAge())
                    .birth(pet.getBirth())
                    .weight(recentWeight)
                    .registrationNumber(pet.getRegistrationNumber())
                    .imageFileUrl(imageUrl)
                    .build();
        });

        return PetListResponse.toDto(summaryPage, "조회를 성공했습니다.");
    }

    /**
     * 대기 중인 가족 등록 요청을 승인하거나 거절합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService#approveOrRejectFamilyMember}를 호출하여 실제 상태 변경 로직을 위임합니다.</li>
     * <li>처리 결과 메시지를 반환받아 DTO에 담아 응답합니다.</li>
     * </ol>
     *
     * @param userId  요청을 수행하는 관리자(기존 가족) ID
     * @param petId        반려동물 ID
     * @param targetUserId 승인/거절 대상 유저 ID
     * @param action       처리할 상태 문자열 ("APPROVED" 또는 "REJECTED")
     * @return 펫 ID와 처리 결과 메시지가 담긴 응답 DTO
     */
    @Transactional
    @Override
    public PetFamilyApprovalResponse manageFamily(UUID userId, Long petId, UUID targetUserId, String action) {

        String resultMessage = userPetService.approveOrRejectFamilyMember(userId, petId, targetUserId, action);

        return PetFamilyApprovalResponse.toDto(petId, resultMessage);
    }

    @Transactional
    @Override
    public PetFamilyApprovalResponse unblockFamily(UUID userId, Long petId, UUID targetUserId) {

        String resultMessage = userPetService.unblockFamilyMember(userId, petId, targetUserId);

        return PetFamilyApprovalResponse.toDto(petId, resultMessage);
    }

    /**
     * 내가 관리하는 모든 반려동물에게 들어온 가족 신청(대기자) 목록을 페이징하여 조회합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService}를 호출하여 내 펫들에 대한 PENDING 상태인 UserPet 엔티티 목록을 받아옵니다.</li>
     * <li>목록에서 펫 ID들을 추출하여 {@link ImageFileService}를 통해 프로필 이미지 URL을 일괄 조회합니다.</li>
     * <li>엔티티 목록을 순회하며 신청자 정보와 대상 펫 정보(이미지 포함)를 {@link PendingUserResponse} DTO로 변환합니다.</li>
     * <li>최종적으로 페이징 정보가 포함된 {@link PendingUserListResponse} 객체를 반환합니다.</li>
     * </ol>
     *
     * @param userId 요청을 수행하는 관리자(기존 가족)의 UUID
     * @param pageable  페이징 요청 정보
     * @return 페이징된 승인 대기자 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public PendingUserListResponse getAllPendingUsers(UUID userId, Pageable pageable, String status) {

        Map<String, Object> result = userPetService.getAllPendingUsers(userId, pageable, status);
        Page<UserPet> entityPage = (Page<UserPet>) result.get("pendingUserPage");

        List<Long> petIds = entityPage.getContent().stream()
                .map(userPet -> userPet.getPet().getPetId())
                .collect(Collectors.toList());

        Map<Long, String> imageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        Page<PendingUserResponse> dtoPage = entityPage.map(userPet ->
                PendingUserResponse.toDto(
                        userPet.getUser().getUsersId(),
                        userPet.getUser().getNickname(),
                        userPet.getUser().getProfileUrl(),
                        userPet.getPet().getPetId(),
                        userPet.getPet().getPetName(),
                        imageMap.get(userPet.getPet().getPetId()),
                        userPet.getRegistrationStatus().name(),
                        userPet.getRegistrationCreatedAt(),
                        getRejectedAt(userPet)
                )
        );

        return PendingUserListResponse.toDto(dtoPage, "조회를 성공했습니다.");
    }

    /**
     * 내가 관리하는 모든 반려동물의 차단된 가족 신청자 목록을 페이징하여 조회합니다.
     * <p>
     * {@link UserPetService}에서 BLOCKED 상태의 {@link UserPet} 목록을 받아오고,
     * 대상 펫 프로필 이미지 URL을 일괄 조회한 뒤 {@link BlockedUserResponse} DTO로 변환합니다.
     *
     * @param userId   요청을 수행하는 관리자(기존 가족)의 UUID
     * @param pageable 페이징 요청 정보
     * @return 페이징된 차단 유저 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public BlockedUserListResponse getAllBlockedUsers(UUID userId, Pageable pageable) {

        Map<String, Object> result = userPetService.getAllBlockedUsers(userId, pageable);
        Page<UserPet> entityPage = (Page<UserPet>) result.get("blockedUserPage");

        List<Long> petIds = entityPage.getContent().stream()
                .map(userPet -> userPet.getPet().getPetId())
                .collect(Collectors.toList());

        Map<Long, String> imageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        Page<BlockedUserResponse> dtoPage = entityPage.map(userPet ->
                BlockedUserResponse.toDto(
                        userPet.getUser().getUsersId(),
                        userPet.getUser().getNickname(),
                        userPet.getUser().getProfileUrl(),
                        userPet.getPet().getPetId(),
                        userPet.getPet().getPetName(),
                        imageMap.get(userPet.getPet().getPetId()),
                        userPet.getRegistrationUpdatedAt()
                )
        );

        return BlockedUserListResponse.toDto(dtoPage, "조회를 성공했습니다.");
    }

    /**
     * 내가 가족 신청을 보낸 후 대기 중인 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService}를 호출하여 내가 신청한(PENDING) UserPet 엔티티 목록을 Map 형태로 받아옵니다.</li>
     * <li>목록에서 펫 ID들을 추출하여 {@link ImageFileService}를 통해 프로필 이미지 URL을 일괄 조회합니다.</li>
     * <li>아직 가족이 아니므로 상세 정보 조회 없이, 펫의 기본 정보(ID, 이름, 이미지, 상태)만 {@link PetApplicationResponse} DTO로 변환합니다.</li>
     * <li>최종적으로 페이징 정보가 포함된 {@link PetApplicationListResponse} 객체를 반환합니다.</li>
     * </ol>
     *
     * @param userId   조회할 사용자의 UUID
     * @param pageable 페이징 요청 정보
     * @return 페이징된 신청 내역 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public PetApplicationListResponse getMyPendingApplications(UUID userId, Pageable pageable, String status) {

        Map<String, Object> result = userPetService.getMyPendingPets(userId, pageable, status);
        Page<UserPet> entityPage = (Page<UserPet>) result.get("pendingPetPage");

        List<Long> petIds = entityPage.getContent().stream()
                .map(userPet -> userPet.getPet().getPetId())
                .collect(Collectors.toList());

        Map<Long, String> imageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        Page<PetApplicationResponse> dtoPage = entityPage.map(userPet ->
                PetApplicationResponse.toDto(
                        userPet.getPet().getPetId(),
                        userPet.getPet().getPetName(),
                        imageMap.get(userPet.getPet().getPetId()),
                        userPet.getRegistrationStatus().name(),
                        userPet.getRegistrationCreatedAt(),
                        getRejectedAt(userPet)
                )
        );

        return PetApplicationListResponse.toDto(dtoPage, "조회를 성공했습니다.");
    }

    private LocalDateTime getRejectedAt(UserPet userPet) {
        if (userPet.getRegistrationStatus() != RegistrationStatus.REJECTED) {
            return null;
        }
        return userPet.getRegistrationUpdatedAt();
    }

    /**
     * 반려동물 상세 정보를 조회합니다.
     * <p>
     * <ol>
     * <li>반려동물 존재 여부를 검증합니다.</li>
     * <li>요청 사용자의 조회 권한(APPROVED 가족 여부)을 검증합니다.</li>
     * <li>반려동물 기본 정보, 이미지, 가족 구성원, 최근 활동, 특이사항, 체중 정보를 조합합니다.</li>
     * <li>조합된 정보를 상세 응답 DTO로 반환합니다.</li>
     * </ol>
     *
     * @param userId 요청한 사용자 ID
     * @param petId  조회할 반려동물 ID
     * @return 반려동물 상세 정보 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public PetDetailResponse getPetDetail(UUID userId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetException(VIEW_PERMISSION_DENIED);
        }

        String imageFileUrl = imageFileService.getProfileUrlsByPetIds(List.of(petId)).get(petId);

        List<PetDetailResponse.FamilyMember> familyMembers = userPetService.getApprovedFamilyMembers(petId).stream()
                .map(userPet -> PetDetailResponse.FamilyMember.builder()
                        .userId(userPet.getUser().getUsersId())
                        .userName(userPet.getUser().getName())
                        .profileImageUrl(userPet.getUser().getProfileUrl())
                        .build())
                .toList();

        PetDetailResponse.LastActivity lastActivity = activityHistoryRepository.findFirstByPet_PetIdOrderByHistoryIdDesc(petId)
                .map(activity -> PetDetailResponse.LastActivity.builder()
                        .activityId(activity.getHistoryId())
                        .activityType(activity.getActivityType().name())
                        .startTime(activity.getActivityHistoryStartAt())
                        .endTime(activity.getActivityHistoryEndAt())
                        .distance(activity.getDistance())
                        .build())
                .orElse(null);

        List<com.dodo.backend.petspecialnote.entity.PetSpecialNote> allSpecialNotes = petSpecialNoteService.getPetSpecialNotes(petId);

        List<PetDetailResponse.SpecialNote> specialNotes = allSpecialNotes.stream()
                .limit(3)
                .map(note -> PetDetailResponse.SpecialNote.builder()
                        .noteId(note.getNoteId())
                        .noteContent(note.getNoteContent())
                        .noteType(note.getNoteType().name())
                        .createdAt(note.getPetSpecialNotesCreatedAt())
                        .build())
                .toList();

        Map<String, Object> weightMap = petWeightService.getWeightInfo(petId);
        PetDetailResponse.WeightInfo weightInfo = PetDetailResponse.WeightInfo.builder()
                .currentWeight((Double) weightMap.get("currentWeight"))
                .weightTrend((String) weightMap.get("weightTrend"))
                .build();

        return PetDetailResponse.builder()
                .message("반려동물 정보 조회에 성공했습니다.")
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .imageFileUrl(imageFileUrl)
                .species(pet.getSpecies().name())
                .breed(pet.getBreed())
                .sex(pet.getSex().name())
                .age(pet.getAge())
                .birth(pet.getBirth())
                .registrationNumber(pet.getRegistrationNumber())
                .deviceId(pet.getDeviceId())
                .referenceHeartRate(pet.getReferenceHeartRate())
                .familyMembers(familyMembers)
                .lastActivity(lastActivity)
                .specialNotes(specialNotes)
                .specialNotesCount(allSpecialNotes.size())
                .weightInfo(weightInfo)
                .build();
    }

    /**
     * 반려동물 가족 그룹에서 탈퇴(삭제)합니다.
     * <p>
     * <ol>
     * <li>요청한 유저가 해당 펫의 가족(소유자)인지 검증합니다.</li>
     * <li>해당 유저와 펫의 연결 관계를 삭제합니다. (가족 나가기)</li>
     * <li>삭제 후, 해당 펫에 남은 가족이 있는지 확인합니다.</li>
     * <li>만약 남은 가족이 한 명도 없다면, 펫 정보(Pet) 자체를 DB에서 완전히 삭제합니다.</li>
     * </ol>
     *
     * @param userId 요청한 사용자의 ID
     * @param petId  삭제(탈퇴)할 반려동물 ID
     */
    @Transactional
    @Override
    public void deletePet(UUID userId, Long petId) {

        if (!petRepository.existsById(petId)) {
            throw new PetException(PET_NOT_FOUND);
        }

        if (!userPetService.isUserPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        userPetService.deleteUserPetRelation(userId, petId);

        if (!userPetService.existsFamilyMember(petId)) {

            petRepository.deleteById(petId);
            log.info("마지막 가족 탈퇴로 펫 정보가 완전 삭제되었습니다. PetId: {}", petId);
        } else {
            log.info("가족 그룹에서 탈퇴했습니다. (다른 가족이 남아있어 펫 정보 유지) User: {}, PetId: {}", userId, petId);
        }
    }

    /**
     * 반려동물의 디바이스를 재등록(수정)합니다.
     * <p>
     * 1. 권한 검증 (소유자 확인)
     * 2. 디바이스 ID 중복 확인 (이미 다른 펫이 사용 중인지)
     * 3. MyBatis를 통한 업데이트 수행
     * 4. DTO 반환 (엔티티 직접 참조 X)
     */
    @Transactional
    @Override
    public PetDeviceUpdateResponse updateDevice(UUID userId, Long petId, PetDeviceUpdateRequest request) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));

        if (!userPetService.isUserPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        String newDeviceId = request.getDeviceId();
        String oldDeviceId = pet.getDeviceId();

        if (!Objects.equals(oldDeviceId, newDeviceId)) {
            if (petRepository.existsByDeviceId(newDeviceId)) {
                throw new PetException(DEVICE_ID_DUPLICATED);
            }
        }

        petMapper.updatePetDevice(petId, newDeviceId);

        log.info("펫 디바이스 변경 완료 - PetId: {}, Old: {}, New: {}", petId, oldDeviceId, newDeviceId);

        return PetDeviceUpdateResponse.toDto(
                pet.getPetId(),
                pet.getPetName(),
                oldDeviceId,
                newDeviceId,
                "디바이스가 성공적으로 재등록되었습니다."
        );
    }

    /**
     * 디바이스 ID 중복 여부를 확인합니다.
     *
     * @param request 확인할 디바이스 ID가 포함된 요청 DTO
     * @return 디바이스 ID 사용 가능 여부 응답
     */
    @Transactional(readOnly = true)
    @Override
    public PetDeviceCheckResponse checkDeviceIdAvailability(PetDeviceCheckRequest request) {
        if (petRepository.existsByDeviceId(request.getDeviceId())) {
            return PetDeviceCheckResponse.toDto("이미 다른 반려동물에 등록된 디바이스 ID입니다.", false);
        }

        return PetDeviceCheckResponse.toDto("사용 가능한 디바이스 ID입니다.", true);
    }

    /**
     * {@code pet} 테이블에 반영할 필드가 있는지 확인합니다.
     * <p>
     * {@code imageFileUrl}은 {@code image_file} 도메인에서 별도로 처리하므로
     * 이 검사 대상에 포함하지 않습니다. 이미지 URL만 수정하는 요청에서
     * {@link PetMapper#updatePetProfileInfo}를 호출하면 MyBatis의 동적
     * {@code <set>}에 들어갈 컬럼이 없어 잘못된 SQL이 생성될 수 있습니다.
     *
     * @param request 반려동물 수정 요청 DTO
     * @return {@code pet} 테이블 업데이트 대상 필드가 하나 이상 있으면 true
     */
    private boolean hasPetTableUpdateFields(PetUpdateRequest request) {
        return request.getRegistrationNumber() != null
                || request.getSex() != null
                || request.getAge() != null
                || request.getPetName() != null
                || request.getBreed() != null
                || request.getReferenceHeartRate() != null
                || request.getDeviceId() != null;
    }

    /**
     * 반려동물 특이사항을 생성합니다.
     * <p>
     * <ol>
     * <li>요청된 반려동물의 존재 여부를 검증합니다.</li>
     * <li>요청 사용자가 해당 반려동물의 승인된 보호자인지 검증합니다.</li>
     * <li>요청 문자열(noteType)을 그대로 전달하여 특이사항 저장을 수행합니다.</li>
     * <li>특이사항 저장을 특이사항 도메인 서비스에 위임합니다.</li>
     * </ol>
     *
     * @param userId  요청한 사용자 ID
     * @param request 특이사항 생성 요청 DTO
     * @return 특이사항 생성 결과 응답 DTO
     */
    @Transactional
    @Override
    public PetSignificantCreateResponse createPetSignificant(UUID userId, PetSignificantCreateRequest request) {
        Long petId = request.getPetId();

        if (!petRepository.existsById(petId)) {
            throw new PetException(PET_NOT_FOUND);
        }

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        String noteType = request.getNoteType();
        if (noteType == null || noteType.isBlank()) {
            throw new PetException(INVALID_REQUEST);
        }
        String normalizedNoteType = noteType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_NOTE_TYPES.contains(normalizedNoteType)) {
            throw new PetException(INVALID_REQUEST);
        }

        Pet pet = petRepository.findById(petId).orElseThrow(() -> new PetException(PET_NOT_FOUND));
        Long noteId = petSpecialNoteService.createPetSpecialNote(pet, request.getNoteContent(), normalizedNoteType);

        return PetSignificantCreateResponse.toDto("펫 특이사항 등록을 완료했습니다.", noteId);
    }

    /**
     * 반려동물 특이사항을 수정합니다.
     * <p>
     * <ol>
     * <li>특이사항 존재 여부를 검증합니다.</li>
     * <li>요청 사용자가 해당 반려동물의 승인된 보호자인지 검증합니다.</li>
     * <li>수정 요청값(noteContent, noteType)의 유효성을 검증합니다.</li>
     * <li>동적 업데이트(Mapper)를 호출하여 변경 항목만 수정합니다.</li>
     * </ol>
     *
     * @param userId  요청한 사용자 ID
     * @param noteId  수정할 특이사항 ID
     * @param request 특이사항 수정 요청 DTO
     * @return 특이사항 수정 결과 응답 DTO
     */
    @Transactional
    @Override
    public PetSignificantUpdateResponse updatePetSignificant(UUID userId, Long noteId, PetSignificantUpdateRequest request) {
        PetSpecialNote note = petSpecialNoteService.findPetSpecialNoteById(noteId)
                .orElseThrow(() -> new PetException(PET_SIGNIFICANT_NOT_FOUND));

        Long petId = note.getPet().getPetId();
        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        if (request.getNoteContent() == null && request.getNoteType() == null) {
            throw new PetException(INVALID_REQUEST);
        }

        if (request.getNoteContent() != null && request.getNoteContent().isBlank()) {
            throw new PetException(INVALID_REQUEST);
        }

        String updatedContent = request.getNoteContent();
        String updatedType = null;
        if (request.getNoteType() != null) {
            if (request.getNoteType().isBlank()) {
                throw new PetException(INVALID_REQUEST);
            }
            updatedType = request.getNoteType().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_NOTE_TYPES.contains(updatedType)) {
                throw new PetException(INVALID_REQUEST);
            }
        }

        petSpecialNoteService.updatePetSpecialNote(noteId, updatedContent, updatedType);
        return PetSignificantUpdateResponse.toDto("펫 특이사항 수정을 완료했습니다.", noteId);
    }

    /**
     * 반려동물 특이사항을 삭제합니다.
     * <p>
     * <ol>
     * <li>특이사항 존재 여부를 검증합니다.</li>
     * <li>요청 사용자가 해당 반려동물의 승인된 보호자인지 검증합니다.</li>
     * <li>특이사항을 삭제하고 삭제 결과를 반환합니다.</li>
     * </ol>
     *
     * @param userId 요청한 사용자 ID
     * @param noteId 삭제할 특이사항 ID
     * @return 특이사항 삭제 결과 응답 DTO
     */
    @Transactional
    @Override
    public PetSignificantDeleteResponse deletePetSignificant(UUID userId, Long noteId) {
        PetSpecialNote note = petSpecialNoteService.findPetSpecialNoteById(noteId)
                .orElseThrow(() -> new PetException(PET_SIGNIFICANT_NOT_FOUND));

        Long petId = note.getPet().getPetId();
        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        petSpecialNoteService.deletePetSpecialNote(noteId);
        return PetSignificantDeleteResponse.toDto("펫 특이사항 삭제를 완료했습니다.", noteId);
    }

    /**
     * 반려동물 특이사항 목록을 페이지네이션으로 조회합니다.
     * <p>
     * <ol>
     * <li>반려동물 존재 여부를 검증합니다.</li>
     * <li>요청 사용자의 반려동물 접근 권한을 검증합니다.</li>
     * <li>요청 정렬 문자열을 파싱하여 내부 엔티티 필드명으로 매핑합니다.</li>
     * <li>특이사항 페이지 데이터를 조회하고 응답 DTO로 변환합니다.</li>
     * </ol>
     *
     * @param userId 요청한 사용자 ID
     * @param petId 조회할 반려동물 ID
     * @param page 페이지 번호(0부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 조건 문자열(property,direction)
     * @return 특이사항 목록 페이징 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public PetSignificantListResponse getPetSignificantList(UUID userId, Long petId, int page, int size, String sort) {
        if (!petRepository.existsById(petId)) {
            throw new PetException(PET_NOT_FOUND);
        }

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        String sortValue = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort.trim();
        String[] split = sortValue.split(",");
        String requestedProperty = split[0].trim();
        String requestedDirection = split.length > 1 ? split[1].trim() : "desc";

        String mappedProperty = switch (requestedProperty) {
            case "createdAt" -> "petSpecialNotesCreatedAt";
            case "noteType" -> "noteType";
            case "noteId" -> "noteId";
            default -> throw new PetException(INVALID_REQUEST);
        };

        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(requestedDirection)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(requestedDirection)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new PetException(INVALID_REQUEST);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, mappedProperty));
        Page<PetSpecialNote> notePage = petSpecialNoteService.getPetSpecialNotes(petId, pageable);

        Page<PetSignificantListResponse.NoteSummary> dtoPage = notePage.map(note ->
                PetSignificantListResponse.NoteSummary.builder()
                        .noteId(note.getNoteId())
                        .noteContent(note.getNoteContent())
                        .noteType(note.getNoteType().name())
                        .createdAt(note.getPetSpecialNotesCreatedAt())
                        .build()
        );

        return PetSignificantListResponse.toDto(dtoPage, "펫 특이사항 목록 조회를 완료했습니다.");
    }

    /**
     * 디바이스 ID로 등록된 반려동물 ID를 조회합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<Long> findPetIdByDeviceId(String deviceId) {
        return petRepository.findByDeviceId(deviceId)
                .map(Pet::getPetId);
    }

    /**
     * ID로 펫 엔티티를 조회합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public Pet getPetById(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));
    }

    /**
     * 반려동물 ID 존재 여부를 확인합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsPetById(Long petId) {
        return petRepository.existsById(petId);
    }

    /**
     * 특정 반려동물의 기준 심박수(Reference Heart Rate)를 조회합니다.
     * <p>
     * 부정맥 판별이나 건강 상태 모니터링 시 비교 기준으로 사용됩니다.
     * 설정된 값이 없는 경우 {@code null}을 반환합니다.
     *
     * @param petId 조회할 반려동물의 ID
     * @return 기준 심박수 (BPM), 설정되지 않았으면 null
     */
    @Transactional(readOnly = true)
    @Override
    public Integer getAverageHeartRate(Long petId) {
        return petRepository.findById(petId)
                .map(Pet::getReferenceHeartRate)
                .orElse(null);
    }

    /**
     * 디바이스 토큰 Principal과 반려동물 ID의 매핑 일치 여부를 검증합니다.
     * <p>
     * 서버에서 사용하는 규칙 {@code DEVICE:{petId}}를 기반으로 UUID를 생성하여
     * 전달된 principalName과 비교합니다.
     *
     * @param principalName 인증 주체 문자열(UUID)
     * @param petId         반려동물 ID
     * @return 매핑이 일치하면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    @Override
    public boolean isDevicePrincipalMatchedPet(String principalName, Long petId) {
        if (principalName == null || petId == null) {
            return false;
        }

        UUID expectedDevicePrincipal = UUID.nameUUIDFromBytes(("DEVICE:" + petId).getBytes(StandardCharsets.UTF_8));
        return expectedDevicePrincipal.toString().equals(principalName);
    }
}
