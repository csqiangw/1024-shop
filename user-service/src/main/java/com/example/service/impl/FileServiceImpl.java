package com.example.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import com.example.config.OSSConfig;
import com.example.service.FileService;
import com.example.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private OSSConfig ossConfig;

    @Override
    public String uploadUserImg(MultipartFile file) {
        //获取相关配置
        String bucketname = ossConfig.getBucketname();
        String endpoint = ossConfig.getEndpoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();
        //创建OSS对象
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        //获取原始文件名
        String originalFilename = file.getOriginalFilename();

        //JDK8的日期格式化
        LocalDateTime ldt = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        //拼装路径，oss上存储的路径 2022/12/1/sadada.jpg 文件名用uuid随机生成
        String floder = dtf.format(ldt);
        String fileName = CommonUtil.generateUUID();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        //拼装 文件夹不存在，oss会帮我们自动创建
        String newFileName = "user/" + floder + "/" + fileName + extension;
        try {
            PutObjectResult result = ossClient.putObject(bucketname, newFileName, file.getInputStream());
            //拼装返回路径
            if(result != null){
                return "https://" + bucketname + "." + endpoint + "/" + newFileName;
            }
        } catch (IOException e) {
            log.error("文件上传失败：{}",e);
        }finally {
            //必须关闭，不然会造成内存泄露
            ossClient.shutdown();
        }
        return null;
    }

}
