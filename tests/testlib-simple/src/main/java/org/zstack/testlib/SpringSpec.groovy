package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/14.
 */
class SpringSpec {
    List<String> CORE_SERVICES = [
            "HostManager.xml",
            "ZoneManager.xml",
            "ClusterManager.xml",
            "PrimaryStorageManager.xml",
            "BackupStorageManager.xml",
            "ImageManager.xml",
            "HostAllocatorManager.xml",
            "ConfigurationManager.xml",
            "VolumeManager.xml",
            "NetworkManager.xml",
            "VmInstanceManager.xml",
            "AccountManager.xml",
            "identity.xml",
            "NetworkService.xml",
            "volumeSnapshot.xml",
            "tag.xml",
            "resourceAttributes.xml",
            "core.xml",
    ]

    Set<String> xmls = []
    boolean all

    void include(String xml) {
        xmls.add(xml)
    }

    void includeAll() {
        all = true
    }

    void includeCoreServices() {
        CORE_SERVICES.each { include(it) }
    }

    void nfsPrimaryStorage() {
        include("NfsPrimaryStorage.xml")
    }

    void localStorage() {
        include("localStorage.xml")
    }

    void externalPrimaryStorage() {
        include("ExternalPrimaryStorage.xml")
    }

    void vyos() {
        include("ApplianceVmFacade.xml")
        include("VirtualRouter.xml")
        include("vyos.xml")
    }

    void virtualRouter() {
        include("ApplianceVmFacade.xml")
        include("VirtualRouter.xml")
        include("NetworkService.xml")
        include("vip.xml")
    }

    void flatNetwork() {
        include("flatNetworkProvider.xml")
    }

    void sftpBackupStorage() {
        include("SftpBackupStorage.xml")
    }

    void eip() {
        include("vip.xml")
        include("eip.xml")
    }

    void lb() {
        include("vip.xml")
        include("acl.xml")
        include("lb.xml")
    }

    void portForwarding() {
        include("vip.xml")
        include("PortForwarding.xml")
    }


    void kvm() {
        include("Kvm.xml")
    }

    void ceph() {
        include("ceph.xml")
    }

    void expon() {
        include("expon.xml")
        include("vhost.xml")
        include("iscsi.xml")
    }

    void zbs() {
        include("zbs.xml")
        include("cbd.xml")
    }

    void smp() {
        include("sharedMountPointPrimaryStorage.xml")
    }

    void securityGroup() {
        include("SecurityGroupManager.xml")
    }

    void console() {
        include("ConsoleManager.xml")
    }
}
