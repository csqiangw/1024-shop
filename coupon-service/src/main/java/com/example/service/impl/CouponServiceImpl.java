package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.enums.BizCodeEnum;
import com.example.enums.CouponCategoryEnum;
import com.example.enums.CouponPublishEnum;
import com.example.enums.CouponStateEnum;
import com.example.exception.BizException;
import com.example.interceptor.LoginInterceptor;
import com.example.mapper.CouponMapper;
import com.example.mapper.CouponRecordMapper;
import com.example.model.CouponDO;
import com.example.model.CouponRecordDO;
import com.example.model.LoginUser;
import com.example.request.NewUserCouponRequest;
import com.example.service.CouponService;
import com.example.util.CommonUtil;
import com.example.util.JsonData;
import com.example.vo.CouponVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wq
 * @since 2023-03-30
 */
@Service
@Slf4j
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private CouponRecordMapper couponRecordMapper;


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Map<String, Object> pageCouponActivity(int page,int size) {
        Page<CouponDO> pageInfo = new Page<>(page,size);
        IPage<CouponDO> couponDOPage = couponMapper.selectPage(pageInfo, new QueryWrapper<CouponDO>()
                .eq("publish", CouponPublishEnum.PUBLISH.name())
                .eq("category", CouponCategoryEnum.PROMOTION.name())
                .orderByDesc("create_time"));
        Map<String,Object> pageMap = new HashMap<>(3);
        pageMap.put("total_record",couponDOPage.getTotal());//获取总记录数
        pageMap.put("total_page",couponDOPage.getPages());//获取总页数
        pageMap.put("current_data",couponDOPage.getRecords().stream().map(obj->beanProcess(obj)).collect(Collectors.toList()));//获取总内容
        return pageMap;
    }

