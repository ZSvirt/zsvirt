package org.zstack.header.log;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by MaJin on 2019/9/22.
 * mask sensitive info in log, usually uses for password.
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NoLogging {
    enum Behavior {
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
