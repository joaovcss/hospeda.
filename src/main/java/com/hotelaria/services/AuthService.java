package com.hotelaria.services;

import com.hotelaria.dtos.request.ChangePasswordRequest;
import com.hotelaria.dtos.request.LoginRequest;
import com.hotelaria.dtos.response.AuthenticatedUserResponse;
import com.hotelaria.dtos.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    AuthenticatedUserResponse getAuthenticatedUser();

    void changePassword(ChangePasswordRequest request);
}
