package org.zstack.header.vm.metadata;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MetadataImpact {
    Impact value();

    /**
     * Spring bean id of the {@code VmUuidFromApiResolver} implementation.
     * Required when value is CONFIG or STORAGE; leave default for NONE.
     */
    String resolver() default "";

    /**
     * The API message field that the resolver reads to extract the VM UUID.
     * Required when value is CONFIG or STORAGE; leave default for NONE.
     */
    String field() default "";

    /**
     * Whether to update metadata even if the API execution fails.
     * Required when value is CONFIG or STORAGE; leave default for NONE.
     */
    boolean updateOnFailure() default false;

    enum Impact {
        NONE,
        CONFIG,
        STORAGE
    }
}
