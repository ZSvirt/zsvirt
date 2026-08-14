package org.zstack.header.core.encrypt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: DaoDao
 * @Date: 2023/3/9
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CovertSubClass {
    String classSimpleName();
    String columnName();
    String columnValue();
}
