package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Model;
import com.concentrix.asset.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, Integer> {
    Optional<Model> findByModelName(String modelName);

    List<Model> findByType(DeviceType type);
}