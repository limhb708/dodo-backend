package com.dodo.backend.reaction.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.service.ActivityHistoryService;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.notification.service.NotificationService;
import com.dodo.backend.reaction.dto.request.ReactionRequest.BoardReactionCreateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.BoardReactionUpdateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionCreateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionUpdateRequest;
import com.dodo.backend.reaction.dto.response.ReactionResponse.ReactionSimpleResponse;
import com.dodo.backend.reaction.entity.Reaction;
import com.dodo.backend.reaction.exception.ReactionException;
import com.dodo.backend.reaction.mapper.ReactionMapper;
import com.dodo.backend.reaction.repository.ReactionRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.dodo.backend.reaction.exception.ReactionErrorCode.ACCESS_DENIED;
import static com.dodo.backend.reaction.exception.ReactionErrorCode.ACTIVITY_REACTION_ALREADY_EXISTS;
import static com.dodo.backend.reaction.exception.ReactionErrorCode.ACTIVITY_REACTION_NOT_FOUND;
import static com.dodo.backend.reaction.exception.ReactionErrorCode.BOARD_REACTION_ALREADY_EXISTS;
import static com.dodo.backend.reaction.exception.ReactionErrorCode.BOARD_REACTION_NOT_FOUND;
import static com.dodo.backend.reaction.exception.ReactionErrorCode.INVALID_REQUEST;

