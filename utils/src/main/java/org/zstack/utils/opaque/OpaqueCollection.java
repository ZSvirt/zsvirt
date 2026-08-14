package org.zstack.utils.opaque;

/**
 * A collection interface for storing and retrieving opaque diagnostic data.
 *
 * <p>This interface provides a fluent API for managing arbitrary key-value pairs
 * that can be attached to objects such as error codes, responses, or exceptions.
 * The opaque data is typically used for debugging, logging, or providing additional
 * context about operations without coupling the implementation to specific data structures.</p>
 *
 * <p>Implementations should support method chaining by returning the collection instance
 * from mutation methods.</p>
 *
 * @since 4.10.20
 */

public interface OpaqueCollection {
    /**
     * Stores a key-value pair in the opaque collection.
     *
     * @param key the key under which the value is stored, must not be null.
     *            Key must be a combination of lowercase alphanumeric characters and dots,
     *            e.g., "response.error", "ssh.cmd", "bash.output", "exception".
     *            Underscores are allowed in special cases, e.g., "error_code".
     * @param value the value to store, may be null
     * @return this collection instance for method chaining
     *
     * @see OpaqueConstants
     * @see OpaqueConstants#OPAQUE_KEY_EXCEPTION
     * @see OpaqueConstants#OPAQUE_KEY_RESPONSE_ERROR
     */
    public OpaqueCollection withOpaque(String key, Object value);

    /**
     * Retrieves a value from the opaque collection by its key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or null if the key is not present
     */
    public Object getFromOpaque(String key);

    /**
     * Stores all key-value pairs from the provided opaque scripts into this collection.
     *
     * <p>This default implementation iterates over the map returned by
     * {@link OpaqueScripts#opaqueScripts()} and adds each entry to this collection
     * using {@link #withOpaque(String, Object)}.</p>
     *
     * @param scripts the opaque scripts provider containing data to store
     * @return this collection instance for method chaining
     */
    default OpaqueCollection withOpaque(OpaqueScripts scripts) {
        scripts.opaqueScripts().forEach(this::withOpaque);
        return this;
    }
}
