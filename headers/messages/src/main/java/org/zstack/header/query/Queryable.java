package org.zstack.header.query;

import javax.persistence.JoinColumn;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Queryable {
    Class mappingClass();

    JoinColumn joinColumn();
}
