package club.freecity.cms.service.impl;

import club.freecity.cms.dto.TagDto;
import club.freecity.cms.entity.Tag;
import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.repository.TagRepository;
import club.freecity.cms.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public TagDto saveTag(TagDto tagDto) {
        Tag tag;
        if (tagDto.getId() != null) {
            // 更新：获取现有实体以保留 tenant_id 等不可变字段
            tag = tagRepository.findById(tagDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("标签不存在"));
            BeanConverter.updateEntity(tag, tagDto);
        } else {
            // 新增
            tag = BeanConverter.toEntity(tagDto);
            if (tag.getArticleCount() == null) {
                tag.setArticleCount(0);
            }
        }
        return BeanConverter.toDto(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TagDto getTagById(Long id) {
        return tagRepository.findById(id)
                .map(BeanConverter::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDto> listAllTags() {
        return tagRepository.findAll().stream()
                .map(BeanConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TagDto getTagByName(String name) {
        return tagRepository.findByName(name)
                .map(BeanConverter::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTags() {
        return tagRepository.count();
    }

    @Override
    @Transactional
    public void increaseArticleCount(Long id) {
        tagRepository.findById(id).ifPresent(tag -> {
            Integer count = tag.getArticleCount();
            tag.setArticleCount(count == null ? 1 : count + 1);
            tagRepository.save(tag);
        });
    }

    @Override
    @Transactional
    public void decreaseArticleCount(Long id) {
        tagRepository.findById(id).ifPresent(tag -> {
            if (tag.getArticleCount() > 0) {
                tag.setArticleCount(tag.getArticleCount() - 1);
                tagRepository.save(tag);
            }
        });
    }
}
