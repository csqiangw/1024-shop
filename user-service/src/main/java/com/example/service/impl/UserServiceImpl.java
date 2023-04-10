package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.enums.BizCodeEnum;
import com.example.enums.SendCodeEnum;
import com.example.feign.CouponFeignService;
import com.example.interceptor.LoginInterceptor;
import com.example.mapper.UserMapper;
import com.example.model.LoginUser;
import com.example.model.UserDO;
import com.example.request.NewUserCouponRequest;
import com.example.request.UserLoginRequest;
import com.example.request.UserRegisterRequest;
import com.example.service.NotifyService;
import com.example.service.UserService;
import com.example.util.CommonUtil;
import com.example.util.JWTUtil;
import com.example.util.JsonData;
import com.example.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

//用户注册
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CouponFeignService couponFeignService;

    /**
     * 用户注册
     * 1.邮箱验证码验证
     * 2.密码加密
     * 3.账号唯一性检查
     * 4.插入数据库
     * 5.新注册用户福利发放
     * @param registerRequest
     * @return
     */
    @Override
    public JsonData register(UserRegisterRequest registerRequest) {//这些内容都是前端传过来
        boolean checkCode = false;
        //校验验证码
        if(StringUtils.isNotBlank(registerRequest.getMail())){
            checkCode = notifyService.checkCode(SendCodeEnum.USER_REGISTER,registerRequest.getMail(), registerRequest.getCode());
        }
        if(!checkCode){
            return JsonData.buildResult(BizCodeEnum.CODE_ERROR);
        }
        UserDO userDO = new UserDO();
        //将对象属性进行拷贝，因为两部取得是一样的
        BeanUtils.copyProperties(registerRequest,userDO);
        userDO.setCreateTime(new Date());
        userDO.setSlogan("人生需要动态规划，学习需要贪心算法");
        //TODO 密码
//        userDO.setPwd(registerRequest.getPwd());
        //生成密钥 盐
        //几乎每个人的盐都不一样
        userDO.setSecret("$1$" + CommonUtil.getStringNumRandom(8));
        String cryptPwd = Md5Crypt.md5Crypt(registerRequest.getPwd().getBytes(),userDO.getSecret());
        userDO.setPwd(cryptPwd);

        //账号唯一性检查 TODO
        //要确保在高并发的环境下，账号的唯一性
        if(checkUnique(userDO.getMail())){
            int rows = userMapper.insert(userDO);//位置错了
            log.info("rows：{}",rows,userDO.toString());
            //新用户福利发放 初始化信息 发放福利等 TODO
            userRegisterInitTask(userDO);
            return JsonData.buildSuccess();
        }
        return JsonData.buildResult(BizCodeEnum.ACCOUNT_REPEAT);
    }

    /**
     * 1.根据mail去数据库中找有没有这条记录
     * 2.有的话，则用密钥+用户传递的铭文密码，进行加密，再和数据库的密文进行匹配
     * @param loginRequest
     * @return
     */
    @Override
    public JsonData login(UserLoginRequest loginRequest) {
        List<UserDO> userDOList = userMapper.selectList(new QueryWrapper<UserDO>().eq("mail", loginRequest.getMail()));
        if(userDOList != null && userDOList.size() == 1){
            //已经注册
            UserDO userDO = userDOList.get(0);
            String cryptPwd = Md5Crypt.md5Crypt(loginRequest.getPwd().getBytes(), userDO.getSecret());
            if(cryptPwd.equals(userDO.getPwd())){//登录成功
                //登录成功，生成token TODO
                //UUID -> token,存储redis并设置过期时间
                //token存在就说明没有过期，仍在登录状态，就可以获取数据，若不存在就说明过期了，没有登录
                LoginUser loginUser = LoginUser.builder().build();
                BeanUtils.copyProperties(userDO,loginUser);
                // accessToken
                // accessToken的过期时间
                //UUID 生成一个token
                String token = JWTUtil.geneJsonWebToken(loginUser);
                return JsonData.buildSuccess(token);
            }
            return JsonData.buildResult(BizCodeEnum.ACCOUNT_PWD_ERROR);
        }
        return JsonData.buildResult(BizCodeEnum.ACCOUNT_PWD_ERROR);//统一提示错误，迷惑别人
    }

    /**
     * 查看用户详情
     * @return
     */
    @Override
    public UserVO findUserDetail() {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();//通过被注入的拦截器获取当前登录用户的信息
        UserDO userDO = userMapper.selectOne(new QueryWrapper<UserDO>().eq("id", loginUser.getId()));
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userDO,userVO);
        return userVO;
    }

    /**
     * 检验用户账号是否唯一
     * 虽然可能多个进去，但是先插入的会被保存，因为数据库那面设置了唯一索引
     * @param mail
     * @return
     */
    private boolean checkUnique(String mail) {
        //操作不具有原子性，此时，可能会产生，都查询到没有，都进去了
        QueryWrapper queryWrapper = new QueryWrapper<UserDO>().eq("mail",mail);
        List<UserDO> list = userMapper.selectList(queryWrapper);
        return list.size() > 0 ? false : true;
    }

    /**
     * 用户注册，初始化福利信息
     * 用户注册不能回滚
     * @param userDO
     */
    private void userRegisterInitTask(UserDO userDO){
        NewUserCouponRequest request = new NewUserCouponRequest();
        request.setName(userDO.getName());
        request.setUserId(userDO.getId());
        JsonData jsonData = couponFeignService.addNewUserCoupon(request);
        log.info("发放新用户注册优惠券:{},结果:{}",request.toString(),jsonData.toString());
    }

}
