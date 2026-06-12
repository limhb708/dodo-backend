package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardTempSaveRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.dto.response.BoardResponse.BoardDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardSimpleResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveResponse;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.exception.BoardException;
import com.dodo.backend.board.mapper.BoardMapper;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dodo.backend.board.exception.BoardErrorCode.BOARD_NOT_FOUND;
import static com.dodo.backend.board.exception.BoardErrorCode.DELETE_PERMISSION_DENIED;
import static com.dodo.backend.board.exception.BoardErrorCode.INVALID_REQUEST;
import static com.dodo.backend.board.exception.BoardErrorCode.TEMP_SAVE_NOT_FOUND;
import static com.dodo.backend.board.exception.BoardErrorCode.TEMP_SAVE_PERMISSION_DENIED;
import static com.dodo.backend.board.exception.BoardErrorCode.UPDATE_PERMISSION_DENIED;
import static com.dodo.backend.board.exception.BoardErrorCode.VIEW_PERMISSION_DENIED;

/**
 * {@link BoardService} 구현체입니다.
 * <p>
 * 게시글 생성, 상세 조회, 수정, 삭제와 Redis 기반 임시 저장 기능을 처리합니다.
 * 게시글 본문 데이터는 JPA Repository와 MyBatis Mapper를 함께 사용해 관리하고,
 * 게시글 이미지 데이터는 {@link ImageFileService}에 위임합니다.
 */
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    /**
     * Redis에 임시 저장 게시글 데이터를 저장할 때 사용하는 키 접두사입니다.
     */
    private static final String TEMP_SAVE_KEY_PREFIX = "board:temp-save:";

    /**
     * 임시 저장 데이터의 Redis 유지 기간입니다.
     */
    private static final long TEMP_SAVE_TTL_DAYS = 7L;

    /**
     * 게시글 저장 및 단건 조회를 처리하는 JPA Repository입니다.
     */
    private final BoardRepository boardRepository;

    /**
     * 사용자 엔티티 조회를 처리하는 서비스입니다.
     */
    private final UserService userService;

    /**
     * 게시글 이미지 URL 저장, 조회, 교체, 삭제를 처리하는 서비스입니다.
     */
    private final ImageFileService imageFileService;

    /**
     * 게시글 수정, 삭제, 조회수 증가처럼 동적 SQL이 필요한 작업을 처리하는 MyBatis Mapper입니다.
     */
    private final BoardMapper boardMapper;

    /**
     * 게시글 임시 저장 데이터를 Redis에 저장하고 조회하기 위한 Template입니다.
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 게시글 ID로 게시글 엔티티를 조회합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     * @throws BoardException 게시글이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    @Override
    public Board getBoardById(Long boardId) {

        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BOARD_NOT_FOUND));
    }

    /**
     * 새 게시글을 생성하고 요청에 포함된 이미지 URL 목록을 게시글에 연결합니다.
     *
     * @param userId  게시글 작성자 ID
     * @param request 게시글 생성 요청 DTO
     * @return 생성된 게시글 ID
     */
    @Override
    @Transactional
    public Long createBoard(UUID userId, BoardCreateRequest request) {

        User user = userService.getUserById(userId);

        Board board = request.toEntity(user);

        Board savedBoard = boardRepository.save(board);

        imageFileService.saveBoardImages(savedBoard, request.getImageFileUrls());

        return savedBoard.getBoardId();
    }

    /**
     * 특정 게시글의 상세 정보를 조회합니다.
     * <p>
     * 삭제되었거나 공개 상태가 아닌 게시글은 조회할 수 없습니다.
     * 작성자가 아닌 사용자가 조회하면 조회수를 1 증가시키고, 증가된 조회수를 응답에 반영합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 조회할 게시글 ID
     * @return 게시글 상세 조회 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 조회 권한 없음인 경우
     */
    @Override
    @Transactional
    public BoardDetailResponse getBoardDetail(UUID userId, Long boardId) {
        Board board = findBoardById(boardId);

        if (board.getBoardStatus() != BoardStatus.PUBLISHED) {
            throw new BoardException(VIEW_PERMISSION_DENIED);
        }

        Integer responseViewCount = board.getViewCount();
        if (!isBoardOwner(userId, board)) {
            boardMapper.increaseViewCount(boardId);
            responseViewCount = responseViewCount == null ? 1 : responseViewCount + 1;
        }

        var imageFileUrls = imageFileService.getBoardImageUrls(boardId);

        return BoardDetailResponse.toDto(board, imageFileUrls, "게시글 상세 조회에 성공했습니다.", responseViewCount);
    }

    /**
     * 특정 게시글의 제목, 본문, 이미지 목록을 수정합니다.
     * <p>
     * 게시글 작성자만 수정할 수 있으며 삭제된 게시글은 수정할 수 없습니다.
     * 제목과 본문은 값이 전달된 필드만 수정하고, 이미지 URL 목록이 전달되면 기존 이미지를 대체합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 수정할 게시글 ID
     * @param request 게시글 수정 요청 DTO
     * @return 게시글 수정 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 수정 권한 없음인 경우
     */
    @Override
    @Transactional
    public BoardSimpleResponse updateBoard(UUID userId, Long boardId, BoardUpdateRequest request) {
        if (request == null) {
            throw new BoardException(INVALID_REQUEST);
        }

        Board board = findBoardById(boardId);
        validateBoardOwner(userId, board, UPDATE_PERMISSION_DENIED);

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            throw new BoardException(UPDATE_PERMISSION_DENIED);
        }

        boolean hasBoardUpdateFields = hasBoardTableUpdateFields(request);
        boolean hasImageUpdateFields = hasImageUpdateFields(request);

        if (!hasBoardUpdateFields && !hasImageUpdateFields) {
            throw new BoardException(INVALID_REQUEST);
        }

        if (hasBoardUpdateFields) {
            boardMapper.updateBoard(boardId, request);
        }

        imageFileService.replaceBoardImages(board, request.getImageFileUrls());

        return BoardSimpleResponse.toDto("게시글이 성공적으로 수정되었습니다.");
    }

    /**
     * 수정 중인 게시글 내용을 Redis에 임시 저장합니다.
     * <p>
     * 게시글 작성자만 임시 저장할 수 있으며 삭제된 게시글은 임시 저장할 수 없습니다.
     * 저장 데이터에는 사용자 ID를 함께 보관하여 조회 시 세션 키 소유자를 검증합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 임시 저장 대상 게시글 ID
     * @param request 게시글 임시 저장 요청 DTO
     * @return 임시 저장 세션 키가 포함된 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 임시 저장 권한 없음인 경우
     */
    @Transactional
    @Override
    public BoardTempSaveResponse tempSaveBoard(UUID userId, Long boardId, BoardTempSaveRequest request) {
        if (request == null || !hasTempSaveFields(request)) {
            throw new BoardException(INVALID_REQUEST);
        }

        Board board = findBoardById(boardId);
        validateBoardOwner(userId, board, TEMP_SAVE_PERMISSION_DENIED);

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            throw new BoardException(TEMP_SAVE_PERMISSION_DENIED);
        }

        String sessionKey = UUID.randomUUID().toString();
        Map<String, Object> tempSaveData = new HashMap<>();
        tempSaveData.put("userId", userId.toString());
        tempSaveData.put("boardId", boardId);
        tempSaveData.put("boardTitle", request.getBoardTitle());
        tempSaveData.put("boardContent", request.getBoardContent());
        tempSaveData.put("imageFileUrl", request.getImageFileUrl());

        redisTemplate.opsForValue().set(
                TEMP_SAVE_KEY_PREFIX + sessionKey,
                tempSaveData,
                TEMP_SAVE_TTL_DAYS,
                TimeUnit.DAYS
        );

        return BoardTempSaveResponse.toDto(sessionKey, "게시글이 성공적으로 임시 저장되었습니다.");
    }

    /**
     * Redis에 임시 저장된 게시글 내용을 조회합니다.
     * <p>
     * 세션 키가 존재하지 않으면 예외를 발생시키고,
     * 저장된 사용자 ID와 요청 사용자 ID가 다르면 권한 없음 예외를 발생시킵니다.
     *
     * @param userId     요청 사용자 ID
     * @param sessionKey Redis에 저장된 임시 데이터의 세션 키
     * @return 임시 저장된 게시글 내용 응답 DTO
     * @throws BoardException 잘못된 요청, 세션 키 없음, 조회 권한 없음인 경우
     */
    @Transactional(readOnly = true)
    @Override
    public BoardTempSaveDetailResponse getTempSavedBoard(UUID userId, String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new BoardException(INVALID_REQUEST);
        }

        Object savedData = redisTemplate.opsForValue().get(TEMP_SAVE_KEY_PREFIX + sessionKey);
        if (!(savedData instanceof Map<?, ?> tempSaveData)) {
            throw new BoardException(TEMP_SAVE_NOT_FOUND);
        }

        String savedUserId = toNullableString(tempSaveData.get("userId"));
        if (userId == null || !userId.toString().equals(savedUserId)) {
            throw new BoardException(TEMP_SAVE_PERMISSION_DENIED);
        }

        return BoardTempSaveDetailResponse.builder()
                .message("임시 저장된 게시글을 성공적으로 불러왔습니다.")
                .boardTitle(toNullableString(tempSaveData.get("boardTitle")))
                .boardContent(toNullableString(tempSaveData.get("boardContent")))
                .imageFileUrl(toNullableString(tempSaveData.get("imageFileUrl")))
                .build();
    }

    /**
     * 특정 게시글을 삭제 상태로 변경하고 연결된 이미지 정보를 삭제합니다.
     * <p>
     * 게시글 작성자만 삭제할 수 있으며 이미 삭제된 게시글은 다시 삭제할 수 없습니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 삭제할 게시글 ID
     * @return 게시글 삭제 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 삭제 권한 없음인 경우
     */
    @Transactional
    @Override
    public BoardSimpleResponse deleteBoard(UUID userId, Long boardId) {
        Board board = findBoardById(boardId);
        validateBoardOwner(userId, board, DELETE_PERMISSION_DENIED);

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            throw new BoardException(DELETE_PERMISSION_DENIED);
        }

        boardMapper.deleteBoard(boardId, BoardStatus.DELETED.name());
        imageFileService.deleteBoardImages(boardId);

        return BoardSimpleResponse.toDto("게시글이 성공적으로 삭제되었습니다.");
    }

    /**
     * 게시글 ID의 유효성을 검증한 뒤 게시글 엔티티를 조회합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     * @throws BoardException 게시글 ID가 null 또는 0 이하이거나 게시글이 존재하지 않는 경우
     */
    private Board findBoardById(Long boardId) {
        if (boardId == null || boardId <= 0) {
            throw new BoardException(INVALID_REQUEST);
        }

        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BOARD_NOT_FOUND));
    }

    /**
     * 요청 사용자가 게시글 작성자인지 검증합니다.
     *
     * @param userId    요청 사용자 ID
     * @param board     권한을 검증할 게시글
     * @param errorCode 작성자가 아닐 때 발생시킬 에러 코드
     * @throws BoardException 요청 사용자가 게시글 작성자가 아닌 경우
     */
    private void validateBoardOwner(UUID userId, Board board, com.dodo.backend.board.exception.BoardErrorCode errorCode) {
        if (userId == null || board.getUser() == null || !userId.equals(board.getUser().getUsersId())) {
            throw new BoardException(errorCode);
        }
    }

    /**
     * 게시글 테이블에 반영할 수정 필드가 있는지 확인합니다.
     * <p>
     * 실제 컬럼 반영 여부는 MyBatis Mapper XML의 동적 update 문에서 최종 결정됩니다.
     *
     * @param request 게시글 수정 요청 DTO
     * @return 제목 또는 본문 수정 값이 하나 이상 전달되었으면 {@code true}
     */
    private boolean hasBoardTableUpdateFields(BoardUpdateRequest request) {
        return request.getBoardTitle() != null
                || request.getBoardContent() != null;
    }

    /**
     * 게시글 이미지 목록 수정 요청이 전달되었는지 확인합니다.
     * <p>
     * {@code null}은 이미지 미수정, 빈 목록은 전체 이미지 삭제 요청으로 처리합니다.
     *
     * @param request 게시글 수정 요청 DTO
     * @return 이미지 URL 목록 필드가 전달되었으면 {@code true}
     */
    private boolean hasImageUpdateFields(BoardUpdateRequest request) {
        return request.getImageFileUrls() != null;
    }

    /**
     * 임시 저장 요청에 저장할 값이 하나 이상 포함되어 있는지 확인합니다.
     *
     * @param request 게시글 임시 저장 요청 DTO
     * @return 제목, 본문, 이미지 URL 중 하나 이상 전달되었으면 {@code true}
     */
    private boolean hasTempSaveFields(BoardTempSaveRequest request) {
        return request.getBoardTitle() != null
                || request.getBoardContent() != null
                || request.getImageFileUrl() != null;
    }

    /**
     * Redis에서 조회한 값을 문자열로 변환합니다.
     *
     * @param value Redis에서 조회한 값
     * @return 값이 null이면 null, 아니면 문자열 표현
     */
    private String toNullableString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 요청 사용자가 게시글 작성자인지 확인합니다.
     *
     * @param userId 요청 사용자 ID
     * @param board  작성자 여부를 확인할 게시글
     * @return 요청 사용자가 게시글 작성자이면 {@code true}
     */
    private boolean isBoardOwner(UUID userId, Board board) {
        return userId != null && board.getUser() != null && userId.equals(board.getUser().getUsersId());
    }
}