/**
 * {@link ReactionService} 인터페이스의 구현체로, 반응 도메인의 비즈니스 로직을 수행합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final ReactionMapper reactionMapper;
    private final ActivityHistoryService activityHistoryService;
    private final BoardService boardService;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * {@inheritDoc}
     * <p>
     * 처리 순서는 요청값 검증, 사용자/활동 조회, 접근 권한 검증, 중복 반응 검증, 반응 저장 순으로 진행됩니다.
     */
    @Transactional
    @Override
    public ReactionSimpleResponse createHistoryReaction(UUID userId, HistoryReactionCreateRequest request) {
        if (request == null || request.getHistoryId() == null || request.getReactionType() == null) {
            throw new ReactionException(INVALID_REQUEST);
        }

        User user = userService.getUserById(userId);
        ActivityHistory history = activityHistoryService.getActivityHistoryById(request.getHistoryId());

        if (history.getUser().getUsersId().equals(userId)) {
            throw new ReactionException(ACCESS_DENIED);
        }

        if (reactionRepository.existsByUserAndHistory(user, history)) {
            throw new ReactionException(ACTIVITY_REACTION_ALREADY_EXISTS);
        }

        Reaction reaction = request.toEntity(user, history);
        Reaction savedReaction = reactionRepository.save(reaction);
        notificationService.notifyReactionCreated(savedReaction);

        log.info("활동 반응 추가 완료 - User: {}, HistoryId: {}, ReactionType: {}",
                userId, request.getHistoryId(), reaction.getReactionType());

        return ReactionSimpleResponse.toDto("반응이 성공적으로 추가되었습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 처리 순서는 요청값 검증, 사용자/활동 조회, 접근 권한 검증, 반응 존재 검증, 반응 변경 순으로 진행됩니다.
     */
    @Transactional
    @Override
    public ReactionSimpleResponse updateHistoryReaction(UUID userId, Long historyId, HistoryReactionUpdateRequest request) {
        if (request == null || request.getReactionType() == null || historyId == null) {
            throw new ReactionException(INVALID_REQUEST);
        }

        User user = userService.getUserById(userId);
        ActivityHistory history = activityHistoryService.getActivityHistoryById(historyId);

        if (history.getUser().getUsersId().equals(userId)) {
            throw new ReactionException(ACCESS_DENIED);
        }

        if (!reactionRepository.existsByUserAndHistory(user, history)) {
            throw new ReactionException(ACTIVITY_REACTION_NOT_FOUND);
        }

        String reactionType = request.getReactionType().trim().toUpperCase(Locale.ROOT);
        int updatedRows = reactionMapper.updateHistoryReactionType(userId, historyId, reactionType);

        if (updatedRows == 0) {
            throw new ReactionException(ACTIVITY_REACTION_NOT_FOUND);
        }

        log.info("활동 반응 변경 완료 - User: {}, HistoryId: {}, ReactionType: {}",
                userId, historyId, reactionType);

        return ReactionSimpleResponse.toDto("반응이 성공적으로 변경되었습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 처리 순서는 요청값 검증, 반응 조회, 반응 삭제 순으로 진행됩니다.
     */
    @Transactional
    @Override
    public ReactionSimpleResponse cancelHistoryReaction(UUID userId, Long historyId) {
        if (historyId == null) {
            throw new ReactionException(INVALID_REQUEST);
        }

        Reaction reaction = reactionRepository.findByUser_UsersIdAndHistory_HistoryId(userId, historyId)
                .orElseThrow(() -> new ReactionException(ACTIVITY_REACTION_NOT_FOUND));

        reactionRepository.delete(reaction);

        log.info("활동 반응 취소 완료 - User: {}, HistoryId: {}", userId, historyId);

        return ReactionSimpleResponse.toDto("반응이 성공적으로 취소되었습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 처리 순서는 요청값 검증, 사용자/게시물 조회, 접근 권한 검증, 중복 반응 검증, 반응 저장 순으로 진행됩니다.
     */
    @Transactional
    @Override
    public ReactionSimpleResponse createBoardReaction(UUID userId, BoardReactionCreateRequest request) {
        if (request == null || request.getBoardId() == null || request.getReactionType() == null) {
            throw new ReactionException(INVALID_REQUEST);
        }

        User user = userService.getUserById(userId);
        Board board = boardService.getBoardById(request.getBoardId());

        if (board.getUser().getUsersId().equals(userId)) {
            throw new ReactionException(ACCESS_DENIED);
        }

        if (reactionRepository.existsByUserAndBoard(user, board)) {
            throw new ReactionException(BOARD_REACTION_ALREADY_EXISTS);
        }

        Reaction reaction = request.toEntity(user, board);
        Reaction savedReaction = reactionRepository.save(reaction);
        notificationService.notifyReactionCreated(savedReaction);

        log.info("게시물 반응 추가 완료 - User: {}, BoardId: {}, ReactionType: {}",
                userId, request.getBoardId(), reaction.getReactionType());

        return ReactionSimpleResponse.toDto("반응이 성공적으로 추가되었습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 처리 순서는 요청값 검증, 사용자/게시물 조회, 접근 권한 검증, 반응 존재 검증, 반응 변경 순으로 진행됩니다.
     */
    @Transactional
    @Override
    public ReactionSimpleResponse updateBoardReaction(UUID userId, Long boardId, BoardReactionUpdateRequest request) {
        if (request == null || request.getReactionType() == null || boardId == null) {
            throw new ReactionException(INVALID_REQUEST);
        }

        User user = userService.getUserById(userId);
        Board board = boardService.getBoardById(boardId);

        if (board.getUser().getUsersId().equals(userId)) {
            throw new ReactionException(ACCESS_DENIED);
        }

        if (!reactionRepository.existsByUserAndBoard(user, board)) {
            throw new ReactionException(BOARD_REACTION_NOT_FOUND);
        }

        String reactionType = request.getReactionType().trim().toUpperCase(Locale.ROOT);
        int updatedRows = reactionMapper.updateBoardReactionType(userId, boardId, reactionType);

        if (updatedRows == 0) {
            throw new ReactionException(BOARD_REACTION_NOT_FOUND);
        }

        log.info("게시물 반응 변경 완료 - User: {}, BoardId: {}, ReactionType: {}",
                userId, boardId, reactionType);

        return ReactionSimpleResponse.toDto("반응이 성공적으로 변경되었습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 처리 순서는 요청값 검증, 반응 조회, 반응 삭제 순으로 진행됩니다.
     */
    @Transactional
    @Override
    public ReactionSimpleResponse cancelBoardReaction(UUID userId, Long boardId) {
        if (boardId == null) {
            throw new ReactionException(INVALID_REQUEST);
        }

        Reaction reaction = reactionRepository.findByUser_UsersIdAndBoard_BoardId(userId, boardId)
                .orElseThrow(() -> new ReactionException(BOARD_REACTION_NOT_FOUND));

        reactionRepository.delete(reaction);

        log.info("게시물 반응 취소 완료 - User: {}, BoardId: {}", userId, boardId);

        return ReactionSimpleResponse.toDto("반응이 성공적으로 취소되었습니다.");
    }

}
