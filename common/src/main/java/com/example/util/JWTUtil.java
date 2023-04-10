package com.example.util;

import com.example.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

//每个微服务都可能要用
@Slf4j
public class JWTUtil {

    /**
     * token过期时间一般是7天，方便测试改为70天
     */
    private static final long EXPIRE = 60 * 1000 * 60 * 24 * 7 * 10;

    /**
     * 加密密钥
     */
    private static final String SECRET = "cswq666666";

    /**
     * 令牌前缀
     */
    private static final String TOKEN_PREFIX = "1024shop";

    /**
     * subject
     */
    private static final String SUBJECT = "jwt1024shop";

    /**
     * 根据用户信息生成令牌
     * @param loginUser
     * @return
     */
    public static String geneJsonWebToken(LoginUser loginUser){
        if(loginUser == null){
            throw new NullPointerException("对象未空");
        }
        String token = Jwts.builder().setSubject(SUBJECT).claim("head_img", loginUser.getHeadImg())//payload
                .claim("id", loginUser.getId())
                .claim("name", loginUser.getName())
                .claim("mail", loginUser.getMail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SignatureAlgorithm.HS256, SECRET).compact();//当前时间 + 期限时间
        token = TOKEN_PREFIX + token;
        return token;
    }

    /**
     * 校验token的方法
     * @param token
     * @return
     */
    public static Claims checkJWT(String token){
        try {
            //拿到负载
            //设置了过期时间，若过期了就解析失败
            final Claims claims = Jwts.parser().
                    setSigningKey(SECRET).
                    parseClaimsJws(token.replace(TOKEN_PREFIX, "")).getBody();
            return claims;
        }catch (Exception e){//解密失败
            //多半是用户登录过期了
            log.info("jwt token解密失败");
            return null;
        }
    }

}
