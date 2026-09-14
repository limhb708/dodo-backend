package com.dodo.backend.notification.controller;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationReadUpdateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.PageInfoResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.UnreadNotificationCountResponse;
import com.dodo.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 알림 컨트롤러 API 경로를 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver())
                .build();
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
    }

    /**
     * 알림 목록 조회 API가 200을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("알림 목록 조회 API 성공")
    void getNotifications_Success() throws Exception {
        NotificationListResponse response = NotificationListResponse.builder()
                .pageInfo(PageInfoResponse.builder().page(1).size(20).totalElements(0).totalPages(0).build())
                .data(List.of())
                .build();
        given(notificationService.getNotifications(eq(userId), eq(1), eq(20), eq(null), eq(null))).willReturn(response);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageInfo.page").value(1));
    }

    @Test
    @DisplayName("notification list API passes filters")
    void getNotifications_WithFilters() throws Exception {
        NotificationListResponse response = NotificationListResponse.builder()
                .pageInfo(PageInfoResponse.builder().page(2).size(10).totalElements(0).totalPages(0).build())
                .data(List.of())
                .build();
        given(notificationService.getNotifications(eq(userId), eq(2), eq(10), eq(false), eq("COMMENT,REACTION")))
                .willReturn(response);

        mockMvc.perform(get("/notifications")
                        .param("page", "2")
                        .param("size", "10")
                        .param("isRead", "false")
                        .param("type", "COMMENT,REACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageInfo.page").value(2));

        verify(notificationService).getNotifications(userId, 2, 10, false, "COMMENT,REACTION");
    }

    /**
     * 알림 읽음 처리 API가 200을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("알림 읽음 처리 API 성공")
    void updateReadStatus_Success() throws Exception {
        given(notificationService.updateReadStatus(eq(userId), eq(1L), org.mockito.ArgumentMatchers.any(NotificationReadUpdateRequest.class)))
                .willReturn(NotificationSimpleResponse.toDto("알림이 성공적으로 읽음 처리되었습니다."));

        mockMvc.perform(patch("/notifications/{notificationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationReadUpdateRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림이 성공적으로 읽음 처리되었습니다."));
    }

    /**
     * 읽지 않은 알림 개수 조회 API가 200을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("읽지 않은 알림 개수 조회 API 성공")
    void getUnreadCount_Success() throws Exception {
        given(notificationService.getUnreadCount(userId)).willReturn(UnreadNotificationCountResponse.toDto(3L));

        mockMvc.perform(get("/notifications/count/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @DisplayName("read all notifications API success")
    void readAll_Success() throws Exception {
        given(notificationService.readAll(userId))
                .willReturn(NotificationSimpleResponse.toDto("all read"));

        mockMvc.perform(patch("/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("all read"));

        verify(notificationService).readAll(userId);
    }

    /**
     * 알림 삭제 API가 204를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("알림 삭제 API 성공")
    void deleteNotification_Success() throws Exception {
        mockMvc.perform(delete("/notifications/{notificationId}", 1L)
                )
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(userId, 1L);
    }

    /**
     * 모든 알림 삭제 API가 204를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("모든 알림 삭제 API 성공")
    void deleteAll_Success() throws Exception {
        mockMvc.perform(delete("/notifications/all"))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteAll(userId);
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return User.withUsername(userId.toString()).password("").roles("USER").build();
            }
        };
    }
}
