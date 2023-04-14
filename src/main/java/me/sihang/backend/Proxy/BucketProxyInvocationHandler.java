package me.sihang.backend.Proxy;

import com.couchbase.client.java.Bucket;
import me.sihang.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * <p>Bucker的动态代理</p>
 * @author liwei
 * @date 2021-3-29 15:49
 * 这个类的功能是为Bucket提供一个动态代理，在执行数据库操作的前后，打印相关log
 *
 */


public class BucketProxyInvocationHandler implements InvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BucketProxyInvocationHandler.class);

    private Bucket bucket;

    public Object getBucketProxy(Bucket bucket){
        this.bucket = bucket;
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),bucket.getClass().getInterfaces(),this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            LOGGER.info("执行了["+method.getName()+"]方法，参数为：["+args[0].toString()+"].");
        }catch (Exception e){}

        Object result = method.invoke(bucket, args);
        try {
            LOGGER.info("返回结果为["+result+"].");
        }catch (Exception e){}
        return result;
    }
}
