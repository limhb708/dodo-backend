package com.dodo.backend.notification.repository;

import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

    Page<NotificationSchedule> findAllByScheduleStatus(NotificationScheduleStatus scheduleStatus, Pageable pageable);

    @Query("""
            select ns.notificationScheduleId
            from NotificationSchedule ns
            where ns.scheduleStatus = :scheduleStatus
              and ns.scheduledAt <= :scheduledAt
            order by ns.scheduledAt asc
            """)
    List<Long> findDueScheduleIds(
            @Param("scheduleStatus") NotificationScheduleStatus scheduleStatus,
            @Param("scheduledAt") LocalDateTime scheduledAt,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationSchedule ns
            set ns.scheduleStatus = :processingStatus,
                ns.processingToken = :processingToken,
                ns.processingStartedAt = :processingStartedAt
            where ns.notificationScheduleId in :scheduleIds
              and ns.scheduleStatus = :pendingStatus
            """)
    int claimDueSchedules(
            @Param("scheduleIds") List<Long> scheduleIds,
            @Param("pendingStatus") NotificationScheduleStatus pendingStatus,
            @Param("processingStatus") NotificationScheduleStatus processingStatus,
            @Param("processingToken") String processingToken,
            @Param("processingStartedAt") LocalDateTime processingStartedAt
    );

    List<NotificationSchedule> findByProcessingTokenOrderByScheduledAtAsc(String processingToken);

    Optional<NotificationSchedule> findByNotificationScheduleIdAndScheduleStatusAndProcessingToken(
            Long notificationScheduleId,
            NotificationScheduleStatus scheduleStatus,
            String processingToken
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationSchedule ns
            set ns.scheduleStatus = :pendingStatus,
                ns.processingToken = null,
                ns.processingStartedAt = null
            where ns.notificationScheduleId = :scheduleId
              and ns.scheduleStatus = :processingStatus
              and ns.processingToken = :processingToken
            """)
    int releaseClaim(
            @Param("scheduleId") Long scheduleId,
            @Param("processingToken") String processingToken,
            @Param("pendingStatus") NotificationScheduleStatus pendingStatus,
            @Param("processingStatus") NotificationScheduleStatus processingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationSchedule ns
            set ns.scheduleStatus = :pendingStatus,
                ns.processingToken = null,
                ns.processingStartedAt = null
            where ns.scheduleStatus = :processingStatus
              and ns.processingStartedAt < :expiredBefore
            """)
    int releaseExpiredClaims(
            @Param("expiredBefore") LocalDateTime expiredBefore,
            @Param("pendingStatus") NotificationScheduleStatus pendingStatus,
            @Param("processingStatus") NotificationScheduleStatus processingStatus
    );
}
