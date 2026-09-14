package com.dodo.backend.notification.integration;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentCreateRequest;
import com.dodo.backend.comment.mapper.CommentMapper;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.comment.service.CommentServiceImpl;
import com.dodo.backend.common.config.JpaAuditingConfig;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationListResponse;
import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.notification.repository.NotificationRepository;
import com.dodo.backend.notification.service.FcmNotificationSender;
import com.dodo.backend.notification.service.NotificationServiceImpl;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({CommentServiceImpl.class, NotificationServiceImpl.class, JpaAuditingConfig.class})
class CommentNotificationIntegrationTest {

    @Autowired
    private CommentServiceImpl commentService;

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CommentRepository commentRepository;

    @MockitoBean
    private BoardService boardService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private FcmNotificationSender fcmNotificationSender;

    @Test
    @DisplayName("comment creation persists COMMENT notification for board writer")
    void createComment_SavesCommentNotificationForBoardWriter() {
        User boardWriter = userRepository.save(createUser("writer@test.com", "writer"));
        User commenter = userRepository.save(createUser("commenter@test.com", "commenter"));
        Board board = boardRepository.save(Board.builder()
                .user(boardWriter)
                .boardTitle("board title")
                .boardContent("board content")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build());
        CommentCreateRequest request = CommentCreateRequest.builder()
                .boardId(board.getBoardId())
                .commentContent("hello")
                .build();

        when(boardService.getBoardById(board.getBoardId())).thenReturn(board);
        when(userService.getUserById(commenter.getUsersId())).thenReturn(commenter);

        commentService.createComment(commenter.getUsersId(), request);
        commentRepository.flush();
        notificationRepository.flush();

        List<Notification> notifications = notificationRepository.findAll();

        assertThat(notifications).hasSize(1);
        Notification notification = notifications.get(0);
        assertThat(notification.getUser().getUsersId()).isEqualTo(boardWriter.getUsersId());
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(notification.getRelatedId()).isEqualTo(board.getBoardId());
        assertThat(notification.getIsRead()).isFalse();
        assertThat(notification.getNotificationCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("created COMMENT notification appears in board writer notification list")
    void getNotifications_ReturnsCreatedCommentNotificationForBoardWriter() {
        User boardWriter = userRepository.save(createUser("list-writer@test.com", "writer"));
        User commenter = userRepository.save(createUser("list-commenter@test.com", "commenter"));
        Board board = boardRepository.save(Board.builder()
                .user(boardWriter)
                .boardTitle("board title")
                .boardContent("board content")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build());
        CommentCreateRequest request = CommentCreateRequest.builder()
                .boardId(board.getBoardId())
                .commentContent("hello")
                .build();

        when(boardService.getBoardById(board.getBoardId())).thenReturn(board);
        when(userService.getUserById(commenter.getUsersId())).thenReturn(commenter);

        commentService.createComment(commenter.getUsersId(), request);
        commentRepository.flush();
        notificationRepository.flush();

        NotificationListResponse response = notificationService.getNotifications(
                boardWriter.getUsersId(),
                1,
                20,
                false,
                "COMMENT"
        );

        assertThat(response.getPageInfo().getPage()).isEqualTo(1);
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(1);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getNotificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(response.getData().get(0).getRelatedId()).isEqualTo(board.getBoardId());
    }

    private User createUser(String email, String nickname) {
        return User.builder()
                .role(UserRole.USER)
                .name("tester")
                .email(email)
                .hasFamily(false)
                .nickname(nickname)
                .region("Seoul")
                .profileUrl("https://example.com/profile.png")
                .userStatus(UserStatus.ACTIVE)
                .notificationEnabled(true)
                .build();
    }
}
