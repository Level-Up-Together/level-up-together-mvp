package io.pinkspider.leveluptogethermvp.metaservice.userlevelconfig.domain.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class UserLevelConfigRequest {

    @NotNull(message = "레벨은 필수입니다.")
    @Min(value = 1, message = "레벨은 1 이상이어야 합니다.")
    private Integer level;

    @NotNull(message = "필요 경험치는 필수입니다.")
    @Min(value = 0, message = "필요 경험치는 0 이상이어야 합니다.")
    private Integer requiredExp;

    @Min(value = 0, message = "누적 경험치는 0 이상이어야 합니다.")
    private Integer cumulativeExp;
}
