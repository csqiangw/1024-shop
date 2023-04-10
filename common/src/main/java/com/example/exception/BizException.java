package com.example.exception;

import com.example.enums.BizCodeEnum;
import lombok.Data;

/**
 * 全局异常处理
 */
@Data
public class BizException extends RuntimeException {

    private Integer code;
    private String msg;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.msg = message;
    }

    //有可能给一个bizCideEnum，而不是只记得状态码 + 异常信息
    public BizException(BizCodeEnum bizCodeEnum) {
        super(bizCodeEnum.getMessage());
        this.code = bizCodeEnum.getCode();
        this.msg = bizCodeEnum.getMessage();
    }

}
