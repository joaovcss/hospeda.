package com.hotelaria.repositories;

import com.hotelaria.models.Permission;
import com.hotelaria.models.enums.PermissionName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByNameIn(Collection<PermissionName> names);
}
