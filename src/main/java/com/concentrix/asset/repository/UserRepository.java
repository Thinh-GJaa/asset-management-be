package com.concentrix.asset.repository;

import com.concentrix.asset.entity.User;
import com.concentrix.asset.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
    Optional<User> findBySso(String sso);

    Optional<User> findByEmail(String email);

    Optional<User> findByMsa(String msa);

    List<User> findBySite_SiteId(Integer siteId);

    List<User> findByRoleAndSite_SiteId(Role role, Integer siteId);


    @Query("SELECT u.email FROM User u WHERE u.role = :role AND u.site.siteId = :siteId")
    List<String> findEmailByRoleAndSite_SiteId(Role role, Integer siteId);

}