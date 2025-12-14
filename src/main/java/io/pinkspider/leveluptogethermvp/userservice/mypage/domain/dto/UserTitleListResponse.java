package io.pinkspider.leveluptogethermvp.userservice.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 보유 칭호 목록 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTitleListResponse {

    @JsonProperty("total_count")
    private Integer totalCount;

    private List<UserTitleItem> titles;

    @JsonProperty("equipped_left_id")
    private Long equippedLeftId;

    @JsonProperty("equipped_right_id")
    private Long equippedRightId;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserTitleItem {
        @JsonProperty("user_title_id")
        private Long userTitleId;

        @JsonProperty("title_id")
        private Long titleId;

        private String name;

        @JsonProperty("display_name")
        private String displayName;

        private String description;

        private String rarity;

        @JsonProperty("color_code")
        private String colorCode;

        @JsonProperty("icon_url")
        private String iconUrl;

        @JsonProperty("is_equipped")
        private Boolean isEquipped;

        @JsonProperty("equipped_position")
        private String equippedPosition;

        @JsonProperty("acquired_at")
        private LocalDateTime acquiredAt;
    }
}
