package io.pinkspider.leveluptogethermvp.userservice.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 칭호 변경 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TitleChangeResponse {

    private String message;

    @JsonProperty("left_title")
    private MyPageResponse.EquippedTitleInfo leftTitle;

    @JsonProperty("right_title")
    private MyPageResponse.EquippedTitleInfo rightTitle;
}
