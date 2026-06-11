package com.dodo.backend.userpet.repository;

import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.entity.UserPet.UserPetId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link UserPet} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 * <p>
 * 유저와 펫 사이의 관계 데이터 생성 및 관리를 수행합니다.
 */
@Repository
public interface UserPetRepository extends JpaRepository<UserPet, UserPetId> {


    /**
     * 특정 사용자가 속한 반려동물 목록 중, 지정된 상태(예: APPROVED)인 데이터만 페이징하여 조회합니다.
     * <p>
     * {@code @EntityGraph}를 사용하여 연관된 Pet 엔티티를 함께 로딩(Fetch Join 효과)합니다.
     *
     * @param userId   사용자 ID
     * @param status   가족 등록 상태 (APPROVED 등)
     * @param pageable 페이징 정보
     * @return 상태 조건에 맞는 페이징된 UserPet 목록
     */
    @EntityGraph(attributePaths = "pet")
    Page<UserPet> findAllByUser_UsersIdAndRegistrationStatus(
            UUID userId,
            RegistrationStatus status,
            Pageable pageable
    );

    /**
     * 관리자(요청자)가 APPROVED 상태로 소유하고 있는 모든 반려동물에 대해,
     * 들어온 가족 신청(PENDING) 목록을 전체 조회합니다.
     * <p>
     * N+1 문제를 방지하기 위해 신청자(user)와 대상 펫(pet) 정보를 Fetch Join으로 함께 가져옵니다.
     *
     * @param managerId 관리자(현재 로그인한 유저)의 ID
     * @param pageable  페이징 정보
     * @return 관리하는 펫들에 대한 모든 승인 대기 내역
     */
    @Query(
            value = "SELECT up FROM UserPet up " +
                    "JOIN FETCH up.user " +
                    "JOIN FETCH up.pet " +
                    "WHERE up.registrationStatus IN :statuses " +
                    "AND up.pet.petId IN (" +
                    "   SELECT my.pet.petId FROM UserPet my " +
                    "   WHERE my.user.usersId = :managerId AND my.registrationStatus = 'APPROVED'" +
                    ")",
            countQuery = "SELECT COUNT(up) FROM UserPet up " +
                    "WHERE up.registrationStatus IN :statuses " +
                    "AND up.pet.petId IN (" +
                    "   SELECT my.pet.petId FROM UserPet my " +
                    "   WHERE my.user.usersId = :managerId AND my.registrationStatus = 'APPROVED'" +
                    ")"
    )
    Page<UserPet> findAllPendingRequestsByManager(
            @Param("managerId") UUID managerId,
            @Param("statuses") List<RegistrationStatus> statuses,
            Pageable pageable
    );

    /**
     * 관리자(요청자)가 APPROVED 상태로 소유하고 있는 모든 반려동물에 대해,
     * 차단된 가족 신청(BLOCKED) 목록을 전체 조회합니다.
     *
     * @param managerId 관리자(현재 로그인한 유저)의 ID
     * @param pageable  페이징 정보
     * @return 관리하는 펫들에 대한 모든 차단 내역
     */
    @Query(
            value = "SELECT up FROM UserPet up " +
                    "JOIN FETCH up.user " +
                    "JOIN FETCH up.pet " +
                    "WHERE up.registrationStatus = 'BLOCKED' " +
                    "AND up.pet.petId IN (" +
                    "   SELECT my.pet.petId FROM UserPet my " +
                    "   WHERE my.user.usersId = :managerId AND my.registrationStatus = 'APPROVED'" +
                    ")",
            countQuery = "SELECT COUNT(up) FROM UserPet up " +
                    "WHERE up.registrationStatus = 'BLOCKED' " +
                    "AND up.pet.petId IN (" +
                    "   SELECT my.pet.petId FROM UserPet my " +
                    "   WHERE my.user.usersId = :managerId AND my.registrationStatus = 'APPROVED'" +
                    ")"
    )
    Page<UserPet> findAllBlockedRequestsByManager(@Param("managerId") UUID managerId, Pageable pageable);

    /**
     * 특정 사용자의 반려동물 신청 내역 중 지정된 상태 목록에 포함되는 데이터만 페이징 조회합니다.
     *
     * @param userId   사용자 ID
     * @param statuses 조회할 등록 상태 목록
     * @param pageable 페이징 정보
     * @return 상태 조건에 맞는 페이징된 UserPet 목록
     */
    @EntityGraph(attributePaths = "pet")
    Page<UserPet> findAllByUser_UsersIdAndRegistrationStatusIn(
            UUID userId,
            List<RegistrationStatus> statuses,
            Pageable pageable
    );

    /**
     * 특정 유저가 특정 반려동물의 소유자(APPROVED 상태)인지 확인합니다.
     * Spring Data JPA의 쿼리 메서드 기능을 사용하여 구현합니다.
     *
     * @param userId 유저 ID
     * @param petId 펫 ID
     * @param status 등록 상태 (주로 APPROVED)
     * @return 존재 여부 (true/false)
     */
    boolean existsByUser_UsersIdAndPet_PetIdAndRegistrationStatus(UUID userId, Long petId, RegistrationStatus status);

    /**
     * 유저 ID와 펫 ID를 이용해 특정 UserPet 엔티티(관계 정보)를 조회합니다.
     * (삭제 대상을 찾을 때 사용)
     *
     * @param userId 유저 ID
     * @param petId  펫 ID
     * @return UserPet 엔티티 (Optional)
     */
    Optional<UserPet> findByUser_UsersIdAndPet_PetId(UUID userId, Long petId);

    /**
     * 특정 반려동물에게 연결된 가족(UserPet)이 존재하는지 확인합니다.
     * (마지막 남은 가족인지 확인할 때 사용)
     *
     * @param petId 확인할 펫 ID
     * @return 연결된 데이터가 하나라도 있으면 true, 없으면 false
     */
    boolean existsByPet_PetId(Long petId);

    /**
     * 특정 사용자가 승인(APPROVED)된 반려동물 ID 목록을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param status 조회할 등록 상태
     * @return 반려동물 ID 목록
     */
    @Query("SELECT up.pet.petId FROM UserPet up WHERE up.user.usersId = :userId AND up.registrationStatus = :status")
    List<Long> findPetIdsByUserIdAndRegistrationStatus(
            @Param("userId") UUID userId,
            @Param("status") RegistrationStatus status
    );

    /**
     * 특정 반려동물의 승인(APPROVED) 가족 구성원 목록을 조회합니다.
     *
     * @param petId  조회할 반려동물 ID
     * @param status 조회할 등록 상태
     * @return 가족 구성원 목록
     */
    @EntityGraph(attributePaths = "user")
    List<UserPet> findAllByPet_PetIdAndRegistrationStatus(Long petId, RegistrationStatus status);
}
