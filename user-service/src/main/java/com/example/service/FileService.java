package com.example.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    //返回url
    String uploadUserImg(MultipartFile file);

}
