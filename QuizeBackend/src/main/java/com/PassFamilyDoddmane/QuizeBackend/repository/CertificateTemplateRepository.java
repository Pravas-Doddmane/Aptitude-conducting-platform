package com.PassFamilyDoddmane.QuizeBackend.repository;

import com.PassFamilyDoddmane.QuizeBackend.entity.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {
    List<CertificateTemplate> findByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
