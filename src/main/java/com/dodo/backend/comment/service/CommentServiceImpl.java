package com.dodo.backend.comment.service;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentCreateRequest;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentUpdateRequest;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentCreateResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentListQueryResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentListResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentSimpleResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.MyCommentListQueryResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.MyCommentListResponse;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.comment.exception.CommentErrorCode;
import com.dodo.backend.comment.exception.CommentException;
import com.dodo.backend.comment.mapper.CommentMapper;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.notification.service.NotificationService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.dodo.backend.comment.exception.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.dodo.backend.comment.exception.CommentErrorCode.DELETE_PERMISSION_DENIED;
import static com.dodo.backend.comment.exception.CommentErrorCode.INVALID_REQUEST;
import static com.dodo.backend.comment.exception.CommentErrorCode.UPDATE_PERMISSION_DENIED;

/**
 * {@link CommentService} 구현체입니다.
 * <p>
 * 댓글 작성, 목록 조회, 수정, 삭제 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int MAX_COMMENT_LIST_SIZE = 100;

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final BoardService boardService;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * 댓글을 작성합니다.
     * <p>
     * 댓글 작성 시 게시글은 BoardService를 통해 조회하고, 작성자는 UserService를 통해 조회합니다.
     * 부모 댓글 ID가 전달되면 같은 게시글의 댓글인지 검증한 뒤 대댓글로 저장합니다.
     *
     * @param userId  요청 사용자 ID
     * @param request 댓글 작성 요청 DTO
     * @return 댓글 작성 응답 DTO
     * @throws CommentException 잘못된 요청 또는 부모 댓글이 없는 경우
     */
    @Transactional
    @Override
    public CommentCreateResponse createComment(UUID userId, CommentCreateRequest request) {
        if (userId == null || request == null || isBlank(request.getCommentContent())) {
            throw new CommentException(INVALID_REQUEST);
        }

        Board board = boardService.getBoardById(request.getBoardId());
        validateCommentableBoard(board);

        User user = userService.getUserById(userId);
        Comment parentComment = findParentComment(request.getParentCommentId(), board.getBoardId());

        Comment comment = request.toEntity(board, user, parentComment);
        Comment savedComment = commentRepository.save(comment);
        notificationService.notifyCommentCreated(savedComment);

        return CommentCreateResponse.toDto(savedComment, "댓글이 성공적으로 작성되었습니다.");
    }

    /**
     * 특정 게시글의 댓글 목록을 조회합니다.
     *
     * @param boardId 댓글을 조회할 게시글 ID
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @return 댓글 목록 조회 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public CommentListResponse getComments(Long boardId, int page, int size) {
        validateCommentListRequest(boardId, page, size);

        Board board = boardService.getBoardById(boardId);
        validateCommentableBoard(board);

        int offset = page * size;
        List<CommentListQueryResponse> queryResponses = commentMapper.findCommentsByBoardId(boardId, offset, size);
        long totalElements = commentMapper.countCommentsByBoardId(boardId);

        return CommentListResponse.toDto(
                queryResponses,
                page,
                size,
                totalElements,
                "댓글 목록을 성공적으로 조회했습니다."
        );
    }

    /**
     * 요청 사용자가 작성한 댓글 목록을 조회합니다.
     *
     * @param userId 요청 사용자 ID
     * @param page   페이지 번호
     * @param size   페이지 크기
     * @return 내가 쓴 댓글 목록 조회 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public MyCommentListResponse getMyComments(UUID userId, int page, int size) {
        if (userId == null) {
            throw new CommentException(INVALID_REQUEST);
        }
        validateMyCommentListRequest(page, size);

        int offset = page * size;
        List<MyCommentListQueryResponse> queryResponses = commentMapper.findMyComments(userId, offset, size);
        long totalElements = commentMapper.countMyComments(userId);

        return MyCommentListResponse.toDto(
                queryResponses,
                page,
                size,
                totalElements,
                "내가 쓴 댓글 목록을 성공적으로 조회했습니다."
        );
    }

    /**
     * 특정 댓글을 수정합니다.
     *
     * @param userId    요청 사용자 ID
     * @param commentId 수정할 댓글 ID
     * @param request   댓글 수정 요청 DTO
     * @return 댓글 수정 응답 DTO
     * @throws CommentException 잘못된 요청, 댓글 없음, 수정 권한 없음인 경우
     */
    @Transactional
    @Override
    public CommentSimpleResponse updateComment(UUID userId, Long commentId, CommentUpdateRequest request) {
        if (userId == null || request == null || isBlank(request.getCommentContent())) {
            throw new CommentException(INVALID_REQUEST);
        }

        Comment comment = findCommentById(commentId);
        validateCommentOwner(userId, comment, UPDATE_PERMISSION_DENIED);

        commentMapper.updateComment(commentId, request.getCommentContent());

        return CommentSimpleResponse.toDto("댓글이 성공적으로 수정되었습니다.");
    }

    /**
     * 특정 댓글을 삭제합니다.
     * <p>
     * 댓글 삭제는 기본 삭제 작업이므로 JPA Repository를 사용합니다.
     *
     * @param userId    요청 사용자 ID
     * @param commentId 삭제할 댓글 ID
     * @return 댓글 삭제 응답 DTO
     * @throws CommentException 잘못된 요청, 댓글 없음, 삭제 권한 없음인 경우
     */
    @Transactional
    @Override
    public CommentSimpleResponse deleteComment(UUID userId, Long commentId) {
        if (userId == null) {
            throw new CommentException(INVALID_REQUEST);
        }

        Comment comment = findCommentById(commentId);
        validateCommentOwner(userId, comment, DELETE_PERMISSION_DENIED);

        commentRepository.delete(comment);

        return CommentSimpleResponse.toDto("댓글이 성공적으로 삭제되었습니다.");
    }

    /**
     * 댓글 ID로 댓글을 조회하고 ID 유효성을 검증합니다.
     *
     * @param commentId 조회할 댓글 ID
     * @return 댓글 엔티티
     */
    private Comment findCommentById(Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new CommentException(INVALID_REQUEST);
        }

        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(COMMENT_NOT_FOUND));
    }

    /**
     * 부모 댓글을 조회하고 같은 게시글의 댓글인지 검증합니다.
     *
     * @param parentCommentId 부모 댓글 ID
     * @param boardId         게시글 ID
     * @return 부모 댓글 또는 null
     */
    private Comment findParentComment(Long parentCommentId, Long boardId) {
        if (parentCommentId == null) {
            return null;
        }

        Comment parentComment = findCommentById(parentCommentId);
        if (parentComment.getBoard() == null || !boardId.equals(parentComment.getBoard().getBoardId())) {
            throw new CommentException(INVALID_REQUEST);
        }

        return parentComment;
    }

    /**
     * 댓글 작성 또는 조회 가능한 게시글인지 검증합니다.
     *
     * @param board 검증할 게시글
     */
    private void validateCommentableBoard(Board board) {
        if (board == null || board.getBoardStatus() != BoardStatus.PUBLISHED) {
            throw new CommentException(INVALID_REQUEST);
        }
    }

    /**
     * 댓글 목록 조회 요청 값을 검증합니다.
     *
     * @param boardId 게시글 ID
     * @param page    페이지 번호
     * @param size    페이지 크기
     */
    private void validateCommentListRequest(Long boardId, int page, int size) {
        if (boardId == null || boardId <= 0 || page < 0 || size <= 0 || size > MAX_COMMENT_LIST_SIZE || page > Integer.MAX_VALUE / size) {
            throw new CommentException(INVALID_REQUEST);
        }
    }

    /**
     * 내가 쓴 댓글 목록 조회 요청 값을 검증합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     */
    private void validateMyCommentListRequest(int page, int size) {
        if (page < 0 || size <= 0 || size > MAX_COMMENT_LIST_SIZE || page > Integer.MAX_VALUE / size) {
            throw new CommentException(INVALID_REQUEST);
        }
    }

    /**
     * 댓글 작성자인지 검증합니다.
     *
     * @param userId    요청 사용자 ID
     * @param comment   검증할 댓글
     * @param errorCode 권한이 없을 때 발생시킬 에러 코드
     */
    private void validateCommentOwner(UUID userId, Comment comment, CommentErrorCode errorCode) {
        if (comment.getUser() == null || !userId.equals(comment.getUser().getUsersId())) {
            throw new CommentException(errorCode);
        }
    }

    /**
     * 문자열이 null이거나 공백인지 확인합니다.
     *
     * @param value 확인할 문자열
     * @return null 또는 공백이면 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
