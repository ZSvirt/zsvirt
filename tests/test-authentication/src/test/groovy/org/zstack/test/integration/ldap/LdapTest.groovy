package org.zstack.test.integration.ldap

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test
import org.zstack.testlib.premium.PremiumEnv

class LdapTest extends Test {
    static SpringSpec springSpec = PremiumEnv.makeSpring()

    public static final String DOMAIN_DSN = "dc=example,dc=com"

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance()
            .bindingToPort(1888)
            .usingDomainDsn(DOMAIN_DSN)
            .importingLdifs("users-import.ldif")
            .build()

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
