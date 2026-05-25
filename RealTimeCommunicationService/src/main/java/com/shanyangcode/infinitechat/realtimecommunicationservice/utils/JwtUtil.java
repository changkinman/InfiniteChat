package com.shanyangcode.infinitechat.realtimecommunicationservice.utils;

import com.shanyangcode.infinitechat.realtimecommunicationservice.constants.ConfigEnum;
import com.shanyangcode.infinitechat.realtimecommunicationservice.constants.TimeOutEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.Date;

public class JwtUtil {
    /**
     * 过期时间目前设置成2天，这个配置随业务需求而定
     */
    private final static Duration expiration = Duration.ofHours(TimeOutEnum.JWT_TIME_OUT.getTimeOut());


    /**
     * 生成JWT
     *
     * @param id 手机
     * @return JWT
     */
    public static String generate(String id) {
        // 过期时间
        Date expiryDate = new Date(System.currentTimeMillis() + expiration.toMillis());

        return Jwts.builder()
                .setSubject(id) // 将id放进JWT
                .setIssuedAt(new Date()) // 设置JWT签发时间
                .setExpiration(expiryDate)  // 设置过期时间
                .signWith(SignatureAlgorithm.HS512, ConfigEnum.TOKEN_SECRET_KEY.getValue()) // 设置加密算法和秘钥
                .compact();
    }

    /**
     * 解析JWT
     *
     * @param token JWT字符串
     * @return 解析成功返回Claims对象，解析失败返回null
     */
    public static Claims parse(String token) {
        if (StringUtils.isEmpty(token)){
            throw new JwtException("token 为空");
        }

        // 这个Claims对象包含了许多属性，比如签发时间、过期时间以及存放的数据等
        Claims claims = null;
        // 解析失败了会抛出异常，所以我们要捕捉一下。token过期、token非法都会导致解析失败
        claims = Jwts.parser()
                .setSigningKey(ConfigEnum.TOKEN_SECRET_KEY.getValue()) // 设置秘钥
                .parseClaimsJws(token)
                .getBody();

        return claims;
    }

}