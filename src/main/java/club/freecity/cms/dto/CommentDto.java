package club.freecity.cms.dto;

import club.freecity.cms.validator.group.CreateGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CommentDto {
    @Null(groups = CreateGroup.class, message = "新增时评论ID必须为空")
    private Long id;

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId;
    private String parentNickname;
    private String ip;
    private String userAgent;
    private Boolean adminReply;
    private Integer status;
    private String verificationCode; // 仅用于提交时的验证码传递
    private LocalDateTime createTime;
    private Set<CommentDto> replies;
}
