package org.zstack.header.description.apis;

/**
 * Capabilities configured for a selection of APIs (class and/or package).
 * Null fields mean unset and do not participate in lookup pickup.
 */
public final class ApiCapabilities {
    public final String serviceId;
    /**
     * {@code Boolean.TRUE} means skip audit. {@code null} means unset.
     * Lookup on the package path takes the first {@code true}.
     */
    public final Boolean skipAudit;

    public ApiCapabilities(String serviceId, Boolean skipAudit) {
        this.serviceId = serviceId;
        this.skipAudit = skipAudit;
    }

    public boolean isEmpty() {
        return (serviceId == null || serviceId.isEmpty()) && !Boolean.TRUE.equals(skipAudit);
    }
}
