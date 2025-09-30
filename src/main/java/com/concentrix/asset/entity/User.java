package com.concentrix.asset.entity;

import com.concentrix.asset.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity lưu thông tin người dùng/nhân viên sử dụng thiết bị.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    String eid;

    @Column(nullable = false)
    String fullName;

    @Column
    String jobTitle;

    @Column(unique = true)
    String email;

    @Column(unique = true)
    String sso;

    @Column
    String location;

    @Column
    String company;

    @Column
    String costCenter;

    @Column
    String msa;

    @Column
    String msaClient;

    @Column
    String msaProgram;

    @Column
    String managerEmail;

    @Column
    boolean isActive;

    @Column
    String password;

    @Column
    @Enumerated(EnumType.STRING)
    Role role;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;

    @ManyToOne
    @JoinColumn(name = "site_id")
    Site site;

    @ManyToOne
    @JoinColumn(name = "create_by_eid")
    User createdBy;

    @ManyToOne
    @JoinColumn(name = "update_by_eid")
    User updatedBy;

    @OneToMany(mappedBy = "user")
    List<DeviceUser> deviceUsers;

    @Column
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}