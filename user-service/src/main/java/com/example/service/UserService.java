package com.example.service;

import com.example.request.UserLoginRequest;
import com.example.request.UserRegisterRequest;
import com.example.util.JsonData;
import com.example.vo.UserVO;

public interface UserService {

    JsonData register(UserRegisterRequest registerRequest);

    JsonData login(UserLoginRequest loginRequest);

    UserVO findUserDetail();
}
