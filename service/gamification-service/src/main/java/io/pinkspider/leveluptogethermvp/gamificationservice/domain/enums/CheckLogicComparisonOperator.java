package io.pinkspider.leveluptogethermvp.gamificationservice.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CheckLogicComparisonOperator {
    EQ("EQ", "같음 (==)", "=="),
    GTE("GTE", "이상 (>=)", ">="),
    GT("GT", "초과 (>)", ">"),
    LTE("LTE", "이하 (<=)", "<="),
    LT("LT", "미만 (<)", "<"),
    NE("NE", "다름 (!=)", "!=");

    private final String code;
    private final String displayName;
    private final String symbol;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static CheckLogicComparisonOperator fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CheckLogicComparisonOperator operator : values()) {
            if (operator.code.equalsIgnoreCase(code)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Unknown ComparisonOperator code: " + code);
    }
}
