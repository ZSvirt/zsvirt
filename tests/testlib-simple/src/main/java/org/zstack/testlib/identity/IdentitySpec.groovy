package org.zstack.testlib.identity

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID

class IdentitySpec extends Spec {

    IdentitySpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        return null
    }

    @Override
    void delete(String sessionId) {
    }

    void admin(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = AdminSpec.class) Closure c) {
        def spec = new AdminSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        addChild(spec)
    }

    void account(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = AccountSpec.class) Closure c) {
        def spec = new AccountSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        addChild(spec)
    }
}
