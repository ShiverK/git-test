package me.sihang.backend.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * <p>日志拦截器</p>
 * @author liwei
 * @date 2021-3-29 15:49
 * 这个类有两个作用：1.在Servlet分发消息前和返回结果前打印相关的Log，2.为每次请求生成一个TRACE_ID，用于在Log中区分某一个独立的请求
 *
 */
@Component
public class LogInterceptor extends HandlerInterceptorAdapter {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    /**
     * 日志跟踪标识
     */
    private static final String TRACE_ID = "TRACE_ID";
    private static final String REMOTE_ADDR = "REMOTE_ADDR";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            String traceId = dateFormat.format(System.currentTimeMillis());
            String remoteAddr = request.getRemoteAddr();
            if (StringUtils.isEmpty(MDC.get(TRACE_ID))) {
                MDC.put(TRACE_ID, traceId);
            }
            if (StringUtils.isEmpty(MDC.get(REMOTE_ADDR))) {
                MDC.put(REMOTE_ADDR, remoteAddr);
            }
            log.info("[Get request message]:["+request.getMethod()+"] "+" [remote addr]:"+remoteAddr+" Request URI: " + request.getRequestURI()+".Query String: " + request.getQueryString());

        }catch (Exception e){}
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        try{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            String startTimeKey = MDC.get(TRACE_ID);
            long startTime = dateFormat.parse(startTimeKey).getTime();
            String endTimeKey = dateFormat.format(System.currentTimeMillis());
            long endTime = dateFormat.parse(endTimeKey).getTime();
            int millisecond = (int)(endTime - startTime);
            String remoteAddr = request.getRemoteAddr();
            log.info("[Reply to request]:"+" [remote addr]:"+remoteAddr+","+ request.getRequestURI()+",response Status:"+response.getStatus()+",elapsed time:"+String.valueOf(millisecond)+"ms.");
        }catch (Exception e){}

        MDC.remove(TRACE_ID);
        MDC.remove(REMOTE_ADDR);
    }
}
