package org.zstack.utils.opaque;

import java.util.Map;

/**
 * A provider interface for supplying opaque diagnostic data as key-value pairs.
 *
 * <p>This interface is typically implemented by exceptions, responses, or result objects
 * that need to provide additional diagnostic information (such as command output,
 * error details, or execution context) without exposing their internal structure directly.</p>
 *
 * <p>The returned map should contain relevant debugging or diagnostic information that
 * can be attached to error codes or logged for troubleshooting purposes. Common use cases include:</p>
 * <ul>
 *   <li>SSH execution results (command, return code, stdout, stderr)</li>
 *   <li>Agent response errors</li>
 *   <li>Exception context and parameters</li>
 * </ul>
 *
 * @since 4.10.20
 * @see OpaqueCollection
 */
public interface OpaqueScripts {
    /**
     * Returns a map containing opaque diagnostic data as key-value pairs.
     *
     * <p>The keys should be descriptive strings identifying the type of data,
     * and values can be any object relevant for debugging or logging purposes.
     * Implementations should return an empty map if no diagnostic data is available.</p>
     *
     * @return a map of diagnostic data; never null but may be empty
     */
    Map<String, Object> opaqueScripts();
}
