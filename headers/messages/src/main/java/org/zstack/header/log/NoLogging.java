package org.zstack.header.log;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Masks sensitive information in logs, usually passwords.
 * <p>
 * For {@link Behavior#Auto}, the runtime value of the annotated field must
 * implement {@link java.io.Serializable} or {@link org.zstack.header.message.Message}.
 * Log masking walks into the value only when one of those types is implemented;
 * otherwise the value is written as-is and nested sensitive fields stay in the log.
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NoLogging {
    enum Behavior {
        /**
         * Walk into the field value and mask nested sensitive fields.
         * The value must implement {@link java.io.Serializable} or
         * {@link org.zstack.header.message.Message}.
         */
        Auto,
        Mask;

        public boolean auto() {
            return this == Auto;
        }

        public boolean mask() {
            return this == Mask;
        }
    }

    enum Type {
        Simple,
        Tag,
        Uri,
        LongText,
        ;

        public boolean simple() {
            return this == Simple;
        }

        public boolean tag() {
            return this == Tag;
        }

        public boolean uri() {
            return this == Uri;
        }

        public boolean longText() {
            return this == LongText;
        }
    }

    Behavior behavior() default Behavior.Mask;

    Type type() default Type.Simple;

    String classNameField() default "";
}
