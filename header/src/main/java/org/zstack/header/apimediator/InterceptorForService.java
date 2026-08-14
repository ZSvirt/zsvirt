package org.zstack.header.apimediator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind an {@link ApiMessageInterceptor} to all portal APIs whose PackageDescription
 * resolved serviceId is one of {@link #value()}.
 * <p>
 * Replaces service-level {@code <interceptor>} declarations formerly in serviceConfig XML.
 * <p>
 * Example:
 * <pre>
 * &#64;InterceptorForService(ImageConstant.SERVICE_ID)
 * public class ImageApiInterceptor implements ApiMessageInterceptor { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterceptorForService {
    /**
     * One or more service ids (e.g. {@code "image"}, {@code ImageConstant.SERVICE_ID}).
     */
    String[] value();
}
