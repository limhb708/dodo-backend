package com.dodo.backend.board.dto.request;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 API에서 사용하는 요청 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "게시글 요청 DTO 그룹")
public class BoardRequest {

    /**
     * 게시글 생성 요청 DTO입니다.
     * <p>
     * 제목과 본문은 필수이며, 이미지 URL 목록은 선택적으로 전달할 수 있습니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "게시글 생성 요청")
    public static class BoardCreateRequest {

        @Schema(description = "게시글 제목", example = "우리 강아지 자랑합니다")
        @NotBlank(message = "제목은 필수입니다.")
        private String boardTitle;

        @Schema(description = "게시글 내용", example = "오늘 산책하다 찍은 사진이에요.")
        @NotBlank(message = "내용은 필수입니다.")
        private String boardContent;

        @Schema(description = "게시글 이미지 URL 목록", example = "[\"https://example.com/images/bori_1.jpg\"]")
        private List<String> imageFileUrls;

        /**
         * 게시글 생성 요청 정보를 {@link Board} 엔티티로 변환합니다.
         *
         * @param user 게시글 작성자 엔티티
         * @return 발행 상태의 게시글 엔티티
         */
        public Board toEntity(User user) {
            return Board.builder()
                    .user(user)
                    .boardTitle(this.boardTitle)
                    .boardContent(this.boardContent)
                    .boardStatus(BoardStatus.PUBLISHED)
                    .boardStatusUpdatedAt(LocalDateTime.now())
                    .boardType(BoardType.FREE)
                    .build();
        }
    }

    /**
     * 게시글 수정 요청 DTO입니다.
     * <p>
     * 전달된 필드만 수정 대상이 되며, 이미지 URL 목록이 전달되면 기존 이미지 목록을 대체합니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "게시글 수정 요청")
    public static class BoardUpdateRequest {

        @Schema(description = "수정할 게시글 제목", example = "우리 보리 산책 기록")
        private String boardTitle;

        @Schema(description = "수정할 게시글 내용", example = "오늘 산책 사진을 다시 정리했어요.")
        private String boardContent;

        @Schema(description = "수정할 게시글 이미지 URL 목록", example = "[\"https://example.com/images/bori_1.jpg\", \"https://example.com/images/bori_2.jpg\"]")
        private List<String> imageFileUrls;
    }

    /**
     * 게시글 임시 저장 요청 DTO입니다.
     * <p>
     * Redis에 저장할 제목, 본문, 단일 이미지 URL을 담습니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "게시글 임시 저장 요청")
    public static class BoardTempSaveRequest {

        @Schema(description = "임시 저장 게시글 제목", example = "임시 저장 제목")
        private String boardTitle;

        @Schema(description = "임시 저장 게시글 내용", example = "임시 저장 내용")
        private String boardContent;

        @Schema(description = "임시 저장 게시글 이미지 URL", example = "https://example.com/images/bori.jpg")
        private String imageFileUrl;
    }
}
