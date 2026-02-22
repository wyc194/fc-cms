package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.TagDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import club.freecity.cms.service.TagService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@Validated
public class TagController {

    private final TagService tagService;

    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.TAG_CREATE, message = "'创建标签: ' + #tagDto.name", logArgs = true)
    public Result<TagDto> saveTag(@Validated(CreateGroup.class) @RequestBody @NotNull TagDto tagDto) {
        return Result.success(tagService.saveTag(tagDto));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.TAG_UPDATE, message = "'更新标签 ID: ' + #tagDto.id", logArgs = true)
    public Result<TagDto> updateTag(@Validated(UpdateGroup.class) @RequestBody @NotNull TagDto tagDto) {
        return Result.success(tagService.saveTag(tagDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.TAG_DELETE, message = "'删除标签 ID: ' + #id")
    public Result<Void> deleteTag(@PathVariable @NotNull Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<TagDto> getTagById(@PathVariable @NotNull Long id) {
        return Result.success(tagService.getTagById(id));
    }

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<List<TagDto>> listAllTags() {
        return Result.success(tagService.listAllTags());
    }

    @GetMapping("/name/{name}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<TagDto> getTagByName(@PathVariable @NotBlank String name) {
        return Result.success(tagService.getTagByName(name));
    }
}
