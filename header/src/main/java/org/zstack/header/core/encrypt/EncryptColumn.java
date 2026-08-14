package org.zstack.header.core.encrypt;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Integrity encrypted field, the field is not encrypted.
 * @Author: DaoDao
 * @Date: 2021/11/23
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EncryptColumn {
}
