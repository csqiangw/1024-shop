package com.example.service;

import com.example.request.AddressAddRequest;
import com.example.vo.AddressVO;

import java.util.List;

public interface AddressService {

    AddressVO detail(Long id);

    void add(AddressAddRequest addressAddRequest);

    int del(long addressId);

    List<AddressVO> listUserAllAddress();
}
