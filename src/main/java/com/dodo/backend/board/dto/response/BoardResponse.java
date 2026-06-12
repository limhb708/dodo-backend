package com.dodo.backend.board.dto.response;

import com.dodo.backend.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "게시글 응답 DTO 그룹")
public class BoardResponse {

    /**
     * 게시글 생성 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "게시글 생성 응답")
    public static class BoardCreateResponse {

        @Schema(description = "응답 메시지", example = "게시글이 성공적으로 작성되었습니다.")
        private String message;

        @Schema(description = "생성된 게시글 ID", example = "1")
        private Long boardId;

        /**
         * 게시글 생성 응답 DTO를 생성합니다.
         *
         * @param boardId 생성된 게시글 ID
         * @param message 응답 메시지
         * @return 게시글 생성 응답 DTO
         */
        public static BoardCreateResponse toDto(Long boardId, String message) {
            return BoardCreateResponse.builder()
                    .message(message)
                    .boardId(boardId)
                    .build();
        }
    }

    /**
     * 게시글 상세 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 상세 조회 응답")
    public static class BoardDetailResponse {

        @Schema(description = "응답 메시지", example = "게시글 상세 조회에 성공했습니다.")
        private String message;

        @Schema(description = "게시글 ID", example = "123")
        private Long boardId;

        @Schema(description = "게시글 제목", example = "우리 강아지 자랑합니다")
        private String boardTitle;

        @Schema(description = "게시글 내용", example = "오늘 산책하다 찍은 사진이에요.")
        private String boardContent;

        @Schema(description = "게시글 이미지 URL 목록", example = "[\"https://example.com/images/bori_1.jpg\", \"https://example.com/images/bori_2.jpg\"]")
        private List<String> imageFileUrls;

        @Schema(description = "작성자 닉네임", example = "자유로운산책")
        private String nickname;

        @Schema(description = "조회수", example = "51")
        private Integer viewCount;

        @Schema(description = "게시글 생성 일시", example = "2025-10-06T10:00:00")
        private LocalDateTime boardCreatedAt;

        @Schema(description = "게시글 수정 일시", example = "2025-10-06T10:30:00")
        private LocalDateTime modifiedAt;

        /**
         * 게시글 엔티티와 이미지 URL 목록을 상세 조회 응답 DTO로 변환합니다.
         *
         * @param board         게시글 엔티티
         * @param imageFileUrls 게시글 이미지 URL 목록
         * @param message       응답 메시지
         * @return 게시글 상세 조회 응답 DTO
         */
        public static BoardDetailResponse toDto(Board board, List<String> imageFileUrls, String message) {
            return toDto(board, imageFileUrls, message, board.getViewCount());
        }

        /**
         * 게시글 엔티티와 응답에 표시할 조회수를 상세 조회 응답 DTO로 변환합니다.
         *
         * @param board         게시글 엔티티
         * @param imageFileUrls 게시글 이미지 URL 목록
         * @param message       응답 메시지
         * @param viewCount     응답에 표시할 조회수
         * @return 게시글 상세 조회 응답 DTO
         */
        public static BoardDetailResponse toDto(Board board, List<String> imageFileUrls, String message, Integer viewCount) {
            return BoardDetailResponse.builder()
                    .message(message)
                    .boardId(board.getBoardId())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .imageFileUrls(imageFileUrls)
                    .nickname(board.getUser().getNickname())
                    .viewCount(viewCount)
                    .boardCreatedAt(board.getBoardCreatedAt())
                    .modifiedAt(board.getModifiedAt())
                    .build();
        }
    }

    /**
     * 게시글 수정, 삭제처럼 메시지만 반환하는 단순 처리 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 단순 처리 응답")
    public static class BoardSimpleResponse {

        @Schema(description = "응답 메시지", example = "게시글이 성공적으로 수정되었습니다.")
        private String message;

        /**
         * 게시글 단순 처리 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @return 게시글 단순 처리 응답 DTO
         */
        public static BoardSimpleResponse toDto(String message) {
            return BoardSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 게시글 임시 저장 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 임시 저장 응답")
    public static class BoardTempSaveResponse {

        @Schema(description = "응답 메시지", example = "게시글이 성공적으로 임시 저장되었습니다.")
        private String message;

        @Schema(description = "Redis에 저장된 임시 데이터의 고유 키", example = "b1a2c3d4-e5f6-7g8h-i9j0-k1l2m3n4o5p6")
        private String sessionKey;

        /**
         * 게시글 임시 저장 응답 DTO를 생성합니다.
         *
         * @param sessionKey Redis에 저장된 임시 데이터의 고유 키
         * @param message    응답 메시지
         * @return 게시글 임시 저장 응답 DTO
         */
        public static BoardTempSaveResponse toDto(String sessionKey, String message) {
            return BoardTempSaveResponse.builder()
                    .message(message)
                    .sessionKey(sessionKey)
                    .build();
        }
    }

    /**
     * Redis에 임시 저장된 게시글 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "임시 저장 게시글 조회 응답")
    public static class BoardTempSaveDetailResponse {

        @Schema(description = "응답 메시지", example = "임시 저장된 게시글을 성공적으로 불러왔습니다.")
        private String message;

        @Schema(description = "임시 저장 게시글 제목", example = "임시 저장 제목")
        private String boardTitle;

        @Schema(description = "임시 저장 게시글 내용", example = "임시 저장 내용")
        private String boardContent;

        @Schema(description = "임시 저장 게시글 이미지 URL", example = "https://example.com/images/bori.jpg")
        private String imageFileUrl;
    }
}
