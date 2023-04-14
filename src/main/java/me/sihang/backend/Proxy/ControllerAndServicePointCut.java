package me.sihang.backend.Proxy;

import me.sihang.backend.service.NoduleService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;


/**
 * MVC配置
 * @author liwei
 * @date 2021-3-29 15:49
 *这个类的目的是对Controller和Service包下的所有类的所有方法进行切片，
 * 在方法执行前，将即将执行的方法的方法名和参数全部打印到Log中，
 * 在方法执行后，将执行的方法返回的结果进行打印
 *

 */


//用于定义配置类，可替换xml配置文件
@Configuration
////开启AspectJ 自动代理模式,如果不填proxyTargetClass=true，默认为false，
@EnableAspectJAutoProxy(proxyTargetClass=true)
@Component
@Aspect
public class ControllerAndServicePointCut {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerAndServicePointCut.class);

    //绑定Service包
    @Pointcut(" execution(* me.sihang.backend.service.*.*(..))")
    public void Service(){}

    //绑定Controller包
    @Pointcut("execution(* me.sihang.backend.controller.*.*(..))")
    public void Controller(){}


    @Around("Service() || Controller()")
    public Object Around(ProceedingJoinPoint joinPoint) throws Throwable {
        try{
            Object[] args = joinPoint.getArgs();
            String argsStr="";
            for (Object arg : joinPoint.getArgs()) {
                if(arg !=null) {
                    argsStr = argsStr + "[" + arg.toString() + "]";
                }
            }
            LOGGER.info("[开始执行]:["+joinPoint.getSignature().toString()+"],args："+argsStr);
        }catch (Exception e){}

        Object result = joinPoint.proceed();
        try{
            LOGGER.info("[执行结束]:["+joinPoint.getSignature().toString()+"]，执行返回结果为："+result);
        }catch (Exception e){}

        return result;
    }


}
