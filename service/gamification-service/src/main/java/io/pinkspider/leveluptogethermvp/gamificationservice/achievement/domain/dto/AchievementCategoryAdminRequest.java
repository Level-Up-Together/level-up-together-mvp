package io.pinkspider.leveluptogethermvp.gamificationservice.achievement.domain.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class AchievementCategoryAdminRequest {

    @NotBlank(message = "카테고리 코드는 필수입니다.")
    @Size(max = 30, message = "카테고리 코드는 30자 이하이어야 합니다.")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "카테고리 코드는 대문자로 시작하고 대문자, 숫자, 언더스코어만 허용됩니다.")
    private String code;

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    @Size(max = 50, message = "카테고리 이름은 50자 이하이어야 합니다.")
    private String name;

    @Size(max = 200, message = "카테고리 설명은 200자 이하이어야 합니다.")
    private String description;

    @NotNull(message = "정렬 순서는 필수입니다.")
    private Integer sortOrder;

    private Boolean isActive;
}
