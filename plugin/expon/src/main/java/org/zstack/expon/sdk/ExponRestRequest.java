package org.zstack.expon.sdk;

import org.springframework.http.HttpMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ExponRestRequest {
    String path();
    HttpMethod method();
    Class responseClass();
    String version() default "v2";
    boolean sync() default true;
    boolean hasBody() default false;
}
