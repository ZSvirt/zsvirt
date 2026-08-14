package org.zstack.header.storage.addon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface SingleFlightExecutor {
    /**
     * the last parameter of m must be ReturnValueCompletion, return type must be void
     */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface SingleFlight {
    }

    /**
     * @return the zstack uuid of the resource
     */
    String getResourceUuid();
}
