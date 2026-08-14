package org.zstack.testlib

import groovy.transform.AutoClone

@AutoClone
class SimpleEnvSpec extends EnvSpec {
    @Override
    void delete() {
        callDeleteOnResourcesNeedDeletion()
    }

    void use(Closure c) {
        c.resolveStrategy = Closure.DELEGATE_FIRST

        def backup = Test.currentEnvSpec

        Test.currentEnvSpec = this
        create(c)
        delete()

        Test.currentEnvSpec = backup
    }

    @Override
    SimpleEnvSpec create(Closure cl = null) {
        adminLogin()

        installDeletionMethods()
        initializeMockedResource()
        installSimulatorHandlers()

        deploy()
        if (cl != null) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }

        return this
    }

    @Override
    SimpleEnvSpec more(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = EnvSpec.class) Closure c) {
        c.delegate = this
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return this
    }

    static Closure makeCreator(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = EnvSpec.class) Closure c) {
        return c
    }
}
