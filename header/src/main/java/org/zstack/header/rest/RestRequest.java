package org.zstack.header.rest;

import org.springframework.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xing5 on 2016/12/7.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestRequest {
    String path();
    String[] optionalPaths() default  {};
    HttpMethod method();
    boolean isAction() default false;
    String parameterName() default RESTConstant.DEFAULT_PARAMETER_NAME;
    String[] mappingFields() default {};
    Class responseClass();
    String category() default "";
}
