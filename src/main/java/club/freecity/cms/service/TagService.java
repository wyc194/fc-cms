package club.freecity.cms.service;

import club.freecity.cms.dto.TagDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface TagService {
    TagDto saveTag(@NotNull TagDto tagDto);
    void deleteTag(@NotNull Long id);
    TagDto getTagById(@NotNull Long id);
    List<TagDto> listAllTags();
    TagDto getTagByName(@NotBlank String name);
    long countTags();
    void increaseArticleCount(Long id);
    void decreaseArticleCount(Long id);
}
