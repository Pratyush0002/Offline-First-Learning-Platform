package com.studenthelper.server.repository;

import com.studenthelper.server.entity.DeviceSyncEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceSyncRepository extends JpaRepository<DeviceSyncEntity, String> {
}
