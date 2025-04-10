package com.anymind.pos.config;

import com.anymind.pos.config.annotation.ReadOnly;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class DataSourceAspect {

    @Pointcut("execution(* com.anymind.pos.service..*.*(..))")
    public void anyControllerMethod() {
    }

    @Before("anyControllerMethod()")
    public void beforeControllerMethod(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (method.isAnnotationPresent(ReadOnly.class)) {
            RoutingDataSource.setDataSourceType("read");
            log.info("Method {}, SQL DataSource type: read", method.getName());
        } else {
            RoutingDataSource.setDataSourceType("write");
            log.info("Method {}, SQL DataSource type: write", method.getName());
        }
    }

    @After("anyControllerMethod()")
    public void afterControllerMethod() {
        RoutingDataSource.clearDataSourceType();
    }
}

