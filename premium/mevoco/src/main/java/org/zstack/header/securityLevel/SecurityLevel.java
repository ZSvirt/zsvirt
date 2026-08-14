package org.zstack.header.securityLevel;

import org.apache.logging.log4j.util.EnglishEnums;

public enum SecurityLevel {
    LEVEL0("0"),

    LEVEL1("1"),

    LEVEL2("2"),

    LEVEL3("3");

    private final String code;

    SecurityLevel(final String code) {
        this.code = code;
    }

    public static SecurityLevel toSecurityLevel(final String name) {
        return toSecurityLevel(name, null);
    }

    /**
     * Returns the SecurityLevel for the given string.
     *
     * @param name The SecurityLevel enum name, case-insensitive. If null, returns, defaultSecurityLevel
     * @param defaultSecurityLevel the SecurityLevel to return if name is null
     * @return a SecurityLevel enum value or null if name is null
     */
    public static SecurityLevel toSecurityLevel(final String name, final SecurityLevel defaultSecurityLevel) {
        return EnglishEnums.valueOf(SecurityLevel.class, name, defaultSecurityLevel);
    }

    /**
     * Retrieve the value of the enumeration.
     * @return The value associated with the enumeration.
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Determine if this enumeration matches the specified name (ignoring case).
     * @param name The name to check.
     * @return true if the name matches this enumeration, ignoring case.
     */
    public boolean isEqual(final String name) {
        return this.name().equalsIgnoreCase(name);
    }

    public static SecurityLevel fromCode(String code) {
        SecurityLevel[] securityLevels = SecurityLevel.values();
        for (SecurityLevel securityLevel : securityLevels) {
            if (code.equals(securityLevel.getCode())) {
                return securityLevel;
            }
        }
        return null;
    }
}
