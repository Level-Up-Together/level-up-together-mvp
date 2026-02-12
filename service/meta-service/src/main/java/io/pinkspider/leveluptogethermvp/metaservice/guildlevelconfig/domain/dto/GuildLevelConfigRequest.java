package io.pinkspider.leveluptogethermvp.metaservice.guildlevelconfig.domain.dto;

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
public class GuildLevelConfigRequest {

    @NotNull(message = "레벨은 필수입니다.")
    @Min(value = 1, message = "레벨은 1 이상이어야 합니다.")
    private Integer level;

    @NotNull(message = "최대 멤버 수는 필수입니다.")
    @Min(value = 1, message = "최대 멤버 수는 1 이상이어야 합니다.")
    private Integer maxMembers;

    private String title;

    private String description;
}
