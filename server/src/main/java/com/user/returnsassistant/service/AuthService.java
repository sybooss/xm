package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.LoginResponse;
import com.user.returnsassistant.pojo.RegisterRequest;
import com.user.returnsassistant.pojo.UserAccount;

public interface AuthService {
    LoginResponse login(String username, String password);

    LoginResponse me(String token);

    LoginResponse register(RegisterRequest request);

    UserAccount requireUser(String token);

    void logout(String token);
}
