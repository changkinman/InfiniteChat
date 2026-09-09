package com.shanyangcode.infinitechat.authenticationservice.conf;

import com.alibaba.fastjson.JSON;
import com.shanyangcode.infinitechat.authenticationservice.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
class SourceHandler implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        X-Request-Source, InfiniteChat-GateWay
        String header = request.getHeader("X-Request-Source");
        if (!"InfiniteChat-GateWay".equals(header)){
            refuseResult(response);

            return false;
        }

        return true;
    }

    public void refuseResult(HttpServletResponse httpServletResponse) throws Exception{
        httpServletResponse.setContentType("text/html;charset=UTF-8");
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setStatus(HttpStatus.FORBIDDEN.value());
        Result<Object> result = new Result<>().setCode(40301).setMsg("非法请求来源");
        httpServletResponse.getWriter().print(JSON.toJSONString(result));
        httpServletResponse.getWriter().flush();
    }
}