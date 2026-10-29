package com.hotelaria.services;

import com.hotelaria.dtos.request.UserCreateRequest;
import com.hotelaria.dtos.request.UserUpdateRequest;
import com.hotelaria.dtos.response.PageResponse;
import com.hotelaria.dtos.response.PermissionResponse;
import com.hotelaria.dtos.response.UserResponse;
import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.UserType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    PageResponse<UserResponse> list(UserType userType, Boolean active, Pageable pageable);

    UserResponse findById(Long id);

    UserResponse update(Long id, UserUpdateRequest request);

    UserResponse updateStatus(Long id, boolean active);

    UserResponse updatePermissions(Long id, Set<PermissionName> permissions);

    List<PermissionResponse> listPermissions();
}
