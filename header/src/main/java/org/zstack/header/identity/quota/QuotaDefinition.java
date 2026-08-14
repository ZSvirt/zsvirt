package org.zstack.header.identity.quota;

/**
 * quota interface
 *
 * define quota's name default value
 * and how to get account's quota usage
 */
public interface QuotaDefinition {
    String getName();

    Long getDefaultValue();

    /**
     * get quota's usage by account
     * @param accountUuid target account need get usage
     * @return used value if return null means unavailable or
     * no usage supported
     */
    Long getQuotaUsage(String accountUuid);
}
