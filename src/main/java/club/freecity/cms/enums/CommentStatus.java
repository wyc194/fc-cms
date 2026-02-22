package club.freecity.cms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentStatus {
    PENDING(0, "待审核"),
    PUBLISHED(1, "已发布"),
    REJECTED(2, "已拒绝");

    private final int value;
    private final String description;

    public static CommentStatus fromValue(Integer value) {
        if (value == null) return PENDING;
        for (CommentStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return PENDING;
    }
}