//    /**
//     *      * 1.获取优惠券是否存在
//     *      * 2.校验优惠券是否可以领取：时间、库存、超过限制
//     *      * 3.扣减库存
//     *      * 4.保存领券记录
//     * @param couponId
//     * @param category
//     * @return
//     */
//    //TODO 存在回滚问题
//    @Override
//    public JsonData addCoupon(long couponId, CouponCategoryEnum category) {
//        String uuid = CommonUtil.generateUUID();//标记线程
//        String lockKey = "lock:coupon:" + couponId;
//        //避免锁过期，防止出现线程不安全，反正在线程结束后会自动释放锁
//        Boolean lockFlag = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, Duration.ofMinutes(10));
//        LoginUser loginUser = LoginInterceptor.threadLocal.get();
//        if(lockFlag){
//            //加锁成功
//            log.info("加锁成功",couponId);
//            try {
//                //执行业务逻辑 TODO
//                CouponDO couponDO = couponMapper.selectOne(new QueryWrapper<CouponDO>()
//                        .eq("id", couponId)
//                        .eq("category", category.name())//传过来的类目是不是发布的
//                        .eq("publish", CouponPublishEnum.PUBLISH.name()));
//                //校验优惠券是否可以领取
//                checkCoupon(couponDO,loginUser.getId());
//
//                //构建领券记录
//                CouponRecordDO couponRecordDO = new CouponRecordDO();
//                BeanUtils.copyProperties(couponDO,couponRecordDO);
//                couponRecordDO.setCreateTime(new Date());
//                couponRecordDO.setUseState(CouponStateEnum.NEW.name());
//                couponRecordDO.setUserId(loginUser.getId());
//                couponRecordDO.setUserName(loginUser.getName());
//                couponRecordDO.setCouponId(couponId);
//                couponRecordDO.setId(null);//用数据库自增的
//
//                //扣减库存 TODO
//                int rows = couponMapper.reduceStock(couponId);
//                if(rows == 1){
//                    //库存扣减成功才保存记录
//                    couponRecordMapper.insert(couponRecordDO);
//                }else{
//                    log.warn("发放优惠券失败：{}，用户：{}",couponDO,loginUser);
//                    throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
//                }
//            }finally {
//                //使用lua脚本进行操作 保证原子性
//                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
////                Integer result = redisTemplate.execute(new DefaultRedisScript<>(script, Integer.class), Arrays.asList(lockKey), uuid);
//                log.info("解锁：{}",result);
//            }
//        }else{
//            //加锁失败 进入自旋，自己调用自己，防止无限自旋，可以睡眠一次，也可以设置自旋次数限制
//            try {
//                TimeUnit.SECONDS.sleep(1);//睡眠1s
//            } catch (InterruptedException e) {
//                log.error("自旋失败{}",e);
//            }
//            addCoupon(couponId,category);
//        }
//        return JsonData.buildSuccess();
//    }

    /**
     *      * 1.获取优惠券是否存在
     *      * 2.校验优惠券是否可以领取：时间、库存、超过限制
     *      * 3.扣减库存
     *      * 4.保存领券记录
     * @param couponId
     * @param category
     * @return
     */
    //在需要的地方加就可以了
    @Transactional(rollbackFor=Exception.class,propagation= Propagation.REQUIRED)
    @Override
    public JsonData addCoupon(long couponId, CouponCategoryEnum category) {
//        String lockKey = "lock:coupon:" + couponId;
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
//        RLock rLock = redissonClient.getLock(lockKey);
        //默认30s，有watchdog功能，自动续期
//        rLock.lock();
        //加锁10秒钟过期，没有watch dog功能，无法自动续期，传过期时间，也无需解锁
        //rLock.lock(10,Time);
        //rlock.tryLock(100,10,TimeUnit.SECONDS) 尝试加锁，最多等待100s，上锁10s后自动解锁
        //多个线程进入会阻塞等待释放锁
        //这时就不用去让其自旋了，有锁之后会被唤醒的
//        log.info("领券接口加锁成功：{}",Thread.currentThread().getId());
        try {
            //执行业务逻辑 TODO
            CouponDO couponDO = couponMapper.selectOne(new QueryWrapper<CouponDO>()
                    .eq("id", couponId)
                    .eq("category", category.name())//传过来的类目是不是发布的
                    .eq("publish", CouponPublishEnum.PUBLISH.name()));
            //校验优惠券是否可以领取
            checkCoupon(couponDO,loginUser.getId());
            //构建领券记录
            CouponRecordDO couponRecordDO = new CouponRecordDO();
            BeanUtils.copyProperties(couponDO,couponRecordDO);
            couponRecordDO.setCreateTime(new Date());
            couponRecordDO.setUseState(CouponStateEnum.NEW.name());
            couponRecordDO.setUserId(loginUser.getId());
            couponRecordDO.setUserName(loginUser.getName());
            couponRecordDO.setCouponId(couponId);
            couponRecordDO.setId(null);//用数据库自增的

            //扣减库存 TODO
            int rows = couponMapper.reduceStock(couponId);
            if(rows == 1){
                    //库存扣减成功才保存记录
                couponRecordMapper.insert(couponRecordDO);
            }else{
                log.warn("发放优惠券失败：{}，用户：{}",couponDO,loginUser);
                throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
            }
        }finally {
//            rLock.unlock();
//            log.info("解锁成功");
        }
        return JsonData.buildSuccess();
    }

    /**
     * 用户微服务调用的时候，没传递token
     *
     * 本地直接调用发放优惠券的方法，需要构造一个登录用户存储在threadLocal
     *
     * 注册的时候没有token的，注册的时候调用这个方法，在这里controller这个方法就不该被拦截
     * 发放优惠券时需要调用上面的addCoupon方法，但它需要一个threadLocal这个用户，需要构造出一个
     *
     * 接口对外暴露，没有做权限校验，是不是会不安全，影响不了，因为新用户都能领取，但领取到一定次数后，就不可领取了，所以无所谓
     * @param newUserCouponRequest
     * @return
     */
    //如果出现异常，需要回滚
    //这面不采用分布式事物，错误就回滚，但不影响注册，后期发放由查看日志等方式重新发放
    @Transactional(rollbackFor=Exception.class,propagation= Propagation.REQUIRED)
    @Override
    public JsonData initNewUserCoupon(NewUserCouponRequest newUserCouponRequest) {
        LoginUser loginUser = LoginUser.builder().id(newUserCouponRequest.getUserId())
                .name(newUserCouponRequest.getName()).build();
        LoginInterceptor.threadLocal.set(loginUser);
        //查询新用户有哪些优惠券
        List<CouponDO> couponDOList = couponMapper.selectList(new QueryWrapper<CouponDO>()
                .eq("category", CouponCategoryEnum.NEW_USER.name()));
        for (CouponDO couponDO : couponDOList){
            //幂等操作，调用需要加锁，多次注册进入，此时通过分布式锁，防止多发放
            addCoupon(couponDO.getId(),CouponCategoryEnum.NEW_USER);
        }
        return JsonData.buildSuccess();
    }

    /**
     * 校验是否可以领取
     * @param couponDO
     * @param user_id
     */
    private void checkCoupon(CouponDO couponDO, Long user_id) {
        if(couponDO == null){
            throw new BizException(BizCodeEnum.COUPON_NO_EXITS);
        }
        //库存是否足够
        if(couponDO.getStock() <= 0){
            throw new BizException(BizCodeEnum.COUPON_NO_STOCK);
        }
        //库存足，是否在领取时间范围
        long time = CommonUtil.getCurrentTimestamp();
        long start = couponDO.getStartTime().getTime();
        long end = couponDO.getEndTime().getTime();
        if(time < start || time > end){
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_TIME);
        }
        //用户是否超过单条限制
        Integer recordNum = couponRecordMapper.selectCount(new QueryWrapper<CouponRecordDO>()
                .eq("coupon_id", couponDO.getId())
                .eq("user_id", user_id));
        if(recordNum >= couponDO.getUserLimit()){
            throw new BizException(BizCodeEnum.COUPON_OUT_OF_LIMIT);//超过限制次数
        }
    }

    private CouponVO beanProcess(CouponDO couponDO) {
        CouponVO couponVO = new CouponVO();
        BeanUtils.copyProperties(couponDO,couponVO);
        return couponVO;
    }

}
