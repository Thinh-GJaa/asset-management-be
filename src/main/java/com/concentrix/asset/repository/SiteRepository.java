package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findBySiteName(String siteName);
}
