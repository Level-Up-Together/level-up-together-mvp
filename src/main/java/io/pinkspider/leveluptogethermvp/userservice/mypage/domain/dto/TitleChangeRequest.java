package io.pinkspider.leveluptogethermvp.userservice.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 칭호 변경 요청 DTO
 * 좌측, 우측 각각 1개씩 필수 선택
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TitleChangeRequest {

    @NotNull(message = "좌측 칭호 ID는 필수입니다.")
    @JsonProperty("left_user_title_id")
    private Long leftUserTitleId;

    @NotNull(message = "우측 칭호 ID는 필수입니다.")
    @JsonProperty("right_user_title_id")
    private Long rightUserTitleId;
}
