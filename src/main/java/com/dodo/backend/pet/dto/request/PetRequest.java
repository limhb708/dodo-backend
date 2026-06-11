package com.dodo.backend.pet.dto.request;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 펫 도메인과 관련된 요청 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "펫 관련 요청 DTO 그룹")
public class PetRequest {

    /**
     * 신규 펫 등록을 위해 클라이언트로부터 전달받는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "펫 등록 요청")
    public static class PetRegisterRequest {

        @Schema(description = "반려동물 등록번호 (선택사항, 없을 시 null)", example = "null", nullable = true)
        private String registrationNumber;

        @Schema(description = "반려동물 프로필 이미지 URL", example = "https://example.com/images/bori.jpg")
        private String imageFileUrl;

        @Schema(description = "성별 (MALE, FEMALE, NEUTER)", example = "MALE")
        @NotNull(message = "성별은 필수입니다.")
        private String sex;

        @Schema(description = "나이", example = "3")
        @NotNull(message = "나이는 필수입니다.")
        private Integer age;

        @Schema(description = "생년월일 (ISO8601)", example = "2022-01-01T00:00:00")
        @NotNull(message = "생일은 필수입니다.")
        private LocalDateTime birth;

        @Schema(description = "펫 이름", example = "바둑이")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        @NotBlank(message = "이름은 필수입니다.")
        private String petName;

        @Schema(description = "종 (CANINE, FELINE)", example = "CANINE")
        @NotNull(message = "종은 필수입니다.")
        private String species;

        @Schema(description = "품종", example = "진돗개")
        @NotBlank(message = "품종은 필수입니다.")
        private String breed;

        @Schema(description = "심박수 기준", example = "80")
        @NotNull(message = "심박수는 필수입니다.")
        @Positive(message = "심박수는 양수여야 합니다.")
        private Integer referenceHeartRate;

        @Schema(description = "디바이스 ID", example = "ABC123XYZ")
        @NotBlank(message = "디바이스 ID는 필수입니다.")
        private String deviceId;

        /**
         * 요청 DTO의 데이터를 기반으로 새로운 {@link Pet} 엔티티를 생성합니다.
         *
         * @return 초기화된 Pet 엔티티 객체
         */
        public Pet toEntity() {
            return Pet.builder()
                    .registrationNumber(this.registrationNumber)
                    .sex(PetSex.valueOf(this.sex))
                    .age(this.age)
                    .birth(this.birth)
                    .petName(this.petName)
                    .species(PetSpecies.valueOf(this.species))
                    .breed(this.breed)
                    .referenceHeartRate(this.referenceHeartRate)
                    .deviceId(this.deviceId)
                    .build();
        }
    }

    /**
     * 반려동물 정보 수정을 위한 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "반려동물 정보 수정 요청")
    public static class PetUpdateRequest {

        @Schema(description = "변경할 등록번호 (선택)", example = "159263487012345")
        @Size(max = 15, message = "등록번호는 15자 이하여야 합니다.")
        private String registrationNumber;

        @Schema(description = "변경할 반려동물 프로필 이미지 URL (선택)", example = "https://example.com/images/bori.jpg")
        private String imageFileUrl;

        @Schema(description = "변경할 성별 (선택, MALE, FEMALE, NEUTER)", example = "FEMALE")
        private String sex;

        @Schema(description = "변경할 나이 (선택)", example = "5")
        private Integer age;

        @Schema(description = "변경할 펫 이름 (선택)", example = "까미")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        private String petName;

        @Schema(description = "변경할 품종 (선택)", example = "리트리버")
        private String breed;

        @Schema(description = "변경할 심박수 기준치 (선택)", example = "90")
        @Positive(message = "심박수는 양수여야 합니다.")
        private Integer referenceHeartRate;

        @Schema(description = "변경할 디바이스 ID (선택)", example = "DEV-UPDATE-777")
        private String deviceId;
    }

    /**
     * 반려동물 가족 초대를 수락하기 위해 사용자가 입력한 코드 정보를 담는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "가족 초대 코드 입력 요청")
    public static class PetFamilyJoinRequest {

        @Schema(description = "(영문 대문자 + 숫자, 총 6자리)", example = "7X9K2P")
        @NotBlank(message = "초대 코드는 필수입니다.")
        @Size(min = 6, max = 6, message = "초대 코드는 6자리여야 합니다.")
        private String code;
    }

    /**
     * 가족 등록 요청 승인/거절/차단을 위한 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "가족 요청 승인/거절/차단 요청")
    public static class PetFamilyApprovalRequest {

        @NotNull
        @Schema(description = "반려동물 ID", example = "1")
        private Long petId;

        @NotNull
        @Schema(description = "승인/거절/차단 대상 유저 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID targetUserId;

        @NotBlank
        @Schema(description = "처리할 상태 (APPROVED: 승인, REJECTED: 거절, BLOCKED: 차단)", example = "APPROVED")
        private String action;
    }

    /**
     * 가족 신청 차단 해제를 위한 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "가족 신청 차단 해제 요청")
    public static class PetFamilyBlockReleaseRequest {

        @NotNull
        @Schema(description = "반려동물 ID", example = "1")
        private Long petId;

        @NotNull
        @Schema(description = "차단 해제 대상 유저 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID targetUserId;
    }

    /**
     * 펫 디바이스 재등록 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "펫 디바이스 재등록 요청 ")
    public static class PetDeviceUpdateRequest {
        @NotBlank(message = "디바이스 ID는 필수입니다.")
        @Schema(description = "새로운 디바이스 ID", example = "NEW_ABC123XYZ")
        private String deviceId;
    }

    /**
     * 디바이스 ID 중복 확인 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "디바이스 ID 중복 확인 요청")
    public static class PetDeviceCheckRequest {
        @NotBlank(message = "디바이스 ID는 필수입니다.")
        @Schema(description = "확인할 디바이스 ID", example = "ABC123XYZ")
        private String deviceId;
    }

    /**
     * 반려동물 특이사항 생성을 위한 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "반려동물 특이사항 생성 요청")
    public static class PetSignificantCreateRequest {

        @NotNull(message = "잘못된 요청입니다.")
        @Schema(description = "반려동물 ID", example = "1")
        private Long petId;

        @NotBlank(message = "잘못된 요청입니다.")
        @Schema(description = "특이사항 내용", example = "닭고기 알레르기가 있어요.")
        private String noteContent;

        @NotBlank(message = "잘못된 요청입니다.")
        @Schema(description = "특이사항 타입", example = "ALLERGY")
        private String noteType;
    }

    /**
     * 반려동물 특이사항 수정을 위한 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "반려동물 특이사항 수정 요청")
    public static class PetSignificantUpdateRequest {

        @Schema(description = "수정할 특이사항 내용", example = "사료를 연어 베이스로 변경 필요")
        private String noteContent;

        @Schema(description = "수정할 특이사항 타입", example = "FOOD")
        private String noteType;
    }

}
