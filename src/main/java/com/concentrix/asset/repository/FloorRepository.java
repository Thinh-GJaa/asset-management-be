package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Floor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Integer>, JpaSpecificationExecutor<Floor> {
    Page<Floor> findAllBySite_SiteId(Integer siteId, Pageable pageable);

    List<Floor> findAllBySite_SiteId(Integer siteId);
}