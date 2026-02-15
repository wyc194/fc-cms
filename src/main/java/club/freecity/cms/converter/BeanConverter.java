package club.freecity.cms.converter;

import club.freecity.cms.dto.*;
import club.freecity.cms.entity.*;
import club.freecity.cms.entity.Package;
import club.freecity.cms.enums.UserRole;
import club.freecity.cms.util.MarkdownUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BeanConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static MediaAssetDto toDto(MediaAsset entity) {
        if (entity == null) return null;
        MediaAssetDto dto = new MediaAssetDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setUrl(entity.getUrl());
        dto.setType(entity.getType());
        dto.setSize(entity.getSize());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    public static CategoryDto toDto(Category entity) {
        if (entity == null) return null;
        CategoryDto dto = new CategoryDto();
        dto.setId(entity.getId());
        dto.setParentId(entity.getParentId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setArticleCount(entity.getArticleCount());
        dto.setWeight(entity.getWeight());
        dto.setLevel(entity.getLevel());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    public static TagDto toDto(Tag entity) {
        if (entity == null) return null;
        TagDto dto = new TagDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setArticleCount(entity.getArticleCount());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    public static ArticleDto toDto(Article entity) {
        if (entity == null) return null;
        ArticleDto dto = new ArticleDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setSummary(entity.getSummary());
        dto.setThumbnail(entity.getThumbnail());
        dto.setViewCount(entity.getViewCount());
        dto.setCommentCount(entity.getCommentCount());
        dto.setLikeCount(entity.getLikeCount());
        dto.setPublished(entity.getPublished());
        dto.setTop(entity.getTop());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setCategory(toDto(entity.getCategory()));
        if (entity.getTags() != null) {
            dto.setTags(entity.getTags().stream().map(BeanConverter::toDto).collect(Collectors.toSet()));
        }
        return dto;
    }

    public static CommentDto toDto(Comment entity) {
        if (entity == null) return null;
        CommentDto dto = new CommentDto();
        dto.setId(entity.getId());
        dto.setArticleId(entity.getArticleId());
        dto.setNickname(entity.getNickname());
        dto.setEmail(entity.getEmail());
        dto.setContent(entity.getContent());
        dto.setIp(entity.getIp());
        dto.setUserAgent(entity.getUserAgent());
        dto.setAdminReply(entity.getAdminReply());
        dto.setStatus(entity.getStatus());
        dto.setCreateTime(entity.getCreateTime());
        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
            dto.setParentNickname(entity.getParent().getNickname());
        }
        if (entity.getReplies() != null) {
            dto.setReplies(entity.getReplies().stream().map(BeanConverter::toDto).collect(Collectors.toSet()));
        }
        return dto;
    }

    public static UserDto toDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole().getValue())
                .status(user.getStatus())
                .tenantId(user.getTenantId())
                .tenantCode(user.getTenant() != null ? user.getTenant().getCode() : null)
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }

    public static TenantDto toDto(Tenant tenant) {
        if (tenant == null) return null;
        TenantDto.TenantDtoBuilder builder = TenantDto.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .status(tenant.getStatus())
                .packageId(tenant.getPackageInfo() != null ? tenant.getPackageInfo().getId() : null)
                .packageName(tenant.getPackageInfo() != null ? tenant.getPackageInfo().getName() : null)
                .expireTime(tenant.getExpireTime())
                .createTime(tenant.getCreateTime())
                .updateTime(tenant.getUpdateTime());

        try {
            if (StringUtils.hasText(tenant.getWebInfo())) {
                builder.webInfo(objectMapper.readValue(tenant.getWebInfo(), WebInfo.class));
            }
            if (StringUtils.hasText(tenant.getSocialInfo())) {
                builder.socialInfo(objectMapper.readValue(tenant.getSocialInfo(), SocialInfo.class));
            }
            if (StringUtils.hasText(tenant.getCustomCode())) {
                builder.customCode(objectMapper.readValue(tenant.getCustomCode(), CustomCode.class));
            }
            if (StringUtils.hasText(tenant.getLinks())) {
                builder.links(objectMapper.readValue(tenant.getLinks(), new TypeReference<Map<String, List<LinkItem>>>() {}));
            }
        } catch (Exception e) {
            log.error("Failed to deserialize tenant JSON fields", e);
        }

        return builder.build();
    }

    public static PackageDto toDto(Package pkg) {
        if (pkg == null) return null;
        return PackageDto.builder()
                .id(pkg.getId())
                .code(pkg.getCode())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .maxArticles(pkg.getMaxArticles())
                .maxStorage(pkg.getMaxStorage())
                .customDomainEnabled(pkg.getCustomDomainEnabled())
                .advancedStatsEnabled(pkg.getAdvancedStatsEnabled())
                .createTime(pkg.getCreateTime())
                .updateTime(pkg.getUpdateTime())
                .build();
    }

    public static void updateEntity(Package pkg, PackageDto dto) {
        if (pkg == null || dto == null) return;
        pkg.setCode(dto.getCode());
        pkg.setName(dto.getName());
        pkg.setDescription(dto.getDescription());
        pkg.setPrice(dto.getPrice());
        pkg.setMaxArticles(dto.getMaxArticles());
        pkg.setMaxStorage(dto.getMaxStorage());
        pkg.setCustomDomainEnabled(dto.getCustomDomainEnabled());
        pkg.setAdvancedStatsEnabled(dto.getAdvancedStatsEnabled());
    }

    public static void updateEntity(User user, UserDto dto) {
        if (user == null || dto == null) return;
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        if (dto.getRole() != null) {
            user.setRole(UserRole.fromValue(dto.getRole()));
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
    }

    public static void updateEntity(Tenant tenant, TenantDto dto) {
        if (tenant == null || dto == null) return;
        if (StringUtils.hasText(dto.getName())) {
            tenant.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            tenant.setStatus(dto.getStatus());
        }
        if (dto.getExpireTime() != null) {
            tenant.setExpireTime(dto.getExpireTime());
        }
        
        // 安全清洗 WebInfo 中的 HTML 字段
        if (dto.getWebInfo() != null) {
            WebInfo webInfo = dto.getWebInfo();
            webInfo.setAnnouncement(MarkdownUtils.sanitize(webInfo.getAnnouncement()));
            webInfo.setAboutMe(MarkdownUtils.sanitize(webInfo.getAboutMe()));
            webInfo.setCopyright(MarkdownUtils.sanitize(webInfo.getCopyright()));
        }
        
        try {
            if (dto.getWebInfo() != null) {
                tenant.setWebInfo(objectMapper.writeValueAsString(dto.getWebInfo()));
            }
            if (dto.getSocialInfo() != null) {
                tenant.setSocialInfo(objectMapper.writeValueAsString(dto.getSocialInfo()));
            }
            if (dto.getCustomCode() != null) {
                tenant.setCustomCode(objectMapper.writeValueAsString(dto.getCustomCode()));
            }
            if (dto.getLinks() != null) {
                tenant.setLinks(objectMapper.writeValueAsString(dto.getLinks()));
            }
        } catch (Exception e) {
            log.error("Failed to serialize tenant JSON fields", e);
        }
    }

    public static void updateEntity(Article article, ArticleDto dto) {
        if (article == null || dto == null) return;
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setThumbnail(dto.getThumbnail());
        article.setPublished(dto.getPublished());
        article.setTop(dto.getTop());
    }

    public static void updateEntity(Category category, CategoryDto dto) {
        if (category == null || dto == null) return;
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setWeight(dto.getWeight() != null ? dto.getWeight() : 0);
    }

    public static void updateEntity(Tag tag, TagDto dto) {
        if (tag == null || dto == null) return;
        tag.setName(dto.getName());
        if (dto.getArticleCount() != null) {
            tag.setArticleCount(dto.getArticleCount());
        }
    }

    public static Category toEntity(CategoryDto dto) {
        if (dto == null) return null;
        Category entity = new Category();
        entity.setId(dto.getId());
        entity.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setArticleCount(dto.getArticleCount() != null ? dto.getArticleCount() : 0);
        entity.setWeight(dto.getWeight() != null ? dto.getWeight() : 0);
        entity.setLevel(dto.getLevel() != null ? dto.getLevel() : 1);
        return entity;
    }

    public static Tag toEntity(TagDto dto) {
        if (dto == null) return null;
        Tag entity = new Tag();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setArticleCount(dto.getArticleCount());
        return entity;
    }

    public static Article toEntity(ArticleDto dto) {
        if (dto == null) return null;
        Article entity = new Article();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setSummary(dto.getSummary());
        entity.setThumbnail(dto.getThumbnail());
        entity.setViewCount(dto.getViewCount());
        entity.setCommentCount(dto.getCommentCount());
        entity.setLikeCount(dto.getLikeCount());
        entity.setPublished(dto.getPublished());
        entity.setTop(dto.getTop());
        entity.setCategory(toEntity(dto.getCategory()));
        if (dto.getTags() != null) {
            entity.setTags(dto.getTags().stream().map(BeanConverter::toEntity).collect(Collectors.toSet()));
        }
        return entity;
    }

    public static Comment toEntity(CommentDto dto) {
        if (dto == null) return null;
        Comment entity = new Comment();
        entity.setId(dto.getId());
        entity.setArticleId(dto.getArticleId());
        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setContent(dto.getContent());
        entity.setIp(dto.getIp());
        entity.setUserAgent(dto.getUserAgent());
        entity.setAdminReply(dto.getAdminReply());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        // Parent and replies are usually handled in service layer for complex relationships
        return entity;
    }
}
