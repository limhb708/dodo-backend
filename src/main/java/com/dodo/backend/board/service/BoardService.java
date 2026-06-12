package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardTempSaveRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.dto.response.BoardResponse.BoardDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardSimpleResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveResponse;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.exception.BoardException;

import java.util.UUID;

/**
 * 게시글(Board) 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface BoardService {

    /**
     * 게시글 ID로 게시글 엔티티를 조회합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 조회된 게시글 엔티티
     * @throws BoardException 게시글이 존재하지 않는 경우
     */
    Board getBoardById(Long boardId);

    /**
     * 새 게시글을 생성하고 이미지 URL 목록을 게시글에 연결합니다.
     *
     * @param userId  게시글 작성자 ID
     * @param request 게시글 생성 요청 DTO
     * @return 생성된 게시글 ID
     */
    Long createBoard(UUID userId, BoardCreateRequest request);

    /**
     * 특정 게시글의 상세 정보를 조회합니다.
     * <p>
     * 작성자가 아닌 사용자가 조회하면 조회수가 1 증가합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 조회할 게시글 ID
     * @return 게시글 상세 조회 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 조회 권한 없음인 경우
     */
    BoardDetailResponse getBoardDetail(UUID userId, Long boardId);

    /**
     * 특정 게시글의 제목, 내용, 이미지 목록을 수정합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 수정할 게시글 ID
     * @param request 게시글 수정 요청 DTO
     * @return 게시글 수정 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 수정 권한 없음인 경우
     */
    BoardSimpleResponse updateBoard(UUID userId, Long boardId, BoardUpdateRequest request);

    /**
     * 수정 중인 게시글 내용을 Redis에 임시 저장합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 임시 저장 대상 게시글 ID
     * @param request 게시글 임시 저장 요청 DTO
     * @return 임시 저장 세션 키가 포함된 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 임시 저장 권한 없음인 경우
     */
    BoardTempSaveResponse tempSaveBoard(UUID userId, Long boardId, BoardTempSaveRequest request);

    /**
     * Redis에 임시 저장된 게시글 내용을 조회합니다.
     *
     * @param userId     요청 사용자 ID
     * @param sessionKey 임시 저장 데이터의 세션 키
     * @return 임시 저장된 게시글 내용 응답 DTO
     * @throws BoardException 잘못된 요청, 세션 키 없음, 조회 권한 없음인 경우
     */
    BoardTempSaveDetailResponse getTempSavedBoard(UUID userId, String sessionKey);

    /**
     * 특정 게시글을 삭제 상태로 변경하고 연결된 이미지 정보를 삭제합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 삭제할 게시글 ID
     * @return 게시글 삭제 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 삭제 권한 없음인 경우
     */
    BoardSimpleResponse deleteBoard(UUID userId, Long boardId);
}
