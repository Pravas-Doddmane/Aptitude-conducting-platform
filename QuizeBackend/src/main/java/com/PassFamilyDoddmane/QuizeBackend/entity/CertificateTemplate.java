package com.PassFamilyDoddmane.QuizeBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "certificate_templates")
public class CertificateTemplate extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Lob
    @Column(name = "html_template", nullable = false)
    private String htmlTemplate;

    @Lob
    @Column(name = "css_template")
    private String cssTemplate;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;
}
