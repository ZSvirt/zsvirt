package org.zstack.header.core.encrypt;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @Author: DaoDao
 * @Date: 2021/11/23
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface GlobalConfigEncrypt {
    String category();
    String name();

}
