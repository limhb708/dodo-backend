package com.dodo.backend.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 펫 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "펫 관련 응답 DTO 그룹")
public class PetResponse {

    /**
     * 펫 등록 처리가 성공적으로 완료되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "펫 등록 완료 응답")
    public static class PetRegisterResponse {

        @Schema(description = "응답 메시지", example = "새 반려동물을 등록완료했습니다.")
        private String message;

        @Schema(description = "생성된 펫 ID", example = "1")
        private Long petId;

        /**
         * 펫 ID와 메시지를 사용하여 응답 DTO 객체를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param petId   생성된 펫의 고유 식별자
         * @param message 클라이언트에게 전달할 성공 메시지
         * @return 초기화된 {@link PetRegisterResponse} 객체
         */
        public static PetRegisterResponse toDto(Long petId, String message) {
            return PetRegisterResponse.builder()
                    .message(message)
                    .petId(petId)
                    .build();
        }
    }

    /**
     * 반려동물 정보 수정(Update)이 완료된 후, 변경된 최신 정보를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 정보 수정 결과 응답")
    public static class PetUpdateResponse {

        @Schema(description = "응답 메시지", example = "반려동물 정보 수정을 완료했습니다.")
        private String message;

        @Schema(description = "반려동물 고유 ID", example = "1")
        private Long petId;

        @Schema(description = "변경된 등록번호", example = "123456789012345", nullable = true)
        private String registrationNumber;

        @Schema(description = "변경된 성별 (MALE, FEMALE, NEUTER)", example = "MALE")
        private String sex;

        @Schema(description = "변경된 나이", example = "4")
        private Integer age;

        @Schema(description = "생년월일", example = "2022-01-01T00:00:00")
        private LocalDateTime birth;

        @Schema(description = "변경된 펫 이름", example = "초코")
        private String petName;

        @Schema(description = "종 (CANINE, FELINE)", example = "CANINE")
        private String species;

        @Schema(description = "변경된 품종", example = "푸들")
        private String breed;

        @Schema(description = "변경된 심박수 기준치", example = "85")
        private Integer referenceHeartRate;

        @Schema(description = "변경된 디바이스 ID", example = "NEW-DEV-999")
        private String deviceId;

        /**
         * 수정된 필드값들을 직접 전달받아 수정 완료 응답 DTO로 변환합니다.
         *
         * @param petId              수정된 반려동물 고유 식별자
         * @param message            응답 메시지
         * @param registrationNumber 변경된 등록번호
         * @param sex                변경된 성별 (String)
         * @param age                변경된 나이
         * @param petName            변경된 이름
         * @param breed              변경된 품종
         * @param referenceHeartRate 변경된 심박수 기준
         * @param deviceId           변경된 디바이스 ID
         * @return 수정 완료 정보가 담긴 {@link PetUpdateResponse} DTO
         */
        public static PetUpdateResponse toDto(Long petId, String message, String registrationNumber,
                                              String sex, Integer age, String petName,
                                              String breed, Integer referenceHeartRate, String deviceId) {
            return PetUpdateResponse.builder()
                    .message(message)
                    .petId(petId)
                    .registrationNumber(registrationNumber)
                    .sex(sex)
                    .age(age)
                    .petName(petName)
                    .breed(breed)
                    .referenceHeartRate(referenceHeartRate)
                    .deviceId(deviceId)
                    .build();
        }
    }

    /**
     * 반려동물 가족 초대를 위한 코드 발급 요청 시 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 가족 초대 코드 응답")
    public static class PetInvitationResponse {

        @Schema(description = "응답 메시지", example = "초대 코드가 생성되었습니다.")
        private String message;

        @Schema(description = "생성된 가족 초대 코드 (영문 대문자 + 숫자, 총 6자리)", example = "7X9K2P")
        private String code;

        @Schema(description = "초대 코드 유효 시간 (초 단위)", example = "900")
        private Long expiresIn;
    }

    /**
     * 가족 초대 코드를 통해 등록 신청을 완료했을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "가족 초대 신청 결과 응답")
    public static class PetFamilyJoinRequestResponse {

        @Schema(description = "신청한 반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "응답 메시지", example = "가족 등록을 신청했으니 승인을 기다려주세요.")
        private String message;

        /**
         * 펫 ID와 메시지를 받아 응답 DTO를 생성합니다.
         *
         * @param petId   신청된 반려동물 ID
         * @param message 처리 결과 메시지
         * @return 초기화된 {@link PetFamilyJoinRequestResponse} 객체
         */
        public static PetFamilyJoinRequestResponse toDto(Long petId, String message) {
            return PetFamilyJoinRequestResponse.builder()
                    .petId(petId)
                    .message(message)
                    .build();
        }
    }

    /**
     * 반려동물 목록 조회 시 반환되는 페이징 된 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 목록 조회 응답 (페이징 포함)")
    public static class PetListResponse {

        @Schema(description = "응답 메시지", example = "조회를 성공했습니다.")
        private String message;

        @Schema(description = "반려동물 데이터 목록")
        private List<PetSummary> pets;

        @Schema(description = "총 페이지 수", example = "5")
        private int totalPages;

        @Schema(description = "총 데이터 수", example = "48")
        private long totalElements;

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        /**
         * 서비스 계층에서 변환된 {@code Page<PetSummary>} 객체를 받아 최종 응답 DTO를 생성합니다.
         * <p>
         * 이 메소드는 엔티티를 직접 참조하지 않으며, 이미 DTO로 변환된 데이터만을 다룹니다.
         *
         * @param petPage 반려동물 요약 정보(DTO)가 담긴 Page 객체
         * @return 페이징 정보와 데이터가 포함된 {@link PetListResponse}
         */
        public static PetListResponse toDto(Page<PetSummary> petPage, String message) {
            return PetListResponse.builder()
                    .message(message)
                    .pets(petPage.getContent())
                    .totalPages(petPage.getTotalPages())
                    .totalElements(petPage.getTotalElements())
                    .currentPage(petPage.getNumber())
                    .pageSize(petPage.getSize())
                    .build();
        }

        /**
         * 목록 내 개별 반려동물 요약 정보 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "반려동물 요약 정보")
        public static class PetSummary {
            @Schema(description = "반려동물 ID", example = "101")
            private Long petId;

            @Schema(description = "이름", example = "보리")
            private String petName;

            @Schema(description = "이미지 URL", example = "https://example.com/images/bori.jpg")
            private String imageFileUrl;

            @Schema(description = "종 (CANINE, FELINE)", example = "CANINE")
            private String species;

            @Schema(description = "품종", example = "말티즈")
            private String breed;

            @Schema(description = "성별", example = "FEMALE")
            private String sex;

            @Schema(description = "나이", example = "5")
            private Integer age;

            @Schema(description = "생년월일", example = "2020-09-30")
            private LocalDateTime birth;

            @Schema(description = "체중", example = "4.2")
            private Double weight;

            @Schema(description = "등록번호", example = "4102020001231")
            private String registrationNumber;
        }
    }

    /**
     * 가족 승인/거절 처리가 완료되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "가족 승인/거절 처리 결과 응답")
    public static class PetFamilyApprovalResponse {

        @Schema(description = "대상 반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "처리 결과 메시지", example = "가족 신청을 승인했습니다.")
        private String message;

        /**
         * 펫 ID와 처리 메시지를 받아 응답 DTO를 생성합니다.
         *
         * @param petId   처리된 반려동물 ID
         * @param message 처리 결과 메시지 (승인/거절 여부 포함)
         * @return 초기화된 {@link PetFamilyApprovalResponse} 객체
         */
        public static PetFamilyApprovalResponse toDto(Long petId, String message) {
            return PetFamilyApprovalResponse.builder()
                    .petId(petId)
                    .message(message)
                    .build();
        }
    }

    /**
     * 신청 대기자 목록 조회 시 반환되는 페이징 된 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "가족 신청 대기자 목록 응답 (페이징 포함)")
    public static class PendingUserListResponse {

        @Schema(description = "응답 메시지", example = "조회를 성공했습니다.")
        private String message;

        @Schema(description = "대기자 데이터 목록")
        private List<PendingUserResponse> users;

        @Schema(description = "총 페이지 수", example = "5")
        private int totalPages;

        @Schema(description = "총 데이터 수", example = "48")
        private long totalElements;

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        public static PendingUserListResponse toDto(Page<PendingUserResponse> page, String message) {
            return PendingUserListResponse.builder()
                    .message(message)
                    .users(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .currentPage(page.getNumber())
                    .pageSize(page.getSize())
                    .build();
        }

        /**
         * 개별 대기자(유저) 정보와 대상 펫 정보를 담는 내부 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "가족 신청 대기자(유저) 및 대상 펫 정보")
        public static class PendingUserResponse {

            @Schema(description = "신청한 유저 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            private UUID userId;

            @Schema(description = "신청한 유저 닉네임", example = "강아지조아")
            private String nickname;

            @Schema(description = "신청한 유저 프로필 이미지 URL", example = "https://example.com/user_profile.jpg")
            private String profileUrl;

            @Schema(description = "신청 대상 반려동물 ID", example = "101")
            private Long targetPetId;

            @Schema(description = "신청 대상 반려동물 이름", example = "보리")
            private String targetPetName;

            @Schema(description = "신청 대상 반려동물 프로필 이미지 URL", example = "https://example.com/pet_profile.jpg")
            private String targetPetImageUrl;

            @Schema(description = "신청 상태", example = "PENDING")
            private String status;

            @Schema(description = "신청 일시", example = "2024-02-01T12:00:00")
            private LocalDateTime requestedAt;

            @Schema(description = "거절 일시 (REJECTED 상태일 때만 반환)", example = "2024-02-01T12:15:00", nullable = true)
            private LocalDateTime rejectedAt;

            public static PendingUserResponse toDto(UUID userId, String nickname, String profileUrl,
                                                    Long targetPetId, String targetPetName, String targetPetImageUrl,
                                                    String status, LocalDateTime requestedAt, LocalDateTime rejectedAt) {
                return PendingUserResponse.builder()
                        .userId(userId)
                        .nickname(nickname)
                        .profileUrl(profileUrl)
                        .targetPetId(targetPetId)
                        .targetPetName(targetPetName)
                        .targetPetImageUrl(targetPetImageUrl)
                        .status(status)
                        .requestedAt(requestedAt)
                        .rejectedAt(rejectedAt)
                        .build();
            }
        }
    }

    /**
     * 차단된 가족 신청자 목록 조회 시 반환되는 페이징 된 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "차단된 가족 신청자 목록 응답 (페이징 포함)")
    public static class BlockedUserListResponse {

        @Schema(description = "응답 메시지", example = "조회를 성공했습니다.")
        private String message;

        @Schema(description = "차단된 유저 데이터 목록")
        private List<BlockedUserResponse> users;

        @Schema(description = "총 페이지 수", example = "5")
        private int totalPages;

        @Schema(description = "총 데이터 수", example = "48")
        private long totalElements;

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        /**
         * 페이징된 차단 유저 DTO 목록을 차단 목록 응답 DTO로 변환합니다.
         *
         * @param page    차단 유저 DTO가 담긴 페이징 객체
         * @param message 응답 메시지
         * @return 페이징 메타데이터와 차단 유저 목록이 포함된 응답 DTO
         */
        public static BlockedUserListResponse toDto(Page<BlockedUserResponse> page, String message) {
            return BlockedUserListResponse.builder()
                    .message(message)
                    .users(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .currentPage(page.getNumber())
                    .pageSize(page.getSize())
                    .build();
        }

        /**
         * 개별 차단 유저 정보와 대상 펫 정보를 담는 내부 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "차단된 가족 신청자 및 대상 펫 정보")
        public static class BlockedUserResponse {

            @Schema(description = "차단된 유저 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            private UUID userId;

            @Schema(description = "차단된 유저 닉네임", example = "강아지조아")
            private String nickname;

            @Schema(description = "차단된 유저 프로필 이미지 URL", example = "https://example.com/user_profile.jpg")
            private String profileUrl;

            @Schema(description = "차단 대상 반려동물 ID", example = "101")
            private Long targetPetId;

            @Schema(description = "차단 대상 반려동물 이름", example = "보리")
            private String targetPetName;

            @Schema(description = "차단 대상 반려동물 프로필 이미지 URL", example = "https://example.com/pet_profile.jpg")
            private String targetPetImageUrl;

            @Schema(description = "차단 일시", example = "2024-02-01T12:00:00")
            private LocalDateTime blockedAt;

            /**
             * 차단된 유저 정보와 대상 반려동물 정보를 개별 차단 유저 응답 DTO로 변환합니다.
             *
             * @param userId            차단된 유저 ID
             * @param nickname          차단된 유저 닉네임
             * @param profileUrl        차단된 유저 프로필 이미지 URL
             * @param targetPetId       차단 대상 반려동물 ID
             * @param targetPetName     차단 대상 반려동물 이름
             * @param targetPetImageUrl 차단 대상 반려동물 프로필 이미지 URL
             * @param blockedAt         차단 일시
             * @return 개별 차단 유저 응답 DTO
             */
            public static BlockedUserResponse toDto(UUID userId, String nickname, String profileUrl,
                                                    Long targetPetId, String targetPetName, String targetPetImageUrl,
                                                    LocalDateTime blockedAt) {
                return BlockedUserResponse.builder()
                        .userId(userId)
                        .nickname(nickname)
                        .profileUrl(profileUrl)
                        .targetPetId(targetPetId)
                        .targetPetName(targetPetName)
                        .targetPetImageUrl(targetPetImageUrl)
                        .blockedAt(blockedAt)
                        .build();
            }
        }
    }

    /**
     * 내 신청 내역 조회 시 반환되는 페이징 된 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "내 신청 내역 목록 응답 (페이징 포함)")
    public static class PetApplicationListResponse {

        @Schema(description = "응답 메시지", example = "조회를 성공했습니다.")
        private String message;

        @Schema(description = "신청 내역 데이터 목록")
        private List<PetApplicationResponse> applications;

        @Schema(description = "총 페이지 수", example = "3")
        private int totalPages;

        @Schema(description = "총 데이터 수", example = "25")
        private long totalElements;

        @Schema(description = "현재 페이지 번호", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        public static PetApplicationListResponse toDto(Page<PetApplicationResponse> page, String message) {
            return PetApplicationListResponse.builder()
                    .message(message)
                    .applications(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .currentPage(page.getNumber())
                    .pageSize(page.getSize())
                    .build();
        }

        /**
         * 개별 신청 내역 정보를 담는 내부 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "개별 신청 내역 정보")
        public static class PetApplicationResponse {

            @Schema(description = "반려동물 ID", example = "101")
            private Long petId;

            @Schema(description = "반려동물 이름", example = "보리")
            private String petName;

            @Schema(description = "반려동물 프로필 이미지 URL", example = "https://example.com/pet_profile.jpg")
            private String petImageUrl;

            @Schema(description = "신청 상태", example = "PENDING")
            private String status;

            @Schema(description = "신청 일시", example = "2024-02-01T12:00:00")
            private LocalDateTime requestedAt;

            @Schema(description = "거절 일시 (REJECTED 상태일 때만 반환)", example = "2024-02-01T12:15:00", nullable = true)
            private LocalDateTime rejectedAt;

            public static PetApplicationResponse toDto(Long petId, String petName, String petImageUrl,
                                                       String status, LocalDateTime requestedAt,
                                                       LocalDateTime rejectedAt) {
                return PetApplicationResponse.builder()
                        .petId(petId)
                        .petName(petName)
                        .petImageUrl(petImageUrl)
                        .status(status)
                        .requestedAt(requestedAt)
                        .rejectedAt(rejectedAt)
                        .build();
            }
        }
    }

    /**
     * 반려동물 정보가 삭제되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 삭제 결과 응답")
    public static class PetDeleteResponse {

        @Schema(description = "처리 결과 메시지", example = "반려동물 목록에서 삭제되었습니다.")
        private String message;

        /**
         * 성공 메시지를 담은 응답 DTO를 생성합니다.
         *
         * @return 초기화된 {@link PetDeleteResponse} 객체
         */
        public static PetDeleteResponse toDto(String message) {
            return PetDeleteResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 펫 디바이스 재등록 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "디바이스 재등록 결과 응답")
    public static class PetDeviceUpdateResponse {

        @Schema(description = "처리 결과 메시지", example = "디바이스가 성공적으로 재등록되었습니다.")
        private String message;

        @Schema(description = "반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "반려동물 이름", example = "보리")
        private String petName;

        @Schema(description = "이전 디바이스 ID", example = "OLD_ABC123XYZ")
        private String previousDeviceId;

        @Schema(description = "새로운 디바이스 ID", example = "NEW_ABC123XYZ")
        private String newDeviceId;

        /**
         * 엔티티를 직접 받지 않고, 필요한 필드만 전달받아 DTO를 생성합니다.
         */
        public static PetDeviceUpdateResponse toDto(Long petId, String petName, String oldDeviceId, String newDeviceId, String message) {
            return PetDeviceUpdateResponse.builder()
                    .message(message)
                    .petId(petId)
                    .petName(petName)
                    .previousDeviceId(oldDeviceId)
                    .newDeviceId(newDeviceId)
                    .build();
        }
    }

    /**
     * 디바이스 ID 중복 확인 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "디바이스 ID 중복 확인 결과 응답")
    public static class PetDeviceCheckResponse {

        @Schema(description = "처리 결과 메시지", example = "사용 가능한 디바이스 ID입니다.")
        private String message;

        @Schema(description = "디바이스 ID 사용 가능 여부", example = "true")
        private boolean available;

        public static PetDeviceCheckResponse toDto(String message, boolean available) {
            return PetDeviceCheckResponse.builder()
                    .message(message)
                    .available(available)
                    .build();
        }
    }

    /**
     * 반려동물 특이사항 생성 결과 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 특이사항 생성 응답")
    public static class PetSignificantCreateResponse {

        @Schema(description = "처리 결과 메시지", example = "펫 특이사항 등록을 완료했습니다.")
        private String message;

        @Schema(description = "생성된 특이사항 ID", example = "1")
        private Long noteId;

        /**
         * 특이사항 생성 응답 DTO를 생성합니다.
         *
         * @param message 처리 결과 메시지
         * @param noteId  생성된 특이사항 ID
         * @return 생성된 응답 DTO
         */
        public static PetSignificantCreateResponse toDto(String message, Long noteId) {
            return PetSignificantCreateResponse.builder()
                    .message(message)
                    .noteId(noteId)
                    .build();
        }
    }

    /**
     * 반려동물 특이사항 수정 결과 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 특이사항 수정 응답")
    public static class PetSignificantUpdateResponse {

        @Schema(description = "처리 결과 메시지", example = "펫 특이사항 수정을 완료했습니다.")
        private String message;

        @Schema(description = "수정된 특이사항 ID", example = "1")
        private Long noteId;

        /**
         * 특이사항 수정 응답 DTO를 생성합니다.
         *
         * @param message 처리 결과 메시지
         * @param noteId  수정된 특이사항 ID
         * @return 생성된 응답 DTO
         */
        public static PetSignificantUpdateResponse toDto(String message, Long noteId) {
            return PetSignificantUpdateResponse.builder()
                    .message(message)
                    .noteId(noteId)
                    .build();
        }
    }

    /**
     * 반려동물 특이사항 삭제 결과 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 특이사항 삭제 응답")
    public static class PetSignificantDeleteResponse {

        @Schema(description = "처리 결과 메시지", example = "펫 특이사항 삭제를 완료했습니다.")
        private String message;

        @Schema(description = "삭제된 특이사항 ID", example = "1")
        private Long noteId;

        /**
         * 특이사항 삭제 응답 DTO를 생성합니다.
         *
         * @param message 처리 결과 메시지
         * @param noteId  삭제된 특이사항 ID
         * @return 생성된 응답 DTO
         */
        public static PetSignificantDeleteResponse toDto(String message, Long noteId) {
            return PetSignificantDeleteResponse.builder()
                    .message(message)
                    .noteId(noteId)
                    .build();
        }
    }

    /**
     * 반려동물 특이사항 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 특이사항 목록 조회 응답")
    public static class PetSignificantListResponse {

        @Schema(description = "처리 결과 메시지", example = "펫 특이사항 목록 조회를 완료했습니다.")
        private String message;

        @Schema(description = "특이사항 목록")
        private List<NoteSummary> notes;

        @Schema(description = "총 페이지 수", example = "1")
        private int totalPages;

        @Schema(description = "총 데이터 수", example = "1")
        private long totalElements;

        @Schema(description = "현재 페이지 번호", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        /**
         * 페이지 데이터를 기반으로 특이사항 목록 조회 응답 DTO를 생성합니다.
         *
         * @param notePage 특이사항 요약 목록 페이지
         * @param message 처리 결과 메시지
         * @return 특이사항 목록 조회 응답 DTO
         */
        public static PetSignificantListResponse toDto(Page<NoteSummary> notePage, String message) {
            return PetSignificantListResponse.builder()
                    .message(message)
                    .notes(notePage.getContent())
                    .totalPages(notePage.getTotalPages())
                    .totalElements(notePage.getTotalElements())
                    .currentPage(notePage.getNumber())
                    .pageSize(notePage.getSize())
                    .build();
        }

        /**
         * 반려동물 특이사항 요약 정보 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "반려동물 특이사항 요약 정보")
        public static class NoteSummary {

            @Schema(description = "특이사항 ID", example = "12")
            private Long noteId;

            @Schema(description = "특이사항 내용", example = "닭고기 알레르기가 있어요.")
            private String noteContent;

            @Schema(description = "특이사항 타입", example = "ALLERGY")
            private String noteType;

            @Schema(description = "생성 시각", example = "2026-02-21T08:20:00")
            private LocalDateTime createdAt;
        }
    }

    /**
     * 반려동물 상세 정보 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 상세 정보 응답")
    public static class PetDetailResponse {

        @Schema(description = "처리 결과 메시지", example = "반려동물 정보 조회에 성공했습니다.")
        private String message;

        @Schema(description = "반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "반려동물 이름", example = "보리")
        private String petName;

        @Schema(description = "반려동물 프로필 이미지 URL", example = "https://example.com/images/bori.jpg")
        private String imageFileUrl;

        @Schema(description = "반려동물 종", example = "CANINE")
        private String species;

        @Schema(description = "반려동물 품종", example = "말티즈")
        private String breed;

        @Schema(description = "반려동물 성별", example = "FEMALE")
        private String sex;

        @Schema(description = "반려동물 나이", example = "5")
        private Integer age;

        @Schema(description = "반려동물 생년월일", example = "2020-09-30T00:00:00")
        private LocalDateTime birth;

        @Schema(description = "반려동물 등록번호", example = "4102020001231")
        private String registrationNumber;

        @Schema(description = "디바이스 ID", example = "ABC123XYZ")
        private String deviceId;

        @Schema(description = "기준 심박수", example = "85")
        private Integer referenceHeartRate;

        @Schema(description = "가족 구성원 목록")
        private List<FamilyMember> familyMembers;

        @Schema(description = "최근 활동 정보")
        private LastActivity lastActivity;

        @Schema(description = "특이사항 목록")
        private List<SpecialNote> specialNotes;

        @Schema(description = "특이사항 개수", example = "2")
        private Integer specialNotesCount;

        @Schema(description = "체중 정보")
        private WeightInfo weightInfo;

        /**
         * 가족 구성원 응답 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "가족 구성원 정보")
        public static class FamilyMember {
            @Schema(description = "사용자 ID", example = "8340705f-59af-4cc0-b0ce-f196b69acd5a")
            private UUID userId;

            @Schema(description = "사용자 이름", example = "김철수")
            private String userName;

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profiles/kim.jpg")
            private String profileImageUrl;
        }

        /**
         * 최근 활동 응답 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "최근 활동 정보")
        public static class LastActivity {
            @Schema(description = "활동 ID", example = "301")
            private Long activityId;

            @Schema(description = "활동 타입", example = "WALK")
            private String activityType;

            @Schema(description = "시작 시각", example = "2025-10-14T09:30:00")
            private LocalDateTime startTime;

            @Schema(description = "종료 시각", example = "2025-10-14T10:15:00")
            private LocalDateTime endTime;

            @Schema(description = "이동 거리", example = "2.3")
            private java.math.BigDecimal distance;
        }

        /**
         * 특이사항 응답 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "특이사항 정보")
        public static class SpecialNote {
            @Schema(description = "특이사항 ID", example = "1")
            private Long noteId;

            @Schema(description = "특이사항 내용", example = "닭고기 알레르기가 있어요.")
            private String noteContent;

            @Schema(description = "특이사항 타입", example = "ALLERGY")
            private String noteType;

            @Schema(description = "생성 시각", example = "2026-02-20T10:00:00")
            private LocalDateTime createdAt;
        }

        /**
         * 체중 정보 응답 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "체중 정보")
        public static class WeightInfo {
            @Schema(description = "현재 체중", example = "4.2")
            private Double currentWeight;

            @Schema(description = "체중 추세", example = "STABLE")
            private String weightTrend;
        }
    }
}
