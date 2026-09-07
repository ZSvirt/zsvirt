package org.zstack.testlib.premium

import org.zstack.testlib.SpringSpec

class PremiumEnv {
    static SpringSpec makeSpring() {
        return makeSpring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
            vyos()
            flatNetwork()
            ceph()
            lb()
            nfsPrimaryStorage()
            externalPrimaryStorage()
            zbs()
            eip()
            portForwarding()
            smp()
            console()
            agents()

            include("authentications/accountImport.xml")
            include("authentications/accountImportTest.xml")
            include("authentications/ldap.xml")
            include("captcha.xml")
            include("CloudBusAopProxy.xml")
            include("ZoneManager.xml")
            include("webhook.xml")
            include("Progress.xml")
            include("vip.xml")
            include("vxlan.xml")
            include("mediateApiValidator.xml")
            include("LongJobManager.xml")
            include("log.xml")
            include("HostAllocateExtension.xml")
            include("HostNetworkManager.xml")
            include("telemetry.xml")
            include("allocatorDomain.xml")
        }
    }

    static SpringSpec makeSpring(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SpringSpec.class) Closure c) {
        def spec = new SpringSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }
}
