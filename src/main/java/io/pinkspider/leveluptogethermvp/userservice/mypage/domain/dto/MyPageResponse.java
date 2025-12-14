package io.pinkspider.leveluptogethermvp.userservice.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MyPage 화면 전체 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyPageResponse {

    // 1. 프로필 정보
    private ProfileInfo profile;

    // 2. 경험치 정보
    private ExperienceInfo experience;

    // 3. 유저 정보/통계
    @JsonProperty("user_info")
    private UserInfo userInfo;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfileInfo {
        @JsonProperty("user_id")
        private String userId;

        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;

        @JsonProperty("left_title")
        private EquippedTitleInfo leftTitle;

        @JsonProperty("right_title")
        private EquippedTitleInfo rightTitle;

        @JsonProperty("follower_count")
        private Integer followerCount;

        @JsonProperty("following_count")
        private Integer followingCount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquippedTitleInfo {
        @JsonProperty("user_title_id")
        private Long userTitleId;

        @JsonProperty("title_id")
        private Long titleId;

        private String name;

        @JsonProperty("display_name")
        private String displayName;

        private String rarity;

        @JsonProperty("color_code")
        private String colorCode;

        @JsonProperty("icon_url")
        private String iconUrl;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExperienceInfo {
        @JsonProperty("current_level")
        private Integer currentLevel;

        @JsonProperty("current_exp")
        private Integer currentExp;

        @JsonProperty("total_exp")
        private Integer totalExp;

        @JsonProperty("next_level_required_exp")
        private Integer nextLevelRequiredExp;

        @JsonProperty("exp_percentage")
        private Double expPercentage;

        @JsonProperty("exp_for_percentage")
        private Integer expForPercentage;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        @JsonProperty("start_date")
        private LocalDate startDate;

        @JsonProperty("days_since_joined")
        private Long daysSinceJoined;

        @JsonProperty("cleared_missions_count")
        private Integer clearedMissionsCount;

        @JsonProperty("cleared_mission_books_count")
        private Integer clearedMissionBooksCount;

        @JsonProperty("ranking_percentile")
        private Double rankingPercentile;

        @JsonProperty("acquired_titles_count")
        private Integer acquiredTitlesCount;

        @JsonProperty("ranking_points")
        private Long rankingPoints;
    }
}
