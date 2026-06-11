package com.dodo.backend.userpet.entity;

/**
 * 펫 등록 신청에 대한 승인 상태를 정의하는 Enum 클래스입니다.
 * <p>
 * 대기(PENDING), 승인(APPROVED), 거절(REJECTED), 차단(BLOCKED) 상태를 관리합니다.
 */
public enum RegistrationStatus {
    PENDING, APPROVED, REJECTED, BLOCKED
}
