package club.freecity.cms.service;

import club.freecity.cms.config.CacheConfig;
import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.enums.TenantStatus;
import club.freecity.cms.dto.*;
import club.freecity.cms.entity.Tenant;
import club.freecity.cms.entity.Package;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.PackageRepository;
import club.freecity.cms.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import club.freecity.cms.entity.User;
import club.freecity.cms.enums.UserRole;
import club.freecity.cms.repository.UserRepository;
import club.freecity.cms.util.PasswordUtils;

import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.tenant.default-code:admin}")
    private String defaultTenantCode;

    @Transactional(readOnly = true)
    public List<TenantDto> listAllTenants() {
        return tenantRepository.findAll().stream()
                .map(BeanConverter::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("租户不存在"));
        return BeanConverter.toDto(tenant);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TENANTS, key = "#tenantDto.code")
    public TenantCreateResultDto saveTenant(TenantDto tenantDto) {
        if (tenantRepository.findByCode(tenantDto.getCode()).isPresent()) {
            throw new BusinessException("租户编码已存在");
        }
        Tenant tenant = new Tenant();
        tenant.setCode(tenantDto.getCode());
        if (!StringUtils.hasText(tenantDto.getStatus())) {
            tenantDto.setStatus(TenantStatus.ACTIVE.getValue());
        }
        
        BeanConverter.updateEntity(tenant, tenantDto);
        
        if (tenantDto.getPackageId() != null) {
            Package pkg = packageRepository.findById(tenantDto.getPackageId())
                    .orElseThrow(() -> new BusinessException("套餐不存在"));
            tenant.setPackageInfo(pkg);
        }
        
        tenant = tenantRepository.save(tenant);
        
        // 自动创建租户管理员
        String adminUsername = tenant.getCode();
        String plainPassword = PasswordUtils.generateRandomPassword();
        
        User admin = new User();
        admin.setTenantId(tenant.getId());
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(plainPassword));
        admin.setNickname(tenant.getName() + "管理员");
        admin.setRole(UserRole.TENANT_ADMIN);
        admin.setStatus("ENABLED");
        userRepository.save(admin);
        
        return TenantCreateResultDto.builder()
                .tenant(BeanConverter.toDto(tenant))
                .adminUsername(adminUsername)
                .adminPassword(plainPassword)
                .build();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TENANTS, key = "#result.code")
    public TenantDto updateTenant(TenantDto tenantDto) {
        Tenant tenant = tenantRepository.findById(tenantDto.getId())
                .orElseThrow(() -> new BusinessException("租户不存在"));
        
        BeanConverter.updateEntity(tenant, tenantDto);
        
        if (tenantDto.getPackageId() != null) {
            Package pkg = packageRepository.findById(tenantDto.getPackageId())
                    .orElseThrow(() -> new BusinessException("套餐不存在"));
            tenant.setPackageInfo(pkg);
        }
        
        return BeanConverter.toDto(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public TenantDto getCurrentTenantConfig() {
        String code = club.freecity.cms.common.TenantContext.getCurrentTenantCode();
        if (code == null) {
            code = "admin"; // 默认租户
        }
        Tenant tenant = tenantRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("当前租户配置不存在"));
        return BeanConverter.toDto(tenant);
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantByCode(String code) {
        Tenant tenant = tenantRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("当前租户配置不存在"));
        return BeanConverter.toDto(tenant);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TENANTS, allEntries = true)
    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("租户不存在"));
        
        if (defaultTenantCode.equals(tenant.getCode())) {
            throw new BusinessException("默认租户不允许删除");
        }
        
        tenantRepository.deleteById(id);
    }
}
