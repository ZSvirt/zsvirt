package org.zstack.header.vo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 */
@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ForeignKey {
    enum ReferenceOption {
        RESTRICT("RESTRICT"),
        CASCADE("CASCADE"),
        SET_NULL("SET NULL"),
        NO_ACTION("NO ACTION");

        private String reference;

        private ReferenceOption(String reference) {
            this.reference = reference;
        }

        public String toOnDeleteSql() {
            return String.format("ON DELETE %s", reference);
        }

        public String toOnUpdateSql() {
            return String.format("ON UPDATE %s", reference);
        }

        @Override
        public String toString() {
            return reference;
        }
    }

    Class parentEntityClass();

    String parentKey() default "";

    ReferenceOption onUpdateAction() default ReferenceOption.NO_ACTION;

    ReferenceOption onDeleteAction() default ReferenceOption.NO_ACTION;
}
