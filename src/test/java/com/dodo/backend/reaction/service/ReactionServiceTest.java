package com.dodo.backend.reaction.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.service.ActivityHistoryService;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.notification.service.NotificationService;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.reaction.dto.request.ReactionRequest.BoardReactionCreateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.BoardReactionUpdateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionCreateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionUpdateRequest;
import com.dodo.backend.reaction.dto.response.ReactionResponse.ReactionSimpleResponse;
import com.dodo.backend.reaction.entity.Reaction;
import com.dodo.backend.reaction.mapper.ReactionMapper;
import com.dodo.backend.reaction.exception.ReactionErrorCode;
import com.dodo.backend.reaction.exception.ReactionException;
import com.dodo.backend.reaction.repository.ReactionRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link ReactionServiceImpl}의 활동 반응 추가 로직을 검증하는 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @InjectMocks
    private ReactionServiceImpl reactionService;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private ReactionMapper reactionMapper;

    @Mock
    private ActivityHistoryService activityHistoryService;

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    /**
     * 활동 반응 추가가 정상적으로 저장되고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 추가 성공: 다른 사용자의 활동 기록에 반응을 저장한다.")
    void createHistoryReaction_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Long historyId = 101L;

        User requester = User.builder().usersId(userId).build();
        User owner = User.builder().usersId(ownerId).build();
        Pet pet = Pet.builder().petId(1L).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .user(owner)
                .pet(pet)
                .build();

        HistoryReactionCreateRequest request = HistoryReactionCreateRequest.builder()
                .historyId(historyId)
                .reactionType("LIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(activityHistoryService.getActivityHistoryById(historyId)).willReturn(history);
        given(reactionRepository.existsByUserAndHistory(requester, history)).willReturn(false);
        given(reactionRepository.save(any(Reaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ReactionSimpleResponse response = reactionService.createHistoryReaction(userId, request);

        // then
        assertNotNull(response);
        assertEquals("반응이 성공적으로 추가되었습니다.", response.getMessage());
        verify(reactionRepository, times(1)).save(any(Reaction.class));
        verify(notificationService).notifyReactionCreated(any(Reaction.class));
    }

    /**
     * 요청값이 비어있는 경우 INVALID_REQUEST 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 추가 실패: 요청값이 null이면 INVALID_REQUEST 예외가 발생한다.")
    void createHistoryReaction_Fail_InvalidRequest() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.createHistoryReaction(userId, null)
        );

        // then
        assertEquals(ReactionErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 자신의 활동 기록에 반응을 시도하면 ACCESS_DENIED 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 추가 실패: 본인 활동 기록에는 반응할 수 없다.")
    void createHistoryReaction_Fail_AccessDeniedForOwnHistory() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 101L;

        User requester = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(1L).build();
        ActivityHistory ownHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(requester)
                .pet(pet)
                .build();

        HistoryReactionCreateRequest request = HistoryReactionCreateRequest.builder()
                .historyId(historyId)
                .reactionType("LIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(activityHistoryService.getActivityHistoryById(historyId)).willReturn(ownHistory);

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.createHistoryReaction(userId, request)
        );

        // then
        assertEquals(ReactionErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    /**
     * 동일 사용자/활동에 반응 이력이 이미 존재하면 CONFLICT 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 추가 실패: 이미 반응을 누른 활동이면 CONFLICT 예외가 발생한다.")
    void createHistoryReaction_Fail_AlreadyReacted() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Long historyId = 101L;

        User requester = User.builder().usersId(userId).build();
        User owner = User.builder().usersId(ownerId).build();
        Pet pet = Pet.builder().petId(1L).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .user(owner)
                .pet(pet)
                .build();

        HistoryReactionCreateRequest request = HistoryReactionCreateRequest.builder()
                .historyId(historyId)
                .reactionType("DISLIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(activityHistoryService.getActivityHistoryById(historyId)).willReturn(history);
        given(reactionRepository.existsByUserAndHistory(requester, history)).willReturn(true);

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.createHistoryReaction(userId, request)
        );

        // then
        assertEquals(ReactionErrorCode.ACTIVITY_REACTION_ALREADY_EXISTS, exception.getErrorCode());
    }

    /**
     * 활동 기록이 없을 때 활동 도메인 예외가 전파되는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 추가 실패: 활동 기록이 없으면 HISTORY_NOT_FOUND 예외가 전파된다.")
    void createHistoryReaction_Fail_HistoryNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;

        User requester = User.builder().usersId(userId).build();
        HistoryReactionCreateRequest request = HistoryReactionCreateRequest.builder()
                .historyId(historyId)
                .reactionType("LIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(activityHistoryService.getActivityHistoryById(historyId))
                .willThrow(new ActivityHistoryException(ActivityHistoryErrorCode.HISTORY_NOT_FOUND));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                reactionService.createHistoryReaction(userId, request)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 활동 반응 변경이 정상적으로 수행되고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 변경 성공: 기존 반응 이력이 있으면 반응 유형을 변경한다.")
    void updateHistoryReaction_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Long historyId = 101L;

        User requester = User.builder().usersId(userId).build();
        User owner = User.builder().usersId(ownerId).build();
        Pet pet = Pet.builder().petId(1L).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .user(owner)
                .pet(pet)
                .build();

        HistoryReactionUpdateRequest request = HistoryReactionUpdateRequest.builder()
                .reactionType("DISLIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(activityHistoryService.getActivityHistoryById(historyId)).willReturn(history);
        given(reactionRepository.existsByUserAndHistory(requester, history)).willReturn(true);
        given(reactionMapper.updateHistoryReactionType(userId, historyId, "DISLIKE")).willReturn(1);

        // when
        ReactionSimpleResponse response = reactionService.updateHistoryReaction(userId, historyId, request);

        // then
        assertNotNull(response);
        assertEquals("반응이 성공적으로 변경되었습니다.", response.getMessage());
        verify(reactionMapper, times(1)).updateHistoryReactionType(userId, historyId, "DISLIKE");
    }

    /**
     * 반응 변경 요청이 null인 경우 INVALID_REQUEST 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 변경 실패: 요청값이 null이면 INVALID_REQUEST 예외가 발생한다.")
    void updateHistoryReaction_Fail_InvalidRequest() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.updateHistoryReaction(userId, 101L, null)
        );

        // then
        assertEquals(ReactionErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 기존 반응 이력이 없으면 ACTIVITY_REACTION_NOT_FOUND 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 변경 실패: 반응 이력이 없으면 ACTIVITY_REACTION_NOT_FOUND 예외가 발생한다.")
    void updateHistoryReaction_Fail_ReactionNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Long historyId = 101L;

        User requester = User.builder().usersId(userId).build();
        User owner = User.builder().usersId(ownerId).build();
        Pet pet = Pet.builder().petId(1L).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .user(owner)
                .pet(pet)
                .build();

        HistoryReactionUpdateRequest request = HistoryReactionUpdateRequest.builder()
                .reactionType("LIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(activityHistoryService.getActivityHistoryById(historyId)).willReturn(history);
        given(reactionRepository.existsByUserAndHistory(requester, history)).willReturn(false);

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.updateHistoryReaction(userId, historyId, request)
        );

        // then
        assertEquals(ReactionErrorCode.ACTIVITY_REACTION_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 본인 활동 기록에 반응 변경을 시도하면 ACCESS_DENIED 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 변경 실패: 본인 활동 기록이면 ACCESS_DENIED 예외가 발생한다.")
    void updateHistoryReaction_Fail_AccessDeniedForOwnHistory() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 101L;

        User requester = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(1L).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .user(requester)
                .pet(pet)
                .build();

        HistoryReactionUpdateRequest request = HistoryReactionUpdateRequest.builder()
                .reactionType("LIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(activityHistoryService.getActivityHistoryById(historyId)).willReturn(history);

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.updateHistoryReaction(userId, historyId, request)
        );

        // then
        assertEquals(ReactionErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    /**
     * 활동 반응 취소가 정상적으로 수행되고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 취소 성공: 기존 반응 이력이 있으면 반응을 삭제한다.")
    void cancelHistoryReaction_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 101L;

        Reaction reaction = Reaction.builder().reactionId(1L).build();

        given(reactionRepository.findByUser_UsersIdAndHistory_HistoryId(userId, historyId))
                .willReturn(Optional.of(reaction));

        // when
        ReactionSimpleResponse response = reactionService.cancelHistoryReaction(userId, historyId);

        // then
        assertNotNull(response);
        assertEquals("반응이 성공적으로 취소되었습니다.", response.getMessage());
        verify(reactionRepository, times(1)).delete(reaction);
    }

    /**
     * 취소 대상 반응 이력이 없으면 ACTIVITY_REACTION_NOT_FOUND 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 취소 실패: 반응 이력이 없으면 ACTIVITY_REACTION_NOT_FOUND 예외가 발생한다.")
    void cancelHistoryReaction_Fail_ReactionNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 101L;

        given(reactionRepository.findByUser_UsersIdAndHistory_HistoryId(userId, historyId))
                .willReturn(Optional.empty());

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.cancelHistoryReaction(userId, historyId)
        );

        // then
        assertEquals(ReactionErrorCode.ACTIVITY_REACTION_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 취소 요청의 historyId가 null이면 INVALID_REQUEST 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("활동 반응 취소 실패: historyId가 null이면 INVALID_REQUEST 예외가 발생한다.")
    void cancelHistoryReaction_Fail_InvalidRequest() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        ReactionException exception = assertThrows(ReactionException.class, () ->
                reactionService.cancelHistoryReaction(userId, null)
        );

        // then
        assertEquals(ReactionErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 게시물 반응 추가가 정상적으로 저장되고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시물 반응 추가 성공: 다른 사용자의 게시물에 반응을 저장한다.")
    void createBoardReaction_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Long boardId = 101L;

        User requester = User.builder().usersId(userId).build();
        User owner = User.builder().usersId(ownerId).build();
        Board board = Board.builder()
                .boardId(boardId)
                .user(owner)
                .build();

        BoardReactionCreateRequest request = BoardReactionCreateRequest.builder()
                .boardId(boardId)
                .reactionType("LIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(boardService.getBoardById(boardId)).willReturn(board);
        given(reactionRepository.existsByUserAndBoard(requester, board)).willReturn(false);
        given(reactionRepository.save(any(Reaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ReactionSimpleResponse response = reactionService.createBoardReaction(userId, request);

        // then
        assertNotNull(response);
        assertEquals("반응이 성공적으로 추가되었습니다.", response.getMessage());
        verify(reactionRepository, times(1)).save(any(Reaction.class));
        verify(notificationService).notifyReactionCreated(any(Reaction.class));
    }

    /**
     * 게시물 반응 변경이 정상적으로 수행되고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시물 반응 변경 성공: 기존 반응 이력이 있으면 반응 유형을 변경한다.")
    void updateBoardReaction_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Long boardId = 101L;

        User requester = User.builder().usersId(userId).build();
        User owner = User.builder().usersId(ownerId).build();
        Board board = Board.builder()
                .boardId(boardId)
                .user(owner)
                .build();

        BoardReactionUpdateRequest request = BoardReactionUpdateRequest.builder()
                .reactionType("DISLIKE")
                .build();

        given(userService.getUserById(userId)).willReturn(requester);
        given(boardService.getBoardById(boardId)).willReturn(board);
        given(reactionRepository.existsByUserAndBoard(requester, board)).willReturn(true);
        given(reactionMapper.updateBoardReactionType(userId, boardId, "DISLIKE")).willReturn(1);

        // when
        ReactionSimpleResponse response = reactionService.updateBoardReaction(userId, boardId, request);

        // then
        assertNotNull(response);
        assertEquals("반응이 성공적으로 변경되었습니다.", response.getMessage());
        verify(reactionMapper, times(1)).updateBoardReactionType(userId, boardId, "DISLIKE");
    }

    /**
     * 게시물 반응 취소가 정상적으로 수행되고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시물 반응 취소 성공: 기존 반응 이력이 있으면 반응을 삭제한다.")
    void cancelBoardReaction_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 101L;

        Reaction reaction = Reaction.builder().reactionId(1L).build();

        given(reactionRepository.findByUser_UsersIdAndBoard_BoardId(userId, boardId))
                .willReturn(Optional.of(reaction));

        // when
        ReactionSimpleResponse response = reactionService.cancelBoardReaction(userId, boardId);

        // then
        assertNotNull(response);
        assertEquals("반응이 성공적으로 취소되었습니다.", response.getMessage());
        verify(reactionRepository, times(1)).delete(reaction);
    }
}
