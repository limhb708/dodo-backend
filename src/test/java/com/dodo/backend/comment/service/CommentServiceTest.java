package com.dodo.backend.comment.service;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentCreateRequest;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentUpdateRequest;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentCreateResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentListQueryResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentListResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentSimpleResponse;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.comment.exception.CommentErrorCode;
import com.dodo.backend.comment.exception.CommentException;
import com.dodo.backend.comment.mapper.CommentMapper;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.notification.service.NotificationService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link CommentService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    /**
     * 댓글 작성 요청 시 게시글과 사용자를 조회하고 댓글을 저장하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 작성 성공: 댓글이 정상적으로 저장되고 작성 응답을 반환한다.")
    void createComment_Success() {
        log.info("테스트 시작: 댓글 작성 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);
        given(user.getNickname()).willReturn("멍멍이집사");

        Board board = Board.builder()
                .boardId(boardId)
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        CommentCreateRequest request = CommentCreateRequest.builder()
                .boardId(boardId)
                .commentContent("좋은 정보 감사합니다!")
                .parentCommentId(null)
                .build();

        Comment savedComment = Comment.builder()
                .commentId(123L)
                .board(board)
                .user(user)
                .commentContent(request.getCommentContent())
                .build();

        given(boardService.getBoardById(boardId)).willReturn(board);
        given(userService.getUserById(userId)).willReturn(user);
        given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

        // when
        CommentCreateResponse response = commentService.createComment(userId, request);

        // then
        assertNotNull(response);
        assertEquals("댓글이 성공적으로 작성되었습니다.", response.getMessage());
        assertEquals(123L, response.getCommentId());
        assertEquals("좋은 정보 감사합니다!", response.getCommentContent());
        assertEquals(userId.toString(), response.getUserId());
        assertEquals("멍멍이집사", response.getNickname());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals(board, captor.getValue().getBoard());
        assertEquals(user, captor.getValue().getUser());
        assertNull(captor.getValue().getParentComment());
        assertEquals("좋은 정보 감사합니다!", captor.getValue().getCommentContent());
        verify(notificationService).notifyCommentCreated(savedComment);

        log.info("테스트 종료: 댓글 작성 성공");
    }

    /**
     * 부모 댓글 ID가 전달되면 같은 게시글의 댓글을 부모 댓글로 연결하는지 검증합니다.
     */
    @Test
    @DisplayName("대댓글 작성 성공: 부모 댓글이 같은 게시글에 있으면 대댓글로 저장한다.")
    void createComment_Success_WithParentComment() {
        log.info("테스트 시작: 대댓글 작성 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        Long parentCommentId = 10L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);
        given(user.getNickname()).willReturn("멍멍이집사");

        Board board = Board.builder()
                .boardId(boardId)
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        Comment parentComment = Comment.builder()
                .commentId(parentCommentId)
                .board(board)
                .user(user)
                .commentContent("부모 댓글")
                .build();

        CommentCreateRequest request = CommentCreateRequest.builder()
                .boardId(boardId)
                .commentContent("대댓글입니다.")
                .parentCommentId(parentCommentId)
                .build();

        Comment savedComment = Comment.builder()
                .commentId(123L)
                .board(board)
                .user(user)
                .parentComment(parentComment)
                .commentContent(request.getCommentContent())
                .build();

        given(boardService.getBoardById(boardId)).willReturn(board);
        given(userService.getUserById(userId)).willReturn(user);
        given(commentRepository.findById(parentCommentId)).willReturn(Optional.of(parentComment));
        given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

        // when
        CommentCreateResponse response = commentService.createComment(userId, request);

        // then
        assertNotNull(response);
        assertEquals(123L, response.getCommentId());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals(parentComment, captor.getValue().getParentComment());
        verify(notificationService).notifyCommentCreated(savedComment);

        log.info("테스트 종료: 대댓글 작성 성공");
    }

    /**
     * 댓글 목록 조회 시 댓글 목록과 페이지 정보를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 목록 조회 성공: 댓글 목록과 페이지 정보를 반환한다.")
    void getComments_Success() {
        log.info("테스트 시작: 댓글 목록 조회 성공");

        // given
        Long boardId = 1L;
        Board board = Board.builder()
                .boardId(boardId)
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        CommentListQueryResponse queryResponse = CommentListQueryResponse.builder()
                .commentId(102L)
                .parentCommentId(null)
                .commentContent("두 번째 댓글입니다.")
                .userId("uuid-user-2")
                .nickname("산책왕뽀삐")
                .createdAt(LocalDateTime.of(2025, 10, 14, 14, 30))
                .build();

        given(boardService.getBoardById(boardId)).willReturn(board);
        given(commentMapper.findCommentsByBoardId(boardId, 0, 10)).willReturn(List.of(queryResponse));
        given(commentMapper.countCommentsByBoardId(boardId)).willReturn(1L);

        // when
        CommentListResponse response = commentService.getComments(boardId, 0, 10);

        // then
        assertNotNull(response);
        assertEquals("댓글 목록을 성공적으로 조회했습니다.", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals(102L, response.getData().get(0).getCommentId());
        assertEquals(1L, response.getPageInfo().getTotalElements());
        assertEquals(1, response.getPageInfo().getTotalPages());

        verify(boardService).getBoardById(boardId);
        verify(commentMapper).findCommentsByBoardId(boardId, 0, 10);
        verify(commentMapper).countCommentsByBoardId(boardId);

        log.info("테스트 종료: 댓글 목록 조회 성공");
    }

    /**
     * 댓글 목록 조회 시 잘못된 페이지 크기이면 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 목록 조회 실패: 페이지 크기가 100보다 크면 예외가 발생한다.")
    void getComments_Fail_InvalidSize() {
        log.info("테스트 시작: 댓글 목록 조회 실패 - 잘못된 페이지 크기");

        // when
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.getComments(1L, 0, 101));

        // then
        assertEquals(CommentErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(commentMapper, never()).findCommentsByBoardId(1L, 0, 101);

        log.info("테스트 종료: 댓글 목록 조회 실패 - 잘못된 페이지 크기");
    }

    /**
     * 댓글 작성자 본인이 댓글을 수정하면 Mapper를 통해 댓글 내용이 수정되는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 수정 성공: 작성자 본인이 댓글을 수정한다.")
    void updateComment_Success() {
        log.info("테스트 시작: 댓글 수정 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long commentId = 123L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);

        Comment comment = Comment.builder()
                .commentId(commentId)
                .user(user)
                .commentContent("기존 댓글")
                .build();

        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .commentContent("수정입니다.")
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        CommentSimpleResponse response = commentService.updateComment(userId, commentId, request);

        // then
        assertEquals("댓글이 성공적으로 수정되었습니다.", response.getMessage());
        verify(commentRepository).findById(commentId);
        verify(commentMapper).updateComment(commentId, "수정입니다.");

        log.info("테스트 종료: 댓글 수정 성공");
    }

    /**
     * 댓글 작성자가 아닌 사용자가 댓글을 수정하면 권한 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 수정 실패: 작성자가 아니면 예외가 발생한다.")
    void updateComment_Fail_PermissionDenied() {
        log.info("테스트 시작: 댓글 수정 실패 - 권한 없음");

        // given
        UUID ownerId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        Long commentId = 123L;
        User owner = mock(User.class);
        given(owner.getUsersId()).willReturn(ownerId);

        Comment comment = Comment.builder()
                .commentId(commentId)
                .user(owner)
                .commentContent("기존 댓글")
                .build();

        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .commentContent("수정입니다.")
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.updateComment(requestUserId, commentId, request));

        // then
        assertEquals(CommentErrorCode.UPDATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(commentRepository).findById(commentId);
        verify(commentMapper, never()).updateComment(commentId, "수정입니다.");

        log.info("테스트 종료: 댓글 수정 실패 - 권한 없음");
    }

    /**
     * 댓글 작성자 본인이 댓글을 삭제하면 Repository를 통해 댓글이 삭제되는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 삭제 성공: 작성자 본인이 댓글을 삭제한다.")
    void deleteComment_Success() {
        log.info("테스트 시작: 댓글 삭제 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long commentId = 123L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);

        Comment comment = Comment.builder()
                .commentId(commentId)
                .user(user)
                .commentContent("삭제할 댓글")
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        CommentSimpleResponse response = commentService.deleteComment(userId, commentId);

        // then
        assertEquals("댓글이 성공적으로 삭제되었습니다.", response.getMessage());
        verify(commentRepository).findById(commentId);
        verify(commentRepository).delete(comment);

        log.info("테스트 종료: 댓글 삭제 성공");
    }

    /**
     * 삭제할 댓글이 존재하지 않으면 댓글 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 삭제 실패: 댓글을 찾을 수 없으면 예외가 발생한다.")
    void deleteComment_Fail_CommentNotFound() {
        log.info("테스트 시작: 댓글 삭제 실패 - 댓글 없음");

        // given
        UUID userId = UUID.randomUUID();
        Long commentId = 999L;
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.deleteComment(userId, commentId));

        // then
        assertEquals(CommentErrorCode.COMMENT_NOT_FOUND, exception.getErrorCode());
        verify(commentRepository).findById(commentId);

        log.info("테스트 종료: 댓글 삭제 실패 - 댓글 없음");
    }
}
