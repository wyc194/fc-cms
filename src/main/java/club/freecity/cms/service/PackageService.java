package club.freecity.cms.service;

import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.dto.PackageDto;
import club.freecity.cms.entity.Package;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;

    @Transactional(readOnly = true)
    public List<PackageDto> listAllPackages() {
        return packageRepository.findAll().stream()
                .map(BeanConverter::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PackageDto getPackageById(Long id) {
        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BusinessException("套餐不存在"));
        return BeanConverter.toDto(pkg);
    }

    @Transactional
    public PackageDto savePackage(PackageDto packageDto) {
        if (packageRepository.findByCode(packageDto.getCode()).isPresent()) {
            throw new BusinessException("套餐编码已存在");
        }
        Package pkg = new Package();
        BeanConverter.updateEntity(pkg, packageDto);
        return BeanConverter.toDto(packageRepository.save(pkg));
    }

    @Transactional
    public PackageDto updatePackage(PackageDto packageDto) {
        Package pkg = packageRepository.findById(packageDto.getId())
                .orElseThrow(() -> new BusinessException("套餐不存在"));
        BeanConverter.updateEntity(pkg, packageDto);
        return BeanConverter.toDto(packageRepository.save(pkg));
    }

    @Transactional
    public void deletePackage(Long id) {
        packageRepository.deleteById(id);
    }
}
