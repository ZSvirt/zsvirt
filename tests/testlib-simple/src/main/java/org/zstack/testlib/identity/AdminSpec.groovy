package org.zstack.testlib.identity

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID

class AdminSpec extends Spec {
    AdminSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    void role(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RoleSpec.class) Closure c) {
        def spec = new RoleSpec(envSpec)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        return null
    }

    @Override
    void delete(String sessionId) {
    }
}
