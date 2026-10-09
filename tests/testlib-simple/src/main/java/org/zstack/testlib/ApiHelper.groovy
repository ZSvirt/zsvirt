package org.zstack.testlib

import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.core.Platform
import org.zstack.sdk.QueryAction

abstract class ApiHelper {
    def errorOut(res) {
        assert res.error == null : "API failure: ${JSONObjectUtil.toJsonString(res.error)}"
        if (res.value.hasProperty("inventory")) {
            return res.value.inventory
        } else if (res.value.hasProperty("inventories")) {
            return res.value.inventories
        } else {
            return res.value
        }
    }

    def runAction(Class actionClass, Closure c) {
        def a = actionClass.newInstance()
        if (actionClass.fields.find { it.name == "sessionId" } != null) {
            a.sessionId = Test.currentEnvSpec?.session?.uuid
        }
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        if (QueryAction.isAssignableFrom(actionClass)) {
            a.conditions = a.conditions.collect { it.toString() }
        }

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }

            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }

            return out
        } else {
            return errorOut(a.call())
        }
    }

    def addAccessControlListEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAccessControlListEntryAction.class) Closure c) {
        runAction(org.zstack.sdk.AddAccessControlListEntryAction, c)
    }

    def addAccessControlListRedirectRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAccessControlListRedirectRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.AddAccessControlListRedirectRuleAction, c)
    }

    def addAccessControlListToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAccessControlListToLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddAccessControlListToLoadBalancerAction, c)
    }

    def addAccessControlRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAccessControlRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.AddAccessControlRuleAction, c)
    }

    def addBackendServerToServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddBackendServerToServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddBackendServerToServerGroupAction, c)
    }

    def addBackupStoragesToReplicationGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddBackupStoragesToReplicationGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddBackupStoragesToReplicationGroupAction, c)
    }

    def addBlockPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddBlockPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddBlockPrimaryStorageAction, c)
    }

    def addCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddCephBackupStorageAction, c)
    }

    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddCephPrimaryStorageAction, c)
    }

    def addCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStoragePoolAction.class) Closure c) {
        runAction(org.zstack.sdk.AddCephPrimaryStoragePoolAction, c)
    }

    def addCertificateToLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCertificateToLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddCertificateToLoadBalancerListenerAction, c)
    }

    def addDisasterImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDisasterImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddDisasterImageStoreBackupStorageAction, c)
    }

    def addDnsToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.AddDnsToL3NetworkAction, c)
    }

    def addDnsToVpcRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToVpcRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.AddDnsToVpcRouterAction, c)
    }

    def addExternalBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddExternalBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddExternalBackupStorageAction, c)
    }

    def addExternalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddExternalPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddExternalPrimaryStorageAction, c)
    }

    def addHostRouteToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddHostRouteToL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.AddHostRouteToL3NetworkAction, c)
    }

    def addHostToHostSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddHostToHostSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddHostToHostSchedulingRuleGroupAction, c)
    }

    def addImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddImageAction, c)
    }

    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddImageStoreBackupStorageAction, c)
    }

    def addIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.AddIpRangeAction, c)
    }

    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        runAction(org.zstack.sdk.AddIpRangeByNetworkCidrAction, c)
    }

    def addIpv6Range(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpv6RangeAction.class) Closure c) {
        runAction(org.zstack.sdk.AddIpv6RangeAction, c)
    }

    def addIpv6RangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpv6RangeByNetworkCidrAction.class) Closure c) {
        runAction(org.zstack.sdk.AddIpv6RangeByNetworkCidrAction, c)
    }

    def addIscsiServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIscsiServerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddIscsiServerAction, c)
    }

    def addKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        runAction(org.zstack.sdk.AddKVMHostAction, c)
    }

    def addKVMHostFromConfigFile(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostFromConfigFileAction.class) Closure c) {
        runAction(org.zstack.sdk.AddKVMHostFromConfigFileAction, c)
    }

    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddLocalPrimaryStorageAction, c)
    }

    def addLogConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLogConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.AddLogConfigurationAction, c)
    }

    def addMdevDeviceSpecToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMdevDeviceSpecToVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.AddMdevDeviceSpecToVmInstanceAction, c)
    }

    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddMonToCephBackupStorageAction, c)
    }

    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddMonToCephPrimaryStorageAction, c)
    }

    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddNfsPrimaryStorageAction, c)
    }

    def addNvmeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNvmeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddNvmeServerAction, c)
    }

    def addPciDeviceSpecToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddPciDeviceSpecToVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.AddPciDeviceSpecToVmInstanceAction, c)
    }

    def addPreconfigurationTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddPreconfigurationTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.AddPreconfigurationTemplateAction, c)
    }

    def addRemoteCidrsToIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddRemoteCidrsToIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.AddRemoteCidrsToIPsecConnectionAction, c)
    }

    def addReservedIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddReservedIpRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.AddReservedIpRangeAction, c)
    }

    def addResourceStackVmPortMonitor(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddResourceStackVmPortMonitorAction.class) Closure c) {
        runAction(org.zstack.sdk.AddResourceStackVmPortMonitorAction, c)
    }

    def addResourcesToDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddResourcesToDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.AddResourcesToDirectoryAction, c)
    }

    def addSchedulerJobGroupToSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSchedulerJobGroupToSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSchedulerJobGroupToSchedulerTriggerAction, c)
    }

    def addSchedulerJobToSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSchedulerJobToSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSchedulerJobToSchedulerTriggerAction, c)
    }

    def addSchedulerJobsToSchedulerJobGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSchedulerJobsToSchedulerJobGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSchedulerJobsToSchedulerJobGroupAction, c)
    }

    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSecurityGroupRuleAction, c)
    }

    def addServerGroupToLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddServerGroupToLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddServerGroupToLoadBalancerListenerAction, c)
    }

    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSftpBackupStorageAction, c)
    }

    def addSharedBlockGroupPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedBlockGroupPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSharedBlockGroupPrimaryStorageAction, c)
    }

    def addSharedBlockToSharedBlockGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedBlockToSharedBlockGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSharedBlockToSharedBlockGroupAction, c)
    }

    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSharedMountPointPrimaryStorageAction, c)
    }

    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSimulatorBackupStorageAction, c)
    }

    def addSimulatorHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSimulatorHostAction, c)
    }

    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.AddSimulatorPrimaryStorageAction, c)
    }

    def addStackTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddStackTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.AddStackTemplateAction, c)
    }

    def addStorageProtocol(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddStorageProtocolAction.class) Closure c) {
        runAction(org.zstack.sdk.AddStorageProtocolAction, c)
    }

    def addVRouterNetworksToOspfArea(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVRouterNetworksToOspfAreaAction.class) Closure c) {
        runAction(org.zstack.sdk.AddVRouterNetworksToOspfAreaAction, c)
    }

    def addVRouterRouteEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVRouterRouteEntryAction.class) Closure c) {
        runAction(org.zstack.sdk.AddVRouterRouteEntryAction, c)
    }

    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.AddVmNicToLoadBalancerAction, c)
    }

    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddVmNicToSecurityGroupAction, c)
    }

    def addVmToAffinityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmToAffinityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddVmToAffinityGroupAction, c)
    }

    def addVmToVmSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmToVmSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AddVmToVmSchedulingRuleGroupAction, c)
    }

    def addXDragonHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddXDragonHostAction.class) Closure c) {
        runAction(org.zstack.sdk.AddXDragonHostAction, c)
    }

    def allocateHostResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AllocateHostResourceAction.class) Closure c) {
        runAction(org.zstack.sdk.AllocateHostResourceAction, c)
    }

    def applyDRSAdvice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ApplyDRSAdviceAction.class) Closure c) {
        runAction(org.zstack.sdk.ApplyDRSAdviceAction, c)
    }

    def applyTemplateConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ApplyTemplateConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.ApplyTemplateConfigAction, c)
    }

    def attachAutoScalingTemplateToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachAutoScalingTemplateToGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachAutoScalingTemplateToGroupAction, c)
    }

    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachBackupStorageToZoneAction, c)
    }

    def attachBaremetalPxeServerToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBaremetalPxeServerToClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachBaremetalPxeServerToClusterAction, c)
    }

    def attachDataVolumeToHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToHostAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachDataVolumeToHostAction, c)
    }

    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachDataVolumeToVmAction, c)
    }

    def attachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachEipAction, c)
    }

    def attachIscsiServerToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIscsiServerToClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachIscsiServerToClusterAction, c)
    }

    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachIsoToVmInstanceAction, c)
    }

    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachL2NetworkToClusterAction, c)
    }

    def attachL2NetworkToHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToHostAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachL2NetworkToHostAction, c)
    }

    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachL3NetworkToVmAction, c)
    }

    def attachL3NetworkToVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmNicAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachL3NetworkToVmNicAction, c)
    }

    def attachL3NetworksToIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworksToIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachL3NetworksToIPsecConnectionAction, c)
    }

    def attachMdevDeviceToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachMdevDeviceToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachMdevDeviceToVmAction, c)
    }

    def attachMonitorTriggerToTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachMonitorTriggerActionToTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachMonitorTriggerActionToTriggerAction, c)
    }

    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachNetworkServiceToL3NetworkAction, c)
    }

    def attachNicToBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNicToBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachNicToBondingAction, c)
    }

    def attachNvmeServerToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNvmeServerToClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachNvmeServerToClusterAction, c)
    }

    def attachPciDeviceToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPciDeviceToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachPciDeviceToVmAction, c)
    }

    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachPortForwardingRuleAction, c)
    }

    def attachPriceTableToAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPriceTableToAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachPriceTableToAccountAction, c)
    }

    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachPrimaryStorageToClusterAction, c)
    }

    def attachScsiLunToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachScsiLunToVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachScsiLunToVmInstanceAction, c)
    }

    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachSecurityGroupToL3NetworkAction, c)
    }

    def attachSshKeyPairToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSshKeyPairToVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachSshKeyPairToVmInstanceAction, c)
    }

    def attachTagToResources(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachTagToResourcesAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachTagToResourcesAction, c)
    }

    def attachUsbDeviceToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachUsbDeviceToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachUsbDeviceToVmAction, c)
    }

    def attachVRouterRouteTableToVRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachVRouterRouteTableToVRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachVRouterRouteTableToVRouterAction, c)
    }

    def attachVmNicToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachVmNicToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.AttachVmNicToVmAction, c)
    }

    def backupStorageMigrateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.BackupStorageMigrateImageAction.class) Closure c) {
        runAction(org.zstack.sdk.BackupStorageMigrateImageAction, c)
    }

    def batchCreateBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.BatchCreateBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.BatchCreateBaremetalChassisAction, c)
    }

    def batchCreateHostKernelInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.BatchCreateHostKernelInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.BatchCreateHostKernelInterfaceAction, c)
    }

    def batchDeleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.BatchDeleteVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.BatchDeleteVolumeSnapshotAction, c)
    }

    def batchQuery(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.BatchQueryAction.class) Closure c) {
        runAction(org.zstack.sdk.BatchQueryAction, c)
    }

    def batchSyncVolumeSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.BatchSyncVolumeSizeAction.class) Closure c) {
        runAction(org.zstack.sdk.BatchSyncVolumeSizeAction, c)
    }

    def calculateAccountBillingSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountBillingSpendingAction.class) Closure c) {
        runAction(org.zstack.sdk.CalculateAccountBillingSpendingAction, c)
    }

    def calculateAccountSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        runAction(org.zstack.sdk.CalculateAccountSpendingAction, c)
    }

    def calculateImageHash(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateImageHashAction.class) Closure c) {
        runAction(org.zstack.sdk.CalculateImageHashAction, c)
    }

    def calculateResourceSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateResourceSpendingAction.class) Closure c) {
        runAction(org.zstack.sdk.CalculateResourceSpendingAction, c)
    }

    def cancelLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CancelLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.CancelLongJobAction, c)
    }

    def changeAccessControlListRedirectRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeAccessControlListRedirectRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeAccessControlListRedirectRuleAction, c)
    }

    def changeAccessControlListServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeAccessControlListServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeAccessControlListServerGroupAction, c)
    }

    def changeAccessKeyState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeAccessKeyStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeAccessKeyStateAction, c)
    }

    def changeAccountPriceTableBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeAccountPriceTableBindingAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeAccountPriceTableBindingAction, c)
    }

    def changeAccountType(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeAccountTypeAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeAccountTypeAction, c)
    }

    def changeAffinityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeAffinityGroupStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeAffinityGroupStateAction, c)
    }

    def changeAutoScalingGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeAutoScalingGroupStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeAutoScalingGroupStateAction, c)
    }

    def changeBackupStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeBackupStorageStateAction, c)
    }

    def changeBaremetalChassisState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBaremetalChassisStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeBaremetalChassisStateAction, c)
    }

    def changeClusterState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeClusterStateAction, c)
    }

    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeDiskOfferingStateAction, c)
    }

    def changeEipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeEipStateAction, c)
    }

    def changeHostNetworkInterfaceLldpMode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostNetworkInterfaceLldpModeAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeHostNetworkInterfaceLldpModeAction, c)
    }

    def changeHostPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostPasswordAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeHostPasswordAction, c)
    }

    def changeHostState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeHostStateAction, c)
    }

    def changeIPSecConnectionState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeIPSecConnectionStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeIPSecConnectionStateAction, c)
    }

    def changeIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeIPsecConnectionAction, c)
    }

    def changeImageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeImageStateAction, c)
    }

    def changeInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeInstanceOfferingAction, c)
    }

    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeInstanceOfferingStateAction, c)
    }

    def changeL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkDhcpIpAddressAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeL3NetworkDhcpIpAddressAction, c)
    }

    def changeL3NetworkState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeL3NetworkStateAction, c)
    }

    def changeLoadBalancerBackendServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeLoadBalancerBackendServerAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeLoadBalancerBackendServerAction, c)
    }

    def changeLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeLoadBalancerListenerAction, c)
    }

    def changeMediaState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeMediaStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeMediaStateAction, c)
    }

    def changeMonitorTriggerStateAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeMonitorTriggerActionStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeMonitorTriggerActionStateAction, c)
    }

    def changeMonitorTriggerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeMonitorTriggerStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeMonitorTriggerStateAction, c)
    }

    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangePortForwardingRuleStateAction, c)
    }

    def changePreconfigurationTemplateState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePreconfigurationTemplateStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangePreconfigurationTemplateStateAction, c)
    }

    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangePrimaryStorageStateAction, c)
    }

    def changeResourceOwner(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeResourceOwnerAction, c)
    }

    def changeSchedulerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSchedulerStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeSchedulerStateAction, c)
    }

    def changeSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeSecurityGroupRuleAction, c)
    }

    def changeSecurityGroupRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupRuleStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeSecurityGroupRuleStateAction, c)
    }

    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeSecurityGroupStateAction, c)
    }

    def changeVfNicHaState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVfNicHaStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVfNicHaStateAction, c)
    }

    def changeVipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVipStateAction, c)
    }

    def changeVmImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmImageAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVmImageAction, c)
    }

    def changeVmNicNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmNicNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVmNicNetworkAction, c)
    }

    def changeVmNicSecurityPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmNicSecurityPolicyAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVmNicSecurityPolicyAction, c)
    }

    def changeVmNicState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmNicStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVmNicStateAction, c)
    }

    def changeVmNicType(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmNicTypeAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVmNicTypeAction, c)
    }

    def changeVmPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVmPasswordAction, c)
    }

    def changeVmSchedulingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmSchedulingRuleStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVmSchedulingRuleStateAction, c)
    }

    def changeVolumeEncryption(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeEncryptionAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVolumeEncryptionAction, c)
    }

    def changeVolumeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVolumeStateAction, c)
    }

    def changeVpcHaGroupMonitorIps(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVpcHaGroupMonitorIpsAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeVpcHaGroupMonitorIpsAction, c)
    }

    def changeZoneState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ChangeZoneStateAction, c)
    }

    def checkBaremetalChassisConfigFile(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckBaremetalChassisConfigFileAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckBaremetalChassisConfigFileAction, c)
    }

    def checkCephHealthStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckCephHealthStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckCephHealthStatusAction, c)
    }

    def checkIpAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckIpAvailabilityAction, c)
    }

    def checkKVMHostConfigFile(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckKVMHostConfigFileAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckKVMHostConfigFileAction, c)
    }

    def checkMemorySnapshotGroupConflict(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckMemorySnapshotGroupConflictAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckMemorySnapshotGroupConflictAction, c)
    }

    def checkNetworkReachable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckNetworkReachableAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckNetworkReachableAction, c)
    }

    def checkScsiLunClusterStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckScsiLunClusterStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckScsiLunClusterStatusAction, c)
    }

    def checkStackTemplateParameters(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckStackTemplateParametersAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckStackTemplateParametersAction, c)
    }

    def checkVipPortAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckVipPortAvailabilityAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckVipPortAvailabilityAction, c)
    }

    def checkVolumeSnapshotGroupAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckVolumeSnapshotGroupAvailabilityAction.class) Closure c) {
        runAction(org.zstack.sdk.CheckVolumeSnapshotGroupAvailabilityAction, c)
    }

    def cleanLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanLongJobAction, c)
    }

    def cleanQueue(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanQueueAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanQueueAction, c)
    }

    def cleanUpBaremetalChassisBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpBaremetalChassisBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanUpBaremetalChassisBondingAction, c)
    }

    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction, c)
    }

    def cleanUpStorageTrashOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpStorageTrashOnPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanUpStorageTrashOnPrimaryStorageAction, c)
    }

    def cleanUpTrashOnBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpTrashOnBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanUpTrashOnBackupStorageAction, c)
    }

    def cleanUpTrashOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpTrashOnPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanUpTrashOnPrimaryStorageAction, c)
    }

    def cleanupAllVmInstanceMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanupAllVmInstanceMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanupAllVmInstanceMetadataAction, c)
    }

    def cleanupBillingUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanupBillingUsageAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanupBillingUsageAction, c)
    }

    def cleanupVmInstanceMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanupVmInstanceMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.CleanupVmInstanceMetadataAction, c)
    }

    def cloneVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.CloneVmInstanceAction, c)
    }

    def convertTemplatedVmInstanceToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ConvertTemplatedVmInstanceToVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.ConvertTemplatedVmInstanceToVmInstanceAction, c)
    }

    def convertVmInstanceToTemplatedVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ConvertVmInstanceToTemplatedVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.ConvertVmInstanceToTemplatedVmInstanceAction, c)
    }

    def createAccessControlList(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccessControlListAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAccessControlListAction, c)
    }

    def createAccessKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccessKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAccessKeyAction, c)
    }

    def createAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAccountAction, c)
    }

    def createAffinityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAffinityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAffinityGroupAction, c)
    }

    def createAutoScalingGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAutoScalingGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAutoScalingGroupAction, c)
    }

    def createAutoScalingGroupAddingNewInstanceRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAutoScalingGroupAddingNewInstanceRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAutoScalingGroupAddingNewInstanceRuleAction, c)
    }

    def createAutoScalingGroupRemovalInstanceRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAutoScalingGroupRemovalInstanceRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAutoScalingGroupRemovalInstanceRuleAction, c)
    }

    def createAutoScalingRuleAlarmTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAutoScalingRuleAlarmTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAutoScalingRuleAlarmTriggerAction, c)
    }

    def createAutoScalingRuleSchedulerJobTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAutoScalingRuleSchedulerJobTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAutoScalingRuleSchedulerJobTriggerAction, c)
    }

    def createAutoScalingVmTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAutoScalingVmTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateAutoScalingVmTemplateAction, c)
    }

    def createBaremetalBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateBaremetalBondingAction, c)
    }

    def createBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateBaremetalChassisAction, c)
    }

    def createBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateBaremetalInstanceAction, c)
    }

    def createBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalPxeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateBaremetalPxeServerAction, c)
    }

    def createBlockVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBlockVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateBlockVolumeAction, c)
    }

    def createBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateBondingAction, c)
    }

    def createCasClient(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateCasClientAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateCasClientAction, c)
    }

    def createCbtTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateCbtTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateCbtTaskAction, c)
    }

    def createCertificate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateCertificateAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateCertificateAction, c)
    }

    def createCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateClusterAction, c)
    }

    def createClusterDRS(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterDRSAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateClusterDRSAction, c)
    }

    def createDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateDataVolumeAction, c)
    }

    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction, c)
    }

    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction, c)
    }

    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction, c)
    }

    def createDataVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateDataVolumeTemplateFromVolumeSnapshotAction, c)
    }

    def createDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateDirectoryAction, c)
    }

    def createDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateDiskOfferingAction, c)
    }

    def createEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateEipAction, c)
    }

    def createEmailMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEmailMediaAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateEmailMediaAction, c)
    }

    def createEmailMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEmailMonitorTriggerActionAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateEmailMonitorTriggerActionAction, c)
    }

    def createHostKernelInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateHostKernelInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateHostKernelInterfaceAction, c)
    }

    def createHostSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateHostSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateHostSchedulingRuleGroupAction, c)
    }

    def createIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateIPsecConnectionAction, c)
    }

    def createImageReplicationGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateImageReplicationGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateImageReplicationGroupAction, c)
    }

    def createInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateInstanceOfferingAction, c)
    }

    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateL2NoVlanNetworkAction, c)
    }

    def createL2PortGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2PortGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateL2PortGroupAction, c)
    }

    def createL2VirtualSwitch(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VirtualSwitchAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateL2VirtualSwitchAction, c)
    }

    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateL2VlanNetworkAction, c)
    }

    def createL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateL2VxlanNetworkAction, c)
    }

    def createL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkPoolAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateL2VxlanNetworkPoolAction, c)
    }

    def createL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateL3NetworkAction, c)
    }

    def createLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateLoadBalancerAction, c)
    }

    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateLoadBalancerListenerAction, c)
    }

    def createLoadBalancerServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateLoadBalancerServerGroupAction, c)
    }

    def createMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateMonitorTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateMonitorTriggerAction, c)
    }

    def createOAuthClient(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateOAuthClientAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateOAuthClientAction, c)
    }

    def createPciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePciDeviceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.CreatePciDeviceOfferingAction, c)
    }

    def createPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePortForwardingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.CreatePortForwardingRuleAction, c)
    }

    def createPortGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePortGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreatePortGroupAction, c)
    }

    def createPriceTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePriceTableAction.class) Closure c) {
        runAction(org.zstack.sdk.CreatePriceTableAction, c)
    }

    def createResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateResourcePriceAction, c)
    }

    def createResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateResourceStackAction, c)
    }

    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction, c)
    }

    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction, c)
    }

    def createSSORedirectTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSSORedirectTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSSORedirectTemplateAction, c)
    }

    def createSchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSchedulerJobAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSchedulerJobAction, c)
    }

    def createSchedulerJobGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSchedulerJobGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSchedulerJobGroupAction, c)
    }

    def createSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSchedulerTriggerAction, c)
    }

    def createSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSecurityGroupAction, c)
    }

    def createSnmpAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSnmpAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSnmpAgentAction, c)
    }

    def createSshKeyPair(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSshKeyPairAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSshKeyPairAction, c)
    }

    def createSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSystemTagAction, c)
    }

    def createSystemTags(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagsAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateSystemTagsAction, c)
    }

    def createTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateTagAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateTagAction, c)
    }

    def createTemplatedVmInstanceFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateTemplatedVmInstanceFromVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateTemplatedVmInstanceFromVmInstanceAction, c)
    }

    def createVRouterOspfArea(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVRouterOspfAreaAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVRouterOspfAreaAction, c)
    }

    def createVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVRouterRouteTableAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVRouterRouteTableAction, c)
    }

    def createVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVipAction, c)
    }

    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVirtualRouterOfferingAction, c)
    }

    def createVmCdRom(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmCdRomAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmCdRomAction, c)
    }

    def createVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmInstanceAction, c)
    }

    def createVmInstanceFromOvf(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceFromOvfAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmInstanceFromOvfAction, c)
    }

    def createVmInstanceFromTemplatedVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceFromTemplatedVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmInstanceFromTemplatedVmInstanceAction, c)
    }

    def createVmInstanceFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceFromVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmInstanceFromVolumeAction, c)
    }

    def createVmInstanceFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceFromVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmInstanceFromVolumeSnapshotAction, c)
    }

    def createVmInstanceFromVolumeSnapshotGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceFromVolumeSnapshotGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmInstanceFromVolumeSnapshotGroupAction, c)
    }

    def createVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmNicAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmNicAction, c)
    }

    def createVmSchedulingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmSchedulingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmSchedulingRuleAction, c)
    }

    def createVmSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVmSchedulingRuleGroupAction, c)
    }

    def createVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVniRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVniRangeAction, c)
    }

    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVolumeSnapshotAction, c)
    }

    def createVolumeSnapshotGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVolumeSnapshotGroupAction, c)
    }

    def createVolumesSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumesSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVolumesSnapshotAction, c)
    }

    def createVpcHaGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVpcHaGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVpcHaGroupAction, c)
    }

    def createVpcVRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVpcVRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVpcVRouterAction, c)
    }

    def createVxlanPoolRemoteVtep(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVxlanPoolRemoteVtepAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVxlanPoolRemoteVtepAction, c)
    }

    def createVxlanVtep(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVxlanVtepAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateVxlanVtepAction, c)
    }

    def createWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateWebhookAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateWebhookAction, c)
    }

    def createZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.CreateZoneAction, c)
    }

    def debugSignal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DebugSignalAction.class) Closure c) {
        runAction(org.zstack.sdk.DebugSignalAction, c)
    }

    def decodeStackTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DecodeStackTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.DecodeStackTemplateAction, c)
    }

    def deleteAccessControlList(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccessControlListAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAccessControlListAction, c)
    }

    def deleteAccessControlRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccessControlRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAccessControlRuleAction, c)
    }

    def deleteAccessKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccessKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAccessKeyAction, c)
    }

    def deleteAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAccountAction, c)
    }

    def deleteAffinityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAffinityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAffinityGroupAction, c)
    }

    def deleteAlert(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAlertAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAlertAction, c)
    }

    def deleteAutoScalingGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAutoScalingGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAutoScalingGroupAction, c)
    }

    def deleteAutoScalingGroupInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAutoScalingGroupInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAutoScalingGroupInstanceAction, c)
    }

    def deleteAutoScalingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAutoScalingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAutoScalingRuleAction, c)
    }

    def deleteAutoScalingRuleTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAutoScalingRuleTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAutoScalingRuleTriggerAction, c)
    }

    def deleteAutoScalingTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAutoScalingTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteAutoScalingTemplateAction, c)
    }

    def deleteBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteBackupStorageAction, c)
    }

    def deleteBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteBaremetalChassisAction, c)
    }

    def deleteBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalPxeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteBaremetalPxeServerAction, c)
    }

    def deleteBilling(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBillingAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteBillingAction, c)
    }

    def deleteBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteBondingAction, c)
    }

    def deleteCbtTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCbtTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteCbtTaskAction, c)
    }

    def deleteCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCephPrimaryStoragePoolAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteCephPrimaryStoragePoolAction, c)
    }

    def deleteCertificate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCertificateAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteCertificateAction, c)
    }

    def deleteCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteClusterAction, c)
    }

    def deleteClusterDRS(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterDRSAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteClusterDRSAction, c)
    }

    def deleteDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteDataVolumeAction, c)
    }

    def deleteDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteDirectoryAction, c)
    }

    def deleteDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteDiskOfferingAction, c)
    }

    def deleteEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteEipAction, c)
    }

    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteExportedImageFromBackupStorageAction, c)
    }

    def deleteExternalBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExternalBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteExternalBackupAction, c)
    }

    def deleteGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteGCJobAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteGCJobAction, c)
    }

    def deleteHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteHostAction, c)
    }

    def deleteHostKernelInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostKernelInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteHostKernelInterfaceAction, c)
    }

    def deleteHostSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteHostSchedulingRuleGroupAction, c)
    }

    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteIPsecConnectionAction, c)
    }

    def deleteImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteImageAction, c)
    }

    def deleteImagePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImagePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteImagePackageAction, c)
    }

    def deleteImageReplicationGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageReplicationGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteImageReplicationGroupAction, c)
    }

    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteInstanceOfferingAction, c)
    }

    def deleteIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpAddressAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteIpAddressAction, c)
    }

    def deleteIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteIpRangeAction, c)
    }

    def deleteIscsiServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIscsiServerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteIscsiServerAction, c)
    }

    def deleteL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteL2NetworkAction, c)
    }

    def deleteL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteL3NetworkAction, c)
    }

    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteLoadBalancerAction, c)
    }

    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteLoadBalancerListenerAction, c)
    }

    def deleteLoadBalancerServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteLoadBalancerServerGroupAction, c)
    }

    def deleteLogConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLogConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteLogConfigurationAction, c)
    }

    def deleteLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteLongJobAction, c)
    }

    def deleteMdevDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteMdevDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteMdevDeviceAction, c)
    }

    def deleteMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteMediaAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteMediaAction, c)
    }

    def deleteMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteMonitorTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteMonitorTriggerAction, c)
    }

    def deleteMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteMonitorTriggerActionAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteMonitorTriggerActionAction, c)
    }

    def deleteNasFileSystem(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNasFileSystemAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteNasFileSystemAction, c)
    }

    def deleteNasMountTarget(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNasMountTargetAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteNasMountTargetAction, c)
    }

    def deleteNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNicQosAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteNicQosAction, c)
    }

    def deleteNvmeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNvmeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteNvmeServerAction, c)
    }

    def deletePciDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePciDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.DeletePciDeviceAction, c)
    }

    def deletePciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePciDeviceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.DeletePciDeviceOfferingAction, c)
    }

    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.DeletePortForwardingRuleAction, c)
    }

    def deletePortGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeletePortGroupAction, c)
    }

    def deletePreconfigurationTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePreconfigurationTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.DeletePreconfigurationTemplateAction, c)
    }

    def deletePriceTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePriceTableAction.class) Closure c) {
        runAction(org.zstack.sdk.DeletePriceTableAction, c)
    }

    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.DeletePrimaryStorageAction, c)
    }

    def deleteReservedIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteReservedIpRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteReservedIpRangeAction, c)
    }

    def deleteResourceConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourceConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteResourceConfigAction, c)
    }

    def deleteResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourcePriceAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteResourcePriceAction, c)
    }

    def deleteResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteResourceStackAction, c)
    }

    def deleteResourceStackVmPortMonitor(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourceStackVmPortMonitorAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteResourceStackVmPortMonitorAction, c)
    }

    def deleteSSOClient(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSSOClientAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSSOClientAction, c)
    }

    def deleteSSORedirectTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSSORedirectTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSSORedirectTemplateAction, c)
    }

    def deleteSchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerJobAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSchedulerJobAction, c)
    }

    def deleteSchedulerJobGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerJobGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSchedulerJobGroupAction, c)
    }

    def deleteSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSchedulerTriggerAction, c)
    }

    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSecurityGroupAction, c)
    }

    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSecurityGroupRuleAction, c)
    }

    def deleteSshKeyPair(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSshKeyPairAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteSshKeyPairAction, c)
    }

    def deleteStackTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteStackTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteStackTemplateAction, c)
    }

    def deleteTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteTagAction, c)
    }

    def deleteTemplatedVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTemplatedVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteTemplatedVmInstanceAction, c)
    }

    def deleteVRouterOspfArea(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVRouterOspfAreaAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVRouterOspfAreaAction, c)
    }

    def deleteVRouterRouteEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVRouterRouteEntryAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVRouterRouteEntryAction, c)
    }

    def deleteVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVRouterRouteTableAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVRouterRouteTableAction, c)
    }

    def deleteVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVipAction, c)
    }

    def deleteVipQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipQosAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVipQosAction, c)
    }

    def deleteVmBootMode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmBootModeAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmBootModeAction, c)
    }

    def deleteVmCdRom(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmCdRomAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmCdRomAction, c)
    }

    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmConsolePasswordAction, c)
    }

    def deleteVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmHostnameAction, c)
    }

    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmInstanceHaLevelAction, c)
    }

    def deleteVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmNicAction, c)
    }

    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmNicFromSecurityGroupAction, c)
    }

    def deleteVmSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmSchedulingRuleGroupAction, c)
    }

    def deleteVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmSshKeyAction, c)
    }

    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmStaticIpAction, c)
    }

    def deleteVmUserDefinedXml(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmUserDefinedXmlAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmUserDefinedXmlAction, c)
    }

    def deleteVmUserDefinedXmlHookScript(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmUserDefinedXmlHookScriptAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVmUserDefinedXmlHookScriptAction, c)
    }

    def deleteVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVniRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVniRangeAction, c)
    }

    def deleteVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeQosAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVolumeQosAction, c)
    }

    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVolumeSnapshotAction, c)
    }

    def deleteVolumeSnapshotGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVolumeSnapshotGroupAction, c)
    }

    def deleteVpcHaGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcHaGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVpcHaGroupAction, c)
    }

    def deleteVxlanL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVxlanL2NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVxlanL2NetworkAction, c)
    }

    def deleteVxlanPoolRemoteVtep(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVxlanPoolRemoteVtepAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteVxlanPoolRemoteVtepAction, c)
    }

    def deleteWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteWebhookAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteWebhookAction, c)
    }

    def deleteZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.DeleteZoneAction, c)
    }

    def destroyBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.DestroyBaremetalInstanceAction, c)
    }

    def destroyVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.DestroyVmInstanceAction, c)
    }

    def detachAutoScalingTemplateFromGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachAutoScalingTemplateFromGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachAutoScalingTemplateFromGroupAction, c)
    }

    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachBackupStorageFromZoneAction, c)
    }

    def detachBaremetalPxeServerFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBaremetalPxeServerFromClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachBaremetalPxeServerFromClusterAction, c)
    }

    def detachDataVolumeFromHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromHostAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachDataVolumeFromHostAction, c)
    }

    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachDataVolumeFromVmAction, c)
    }

    def detachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachEipAction, c)
    }

    def detachHostFromHostSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachHostFromHostSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachHostFromHostSchedulingRuleGroupAction, c)
    }

    def detachIscsiServerFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIscsiServerFromClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachIscsiServerFromClusterAction, c)
    }

    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachIsoFromVmInstanceAction, c)
    }

    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachL2NetworkFromClusterAction, c)
    }

    def detachL2NetworkFromHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromHostAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachL2NetworkFromHostAction, c)
    }

    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachL3NetworkFromVmAction, c)
    }

    def detachL3NetworksFromIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworksFromIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachL3NetworksFromIPsecConnectionAction, c)
    }

    def detachMdevDeviceFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachMdevDeviceFromVmAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachMdevDeviceFromVmAction, c)
    }

    def detachMonitorTriggerFromTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachMonitorTriggerActionFromTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachMonitorTriggerActionFromTriggerAction, c)
    }

    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction, c)
    }

    def detachNicFromBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNicFromBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachNicFromBondingAction, c)
    }

    def detachNvmeServerFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNvmeServerFromClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachNvmeServerFromClusterAction, c)
    }

    def detachPciDeviceFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPciDeviceFromVmAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachPciDeviceFromVmAction, c)
    }

    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachPortForwardingRuleAction, c)
    }

    def detachPriceTableFromAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPriceTableFromAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachPriceTableFromAccountAction, c)
    }

    def detachPrimaryStorageFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPrimaryStorageFromClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachPrimaryStorageFromClusterAction, c)
    }

    def detachScsiLunFromHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachScsiLunFromHostAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachScsiLunFromHostAction, c)
    }

    def detachScsiLunFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachScsiLunFromVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachScsiLunFromVmInstanceAction, c)
    }

    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction, c)
    }

    def detachSshKeyPairFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSshKeyPairFromVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachSshKeyPairFromVmInstanceAction, c)
    }

    def detachTagFromResources(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachTagFromResourcesAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachTagFromResourcesAction, c)
    }

    def detachUsbDeviceFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachUsbDeviceFromVmAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachUsbDeviceFromVmAction, c)
    }

    def detachVRouterRouteTableFromVRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachVRouterRouteTableFromVRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachVRouterRouteTableFromVRouterAction, c)
    }

    def detachVmFromVmSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachVmFromVmSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.DetachVmFromVmSchedulingRuleGroupAction, c)
    }

    def disableCbtTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DisableCbtTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.DisableCbtTaskAction, c)
    }

    def discoverExternalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DiscoverExternalPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.DiscoverExternalPrimaryStorageAction, c)
    }

    def discoverStrangePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DiscoverStrangePrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.DiscoverStrangePrimaryStorageAction, c)
    }

    def enableCbtTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.EnableCbtTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.EnableCbtTaskAction, c)
    }

    def executeAutoScalingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExecuteAutoScalingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.ExecuteAutoScalingRuleAction, c)
    }

    def executeDRSScheduling(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExecuteDRSSchedulingAction.class) Closure c) {
        runAction(org.zstack.sdk.ExecuteDRSSchedulingAction, c)
    }

    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.ExportImageFromBackupStorageAction, c)
    }

    def exportNbdVolumes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportNbdVolumesAction.class) Closure c) {
        runAction(org.zstack.sdk.ExportNbdVolumesAction, c)
    }

    def exportVmOvaPackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportVmOvaPackageAction.class) Closure c) {
        runAction(org.zstack.sdk.ExportVmOvaPackageAction, c)
    }

    def expungeBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.ExpungeBaremetalInstanceAction, c)
    }

    def expungeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.ExpungeDataVolumeAction, c)
    }

    def expungeImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        runAction(org.zstack.sdk.ExpungeImageAction, c)
    }

    def expungeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.ExpungeVmInstanceAction, c)
    }

    def flattenVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.FlattenVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.FlattenVmInstanceAction, c)
    }

    def flattenVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.FlattenVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.FlattenVolumeAction, c)
    }

    def fstrimVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.FstrimVmAction.class) Closure c) {
        runAction(org.zstack.sdk.FstrimVmAction, c)
    }

    def generateAccountBilling(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GenerateAccountBillingAction.class) Closure c) {
        runAction(org.zstack.sdk.GenerateAccountBillingAction, c)
    }

    def generateMdevDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GenerateMdevDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.GenerateMdevDevicesAction, c)
    }

    def generateSeMdevDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GenerateSeMdevDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.GenerateSeMdevDevicesAction, c)
    }

    def generateSriovPciDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GenerateSriovPciDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.GenerateSriovPciDevicesAction, c)
    }

    def generateSshKeyPair(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GenerateSshKeyPairAction.class) Closure c) {
        runAction(org.zstack.sdk.GenerateSshKeyPairAction, c)
    }

    def getAccessPath(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccessPathAction.class) Closure c) {
        runAction(org.zstack.sdk.GetAccessPathAction, c)
    }

    def getAccountPriceTableRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountPriceTableRefAction.class) Closure c) {
        runAction(org.zstack.sdk.GetAccountPriceTableRefAction, c)
    }

    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        runAction(org.zstack.sdk.GetAccountQuotaUsageAction, c)
    }

    def getAttachablePublicL3ForVRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAttachablePublicL3ForVRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.GetAttachablePublicL3ForVRouterAction, c)
    }

    def getAttachableVpcL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAttachableVpcL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.GetAttachableVpcL3NetworkAction, c)
    }

    def getAvailableTriggers(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAvailableTriggersAction.class) Closure c) {
        runAction(org.zstack.sdk.GetAvailableTriggersAction, c)
    }

    def getBackupStorageCandidatesForImageMigration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCandidatesForImageMigrationAction.class) Closure c) {
        runAction(org.zstack.sdk.GetBackupStorageCandidatesForImageMigrationAction, c)
    }

    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.GetBackupStorageCapacityAction, c)
    }

    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction, c)
    }

    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction, c)
    }

    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetBackupStorageTypesAction, c)
    }

    def getBaremetalChassisPowerStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBaremetalChassisPowerStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.GetBaremetalChassisPowerStatusAction, c)
    }

    def getBlockPrimaryStorageMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBlockPrimaryStorageMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.GetBlockPrimaryStorageMetadataAction, c)
    }

    def getCandidateAffinityGroupForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateAffinityGroupForAttachingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateAffinityGroupForAttachingVmAction, c)
    }

    def getCandidateAffinityGroupForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateAffinityGroupForCreatingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateAffinityGroupForCreatingVmAction, c)
    }

    def getCandidateBackupStorageForCreatingImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateBackupStorageForCreatingImageAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateBackupStorageForCreatingImageAction, c)
    }

    def getCandidateClustersForAttachingL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateClustersForAttachingL2NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateClustersForAttachingL2NetworkAction, c)
    }

    def getCandidateHostKernelInterfaces(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateHostKernelInterfacesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateHostKernelInterfacesAction, c)
    }

    def getCandidateImagesForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateImagesForCreatingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateImagesForCreatingVmAction, c)
    }

    def getCandidateInterfaceVlanIds(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateInterfaceVlanIdsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateInterfaceVlanIdsAction, c)
    }

    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateIsoForAttachingVmAction, c)
    }

    def getCandidateL2NetworksForAttachingCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateL2NetworksForAttachingClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateL2NetworksForAttachingClusterAction, c)
    }

    def getCandidateL3NetworksForChangeVmNicNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateL3NetworksForChangeVmNicNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateL3NetworksForChangeVmNicNetworkAction, c)
    }

    def getCandidateL3NetworksForIpSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateL3NetworksForIpSecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateL3NetworksForIpSecConnectionAction, c)
    }

    def getCandidateL3NetworksForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateL3NetworksForLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateL3NetworksForLoadBalancerAction, c)
    }

    def getCandidateL3NetworksForServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateL3NetworksForServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateL3NetworksForServerGroupAction, c)
    }

    def getCandidateNetworkBondings(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateNetworkBondingsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateNetworkBondingsAction, c)
    }

    def getCandidateNetworkInterfaces(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateNetworkInterfacesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateNetworkInterfacesAction, c)
    }

    def getCandidatePrimaryStoragesForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmAction, c)
    }

    def getCandidateVMForAttachingAffinityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVMForAttachingAffinityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateVMForAttachingAffinityGroupAction, c)
    }

    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateVmForAttachingIsoAction, c)
    }

    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction, c)
    }

    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction, c)
    }

    def getCandidateVmNicsForLoadBalancerServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateVmNicsForLoadBalancerServerGroupAction, c)
    }

    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction, c)
    }

    def getChainTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetChainTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.GetChainTaskAction, c)
    }

    def getChronyServers(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetChronyServersAction.class) Closure c) {
        runAction(org.zstack.sdk.GetChronyServersAction, c)
    }

    def getClusterDRSStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetClusterDRSStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.GetClusterDRSStatusAction, c)
    }

    def getClusterHostNetworkFacts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetClusterHostNetworkFactsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetClusterHostNetworkFactsAction, c)
    }

    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCpuMemoryCapacityAction, c)
    }

    def getCurrentTime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetCurrentTimeAction, c)
    }

    def getDataVolumeAttachableVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataVolumeAttachableVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetDataVolumeAttachableVmAction, c)
    }

    def getDebugSignal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDebugSignalAction.class) Closure c) {
        runAction(org.zstack.sdk.GetDebugSignalAction, c)
    }

    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetEipAttachableVmNicsAction, c)
    }

    def getEncryptedField(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEncryptedFieldAction.class) Closure c) {
        runAction(org.zstack.sdk.GetEncryptedFieldAction, c)
    }

    def getFactoryModeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFactoryModeStateAction.class) Closure c) {
        runAction(org.zstack.sdk.GetFactoryModeStateAction, c)
    }

    def getFreeIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpAction.class) Closure c) {
        runAction(org.zstack.sdk.GetFreeIpAction, c)
    }

    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetFreeIpOfIpRangeAction, c)
    }

    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.GetFreeIpOfL3NetworkAction, c)
    }

    def getGlobalConfigOptions(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetGlobalConfigOptionsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetGlobalConfigOptionsAction, c)
    }

    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostAllocatorStrategiesAction, c)
    }

    def getHostBlockDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostBlockDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostBlockDevicesAction, c)
    }

    def getHostCandidatesForVmMigration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostCandidatesForVmMigrationAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostCandidatesForVmMigrationAction, c)
    }

    def getHostIommuState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostIommuStateAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostIommuStateAction, c)
    }

    def getHostIommuStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostIommuStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostIommuStatusAction, c)
    }

    def getHostMultipathTopology(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostMultipathTopologyAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostMultipathTopologyAction, c)
    }

    def getHostNUMATopology(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostNUMATopologyAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostNUMATopologyAction, c)
    }

    def getHostNetworkFacts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostNetworkFactsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostNetworkFactsAction, c)
    }

    def getHostNetworkInterfaceLldp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostNetworkInterfaceLldpAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostNetworkInterfaceLldpAction, c)
    }

    def getHostPhysicalMemoryFacts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostPhysicalMemoryFactsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostPhysicalMemoryFactsAction, c)
    }

    def getHostPowerStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostPowerStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostPowerStatusAction, c)
    }

    def getHostResourceAllocation(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostResourceAllocationAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostResourceAllocationAction, c)
    }

    def getHostSensors(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostSensorsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostSensorsAction, c)
    }

    def getHostTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostTaskAction, c)
    }

    def getHostWebSshUrl(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostWebSshUrlAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHostWebSshUrlAction, c)
    }

    def getHypervisorTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetHypervisorTypesAction, c)
    }

    def getImageCandidatesForVmToChange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageCandidatesForVmToChangeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetImageCandidatesForVmToChangeAction, c)
    }

    def getImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageQgaAction.class) Closure c) {
        runAction(org.zstack.sdk.GetImageQgaAction, c)
    }

    def getImagesFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImagesFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.GetImagesFromImageStoreBackupStorageAction, c)
    }

    def getInterdependentL3NetworksBackupStorages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksBackupStoragesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetInterdependentL3NetworksBackupStoragesAction, c)
    }

    def getInterdependentL3NetworksImages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksImagesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetInterdependentL3NetworksImagesAction, c)
    }

    def getInterfaceServiceTypeStatistic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterfaceServiceTypeStatisticAction.class) Closure c) {
        runAction(org.zstack.sdk.GetInterfaceServiceTypeStatisticAction, c)
    }

    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.GetIpAddressCapacityAction, c)
    }

    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetL2NetworkTypesAction, c)
    }

    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        runAction(org.zstack.sdk.GetL3NetworkDhcpIpAddressAction, c)
    }

    def getL3NetworkIpStatistic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkIpStatisticAction.class) Closure c) {
        runAction(org.zstack.sdk.GetL3NetworkIpStatisticAction, c)
    }

    def getL3NetworkMtu(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkMtuAction.class) Closure c) {
        runAction(org.zstack.sdk.GetL3NetworkMtuAction, c)
    }

    def getL3NetworkRouterInterfaceIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkRouterInterfaceIpAction.class) Closure c) {
        runAction(org.zstack.sdk.GetL3NetworkRouterInterfaceIpAction, c)
    }

    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetL3NetworkTypesAction, c)
    }

    def getLoadBalancerListenerACLEntries(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLoadBalancerListenerACLEntriesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetLoadBalancerListenerACLEntriesAction, c)
    }

    def getLocalRaidPhysicalDriveSmart(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalRaidPhysicalDriveSmartAction.class) Closure c) {
        runAction(org.zstack.sdk.GetLocalRaidPhysicalDriveSmartAction, c)
    }

    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.GetLocalStorageHostDiskCapacityAction, c)
    }

    def getLogConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLogConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.GetLogConfigurationAction, c)
    }

    def getLoginCaptcha(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLoginCaptchaAction.class) Closure c) {
        runAction(org.zstack.sdk.GetLoginCaptchaAction, c)
    }

    def getLoginProcedures(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLoginProceduresAction.class) Closure c) {
        runAction(org.zstack.sdk.GetLoginProceduresAction, c)
    }

    def getManagementNodeArch(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetManagementNodeArchAction.class) Closure c) {
        runAction(org.zstack.sdk.GetManagementNodeArchAction, c)
    }

    def getManagementNodeOS(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetManagementNodeOSAction.class) Closure c) {
        runAction(org.zstack.sdk.GetManagementNodeOSAction, c)
    }

    def getMdevDeviceCandidates(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetMdevDeviceCandidatesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetMdevDeviceCandidatesAction, c)
    }

    def getMdevDeviceSpecCandidates(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetMdevDeviceSpecCandidatesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetMdevDeviceSpecCandidatesAction, c)
    }

    def getMemorySnapshotGroupReference(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetMemorySnapshotGroupReferenceAction.class) Closure c) {
        runAction(org.zstack.sdk.GetMemorySnapshotGroupReferenceAction, c)
    }

    def getMonitorItem(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetMonitorItemAction.class) Closure c) {
        runAction(org.zstack.sdk.GetMonitorItemAction, c)
    }

    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetNetworkServiceTypesAction, c)
    }

    def getNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNicQosAction.class) Closure c) {
        runAction(org.zstack.sdk.GetNicQosAction, c)
    }

    def getNoTriggerSchedulerJobs(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNoTriggerSchedulerJobsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetNoTriggerSchedulerJobsAction, c)
    }

    def getOAuth2Token(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOAuth2TokenAction.class) Closure c) {
        runAction(org.zstack.sdk.GetOAuth2TokenAction, c)
    }

    def getOAuthClientSecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOAuthClientSecretAction.class) Closure c) {
        runAction(org.zstack.sdk.GetOAuthClientSecretAction, c)
    }

    def getPciDeviceCandidatesForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPciDeviceCandidatesForAttachingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPciDeviceCandidatesForAttachingVmAction, c)
    }

    def getPciDeviceCandidatesForNewCreateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPciDeviceCandidatesForNewCreateVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPciDeviceCandidatesForNewCreateVmAction, c)
    }

    def getPciDeviceSpecCandidates(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPciDeviceSpecCandidatesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPciDeviceSpecCandidatesAction, c)
    }

    def getPhysicalMachineBlockDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPhysicalMachineBlockDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPhysicalMachineBlockDevicesAction, c)
    }

    def getPlatformTimeZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPlatformTimeZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPlatformTimeZoneAction, c)
    }

    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPortForwardingAttachableVmNicsAction, c)
    }

    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction, c)
    }

    def getPrimaryStorageCandidatesForVmMigration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCandidatesForVmMigrationAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPrimaryStorageCandidatesForVmMigrationAction, c)
    }

    def getPrimaryStorageCandidatesForVolumeMigration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCandidatesForVolumeMigrationAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPrimaryStorageCandidatesForVolumeMigrationAction, c)
    }

    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPrimaryStorageCapacityAction, c)
    }

    def getPrimaryStorageLicenseInfo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageLicenseInfoAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPrimaryStorageLicenseInfoAction, c)
    }

    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPrimaryStorageTypesAction, c)
    }

    def getPrimaryStorageUsageReport(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageUsageReportAction.class) Closure c) {
        runAction(org.zstack.sdk.GetPrimaryStorageUsageReportAction, c)
    }

    def getResourceAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceAccountAction, c)
    }

    def getResourceBindableConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceBindableConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceBindableConfigAction, c)
    }

    def getResourceConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceConfigAction, c)
    }

    def getResourceConfigs(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceConfigsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceConfigsAction, c)
    }

    def getResourceFromResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceFromResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceFromResourceStackAction, c)
    }

    def getResourceNames(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceNamesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceNamesAction, c)
    }

    def getResourceStackFromResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceStackFromResourceAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceStackFromResourceAction, c)
    }

    def getResourceStackVmStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceStackVmStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.GetResourceStackVmStatusAction, c)
    }

    def getRouteTableVpcVRouterCandidate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetRouteTableVpcVRouterCandidateAction.class) Closure c) {
        runAction(org.zstack.sdk.GetRouteTableVpcVRouterCandidateAction, c)
    }

    def getSSOClient(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSSOClientAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSSOClientAction, c)
    }

    def getSchedulerExecutionReport(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSchedulerExecutionReportAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSchedulerExecutionReportAction, c)
    }

    def getScsiLunCandidatesForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetScsiLunCandidatesForAttachingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetScsiLunCandidatesForAttachingVmAction, c)
    }

    def getSharedBlockCandidate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSharedBlockCandidateAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSharedBlockCandidateAction, c)
    }

    def getSharedBlockVolumeSnapshotLanFreeLayout(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSharedBlockVolumeSnapshotLanFreeLayoutAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSharedBlockVolumeSnapshotLanFreeLayoutAction, c)
    }

    def getSharedBlockVolumeLanFreeLayout(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSharedBlockVolumeLanFreeLayoutAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSharedBlockVolumeLanFreeLayoutAction, c)
    }

    def getSpiceCertificates(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSpiceCertificatesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSpiceCertificatesAction, c)
    }

    def getSupportAPIs(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSupportAPIsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSupportAPIsAction, c)
    }

    def getSupportedCloudFormationResources(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetSupportedCloudFormationResourcesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetSupportedCloudFormationResourcesAction, c)
    }

    def getTaskProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTaskProgressAction.class) Closure c) {
        runAction(org.zstack.sdk.GetTaskProgressAction, c)
    }

    def getTrashOnBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTrashOnBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.GetTrashOnBackupStorageAction, c)
    }

    def getTrashOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTrashOnPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.GetTrashOnPrimaryStorageAction, c)
    }

    def getTwoFactorAuthenticationSecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTwoFactorAuthenticationSecretAction.class) Closure c) {
        runAction(org.zstack.sdk.GetTwoFactorAuthenticationSecretAction, c)
    }

    def getTwoFactorAuthenticationState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTwoFactorAuthenticationStateAction.class) Closure c) {
        runAction(org.zstack.sdk.GetTwoFactorAuthenticationStateAction, c)
    }

    def getUploadImageJobDetails(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetUploadImageJobDetailsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetUploadImageJobDetailsAction, c)
    }

    def getUsbDeviceCandidatesForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetUsbDeviceCandidatesForAttachingVmAction.class) Closure c) {
        runAction(org.zstack.sdk.GetUsbDeviceCandidatesForAttachingVmAction, c)
    }

    def getVRouterOspfNeighbor(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVRouterOspfNeighborAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVRouterOspfNeighborAction, c)
    }

    def getVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVRouterRouteTableAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVRouterRouteTableAction, c)
    }

    def getVRouterRouterId(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVRouterRouterIdAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVRouterRouterIdAction, c)
    }

    def getVSwitchTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVSwitchTypesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVSwitchTypesAction, c)
    }

    def getVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVersionAction, c)
    }

    def getVipAvailablePort(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVipAvailablePortAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVipAvailablePortAction, c)
    }

    def getVipQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVipQosAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVipQosAction, c)
    }

    def getVipUsedPorts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVipUsedPortsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVipUsedPortsAction, c)
    }

    def getVirtualRouterSoftwareVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVirtualRouterSoftwareVersionAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVirtualRouterSoftwareVersionAction, c)
    }

    def getVirtualizerInfo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVirtualizerInfoAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVirtualizerInfoAction, c)
    }

    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmAttachableDataVolumeAction, c)
    }

    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmAttachableL3NetworkAction, c)
    }

    def getVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmBootOrderAction, c)
    }

    def getVmCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmCapabilitiesAction, c)
    }

    def getVmConsoleAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmConsoleAddressAction, c)
    }

    def getVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmConsolePasswordAction, c)
    }

    def getVmDeviceAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmDeviceAddressAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmDeviceAddressAction, c)
    }

    def getVmDns(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmDnsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmDnsAction, c)
    }

    def getVmEmulatorPinning(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmEmulatorPinningAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmEmulatorPinningAction, c)
    }

    def getVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmHostnameAction, c)
    }

    def getVmInstanceFirstBootDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceFirstBootDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmInstanceFirstBootDeviceAction, c)
    }

    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmInstanceHaLevelAction, c)
    }

    def getVmInstanceMetadataFromPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceMetadataFromPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmInstanceMetadataFromPrimaryStorageAction, c)
    }

    def getVmMigrationCandidateHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMigrationCandidateHostsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmMigrationCandidateHostsAction, c)
    }

    def getVmMonitorNumber(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMonitorNumberAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmMonitorNumberAction, c)
    }

    def getVmNicAttachableEips(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmNicAttachableEipsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmNicAttachableEipsAction, c)
    }

    def getVmNicAttachedNetworkService(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmNicAttachedNetworkServiceAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmNicAttachedNetworkServiceAction, c)
    }

    def getVmNuma(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmNumaAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmNumaAction, c)
    }

    def getVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmQgaAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmQgaAction, c)
    }

    def getVmRDP(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmRDPAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmRDPAction, c)
    }

    def getVmSchedulingRulesExecuteState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSchedulingRulesExecuteStateAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmSchedulingRulesExecuteStateAction, c)
    }

    def getVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmSshKeyAction, c)
    }

    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmStartingCandidateClustersHostsAction, c)
    }

    def getVmTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmTaskAction, c)
    }

    def getVmUptime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmUptimeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmUptimeAction, c)
    }

    def getVmUsbRedirect(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmUsbRedirectAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmUsbRedirectAction, c)
    }

    def getVmXml(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmXmlAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmXmlAction, c)
    }

    def getVmXmlHookScript(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmXmlHookScriptAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmXmlHookScriptAction, c)
    }

    def getVmsCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmsCapabilitiesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmsCapabilitiesAction, c)
    }

    def getVmsSchedulingStateFromSchedulingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmsSchedulingStateFromSchedulingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmsSchedulingStateFromSchedulingRuleAction, c)
    }

    def getVmvNUMATopology(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmvNUMATopologyAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVmvNUMATopologyAction, c)
    }

    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVolumeCapabilitiesAction, c)
    }

    def getVolumeFormat(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVolumeFormatAction, c)
    }

    def getVolumeIoThreadPin(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeIoThreadPinAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVolumeIoThreadPinAction, c)
    }

    def getVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeQosAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVolumeQosAction, c)
    }

    def getVolumeSnapshotGroupTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeSnapshotGroupTreeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVolumeSnapshotGroupTreeAction, c)
    }

    def getVolumeSnapshotSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeSnapshotSizeAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVolumeSnapshotSizeAction, c)
    }

    def getVpcAttachedEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcAttachedEipAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcAttachedEipAction, c)
    }

    def getVpcAttachedIpsec(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcAttachedIpsecAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcAttachedIpsecAction, c)
    }

    def getVpcAttachedLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcAttachedLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcAttachedLoadBalancerAction, c)
    }

    def getVpcAttachedOspf(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcAttachedOspfAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcAttachedOspfAction, c)
    }

    def getVpcAttachedPortForwardingRules(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcAttachedPortForwardingRulesAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcAttachedPortForwardingRulesAction, c)
    }

    def getVpcAttachedVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcAttachedVipAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcAttachedVipAction, c)
    }

    def getVpcIPsecLog(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcIPsecLogAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcIPsecLogAction, c)
    }

    def getVpcVRouterDistributedRoutingConnections(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcVRouterDistributedRoutingConnectionsAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcVRouterDistributedRoutingConnectionsAction, c)
    }

    def getVpcVRouterDistributedRoutingEnabled(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcVRouterDistributedRoutingEnabledAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcVRouterDistributedRoutingEnabledAction, c)
    }

    def getVpcVRouterNetworkServiceState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcVRouterNetworkServiceStateAction.class) Closure c) {
        runAction(org.zstack.sdk.GetVpcVRouterNetworkServiceStateAction, c)
    }

    def getZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.GetZoneAction, c)
    }

    def identifyHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IdentifyHostAction.class) Closure c) {
        runAction(org.zstack.sdk.IdentifyHostAction, c)
    }

    def inspectBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.InspectBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.InspectBaremetalChassisAction, c)
    }

    def isOpensourceVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsOpensourceVersionAction.class) Closure c) {
        runAction(org.zstack.sdk.IsOpensourceVersionAction, c)
    }

    def isReadyToGo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        runAction(org.zstack.sdk.IsReadyToGoAction, c)
    }

    def isVfNicAvailableInL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsVfNicAvailableInL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.IsVfNicAvailableInL3NetworkAction, c)
    }

    def kvmRunShell(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        runAction(org.zstack.sdk.KvmRunShellAction, c)
    }

    def listVmSchedulingRulesFromExecuteState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ListVmSchedulingRulesFromExecuteStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ListVmSchedulingRulesFromExecuteStateAction, c)
    }

    def listVmsFromSchedulingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ListVmsFromSchedulingStateAction.class) Closure c) {
        runAction(org.zstack.sdk.ListVmsFromSchedulingStateAction, c)
    }

    def localStorageGetVolumeMigratableHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction.class) Closure c) {
        runAction(org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction, c)
    }

    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.LocalStorageMigrateVolumeAction, c)
    }

    def locateHostNetworkInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocateHostNetworkInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.LocateHostNetworkInterfaceAction, c)
    }

    def locateLocalRaidPhysicalDrive(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocateLocalRaidPhysicalDriveAction.class) Closure c) {
        runAction(org.zstack.sdk.LocateLocalRaidPhysicalDriveAction, c)
    }

    def logIn(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInAction.class) Closure c) {
        runAction(org.zstack.sdk.LogInAction, c)
    }

    def logInByAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.LogInByAccountAction, c)
    }

    def logOut(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        runAction(org.zstack.sdk.LogOutAction, c)
    }

    def migrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        runAction(org.zstack.sdk.MigrateVmAction, c)
    }

    def mountBlockDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MountBlockDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.MountBlockDeviceAction, c)
    }

    def moveDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MoveDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.MoveDirectoryAction, c)
    }

    def moveResourcesToDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MoveResourcesToDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.MoveResourcesToDirectoryAction, c)
    }

    def parseOvf(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ParseOvfAction.class) Closure c) {
        runAction(org.zstack.sdk.ParseOvfAction, c)
    }

    def pauseVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.PauseVmInstanceAction, c)
    }

    def powerOffBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOffBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.PowerOffBaremetalChassisAction, c)
    }

    def powerOffHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOffHostAction.class) Closure c) {
        runAction(org.zstack.sdk.PowerOffHostAction, c)
    }

    def powerOnBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOnBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.PowerOnBaremetalChassisAction, c)
    }

    def powerOnHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOnHostAction.class) Closure c) {
        runAction(org.zstack.sdk.PowerOnHostAction, c)
    }

    def powerResetBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerResetBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.PowerResetBaremetalChassisAction, c)
    }

    def powerResetHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerResetHostAction.class) Closure c) {
        runAction(org.zstack.sdk.PowerResetHostAction, c)
    }

    def previewResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PreviewResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.PreviewResourceStackAction, c)
    }

    def primaryStorageMigrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrimaryStorageMigrateVmAction.class) Closure c) {
        runAction(org.zstack.sdk.PrimaryStorageMigrateVmAction, c)
    }

    def primaryStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrimaryStorageMigrateVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.PrimaryStorageMigrateVolumeAction, c)
    }

    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        runAction(org.zstack.sdk.PrometheusQueryLabelValuesAction, c)
    }

    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.PrometheusQueryMetadataAction, c)
    }

    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        runAction(org.zstack.sdk.PrometheusQueryPassThroughAction, c)
    }

    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        runAction(org.zstack.sdk.PrometheusQueryVmMonitoringDataAction, c)
    }

    def provisionVirtualRouterConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ProvisionVirtualRouterConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.ProvisionVirtualRouterConfigAction, c)
    }

    def queryAccessControlList(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccessControlListAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAccessControlListAction, c)
    }

    def queryAccessControlRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccessControlRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAccessControlRuleAction, c)
    }

    def queryAccessKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccessKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAccessKeyAction, c)
    }

    def queryAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAccountAction, c)
    }

    def queryAccountBilling(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountBillingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAccountBillingAction, c)
    }

    def queryAccountPriceTableRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountPriceTableRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAccountPriceTableRefAction, c)
    }

    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAccountResourceRefAction, c)
    }

    def queryAddressPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAddressPoolAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAddressPoolAction, c)
    }

    def queryAffinityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAffinityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAffinityGroupAction, c)
    }

    def queryAlert(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAlertAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAlertAction, c)
    }

    def queryApplianceVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryApplianceVmAction, c)
    }

    def queryAutoScalingGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAutoScalingGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAutoScalingGroupAction, c)
    }

    def queryAutoScalingGroupActivity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAutoScalingGroupActivityAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAutoScalingGroupActivityAction, c)
    }

    def queryAutoScalingGroupInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAutoScalingGroupInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAutoScalingGroupInstanceAction, c)
    }

    def queryAutoScalingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAutoScalingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAutoScalingRuleAction, c)
    }

    def queryAutoScalingRuleTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAutoScalingRuleTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAutoScalingRuleTriggerAction, c)
    }

    def queryAutoScalingVmTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAutoScalingVmTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryAutoScalingVmTemplateAction, c)
    }

    def queryBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryBackupStorageAction, c)
    }

    def queryBaremetalBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryBaremetalBondingAction, c)
    }

    def queryBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryBaremetalChassisAction, c)
    }

    def queryBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryBaremetalInstanceAction, c)
    }

    def queryBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalPxeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryBaremetalPxeServerAction, c)
    }

    def queryBlockPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBlockPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryBlockPrimaryStorageAction, c)
    }

    def queryBlockVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBlockVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryBlockVolumeAction, c)
    }

    def queryCbtTask(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCbtTaskAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryCbtTaskAction, c)
    }

    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryCephBackupStorageAction, c)
    }

    def queryCephOsdGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephOsdGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryCephOsdGroupAction, c)
    }

    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryCephPrimaryStorageAction, c)
    }

    def queryCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStoragePoolAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryCephPrimaryStoragePoolAction, c)
    }

    def queryCertificate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCertificateAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryCertificateAction, c)
    }

    def queryCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryClusterAction, c)
    }

    def queryClusterDRS(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterDRSAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryClusterDRSAction, c)
    }

    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryConsoleProxyAgentAction, c)
    }

    def queryDRSAdvice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDRSAdviceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryDRSAdviceAction, c)
    }

    def queryDRSVmMigrationActivity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDRSVmMigrationActivityAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryDRSVmMigrationActivityAction, c)
    }

    def queryDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryDirectoryAction, c)
    }

    def queryDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryDiskOfferingAction, c)
    }

    def queryEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryEipAction, c)
    }

    def queryEmailMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEmailMediaAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryEmailMediaAction, c)
    }

    def queryEmailTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEmailTriggerActionAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryEmailTriggerActionAction, c)
    }

    def queryEthernetVF(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEthernetVFAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryEthernetVFAction, c)
    }

    def queryEventFromResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEventFromResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryEventFromResourceStackAction, c)
    }

    def queryEventLog(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEventLogAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryEventLogAction, c)
    }

    def queryExponBlockVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryExponBlockVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryExponBlockVolumeAction, c)
    }

    def queryExternalBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryExternalBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryExternalBackupAction, c)
    }

    def queryFcHbaDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFcHbaDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryFcHbaDeviceAction, c)
    }

    def queryFiberChannelLun(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFiberChannelLunAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryFiberChannelLunAction, c)
    }

    def queryFiberChannelStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFiberChannelStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryFiberChannelStorageAction, c)
    }

    def queryGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGCJobAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryGCJobAction, c)
    }

    def queryGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryGlobalConfigAction, c)
    }

    def queryGlobalConfigTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryGlobalConfigTemplateAction, c)
    }

    def queryGpuDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGpuDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryGpuDeviceAction, c)
    }

    def queryHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostAction, c)
    }

    def queryHostKernelInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostKernelInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostKernelInterfaceAction, c)
    }

    def queryHostNetworkBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostNetworkBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostNetworkBondingAction, c)
    }

    def queryHostNetworkInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostNetworkInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostNetworkInterfaceAction, c)
    }

    def queryHostNetworkInterfaceLldp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostNetworkInterfaceLldpAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostNetworkInterfaceLldpAction, c)
    }

    def queryHostOsCategory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostOsCategoryAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostOsCategoryAction, c)
    }

    def queryHostPhysicalCpu(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostPhysicalCpuAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostPhysicalCpuAction, c)
    }

    def queryHostPhysicalMemory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostPhysicalMemoryAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostPhysicalMemoryAction, c)
    }

    def queryHostSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryHostSchedulingRuleGroupAction, c)
    }

    def queryIPSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryIPSecConnectionAction, c)
    }

    def queryImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryImageAction, c)
    }

    def queryImageCache(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageCacheAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryImageCacheAction, c)
    }

    def queryImagePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImagePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryImagePackageAction, c)
    }

    def queryImageReplicationGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageReplicationGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryImageReplicationGroupAction, c)
    }

    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryImageStoreBackupStorageAction, c)
    }

    def queryInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryInstanceOfferingAction, c)
    }

    def queryIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpAddressAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryIpAddressAction, c)
    }

    def queryIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryIpRangeAction, c)
    }

    def queryIscsiLun(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIscsiLunAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryIscsiLunAction, c)
    }

    def queryIscsiServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIscsiServerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryIscsiServerAction, c)
    }

    def queryKvmHypervisorInfo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryKvmHypervisorInfoAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryKvmHypervisorInfoAction, c)
    }

    def queryL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryL2NetworkAction, c)
    }

    def queryL2PortGroupNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2PortGroupNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryL2PortGroupNetworkAction, c)
    }

    def queryL2VirtualSwitchNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VirtualSwitchNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryL2VirtualSwitchNetworkAction, c)
    }

    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryL2VlanNetworkAction, c)
    }

    def queryL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryL2VxlanNetworkAction, c)
    }

    def queryL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkPoolAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryL2VxlanNetworkPoolAction, c)
    }

    def queryL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryL3NetworkAction, c)
    }

    def queryLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryLoadBalancerAction, c)
    }

    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryLoadBalancerListenerAction, c)
    }

    def queryLoadBalancerServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryLoadBalancerServerGroupAction, c)
    }

    def queryLocalRaidController(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalRaidControllerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryLocalRaidControllerAction, c)
    }

    def queryLocalRaidPhysicalDrive(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalRaidPhysicalDriveAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryLocalRaidPhysicalDriveAction, c)
    }

    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryLocalStorageResourceRefAction, c)
    }

    def queryLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryLongJobAction, c)
    }

    def queryManagementNode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryManagementNodeAction, c)
    }

    def queryMdevDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMdevDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryMdevDeviceAction, c)
    }

    def queryMdevDeviceSpec(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMdevDeviceSpecAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryMdevDeviceSpecAction, c)
    }

    def queryMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMediaAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryMediaAction, c)
    }

    def queryMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMonitorTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryMonitorTriggerAction, c)
    }

    def queryMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMonitorTriggerActionAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryMonitorTriggerActionAction, c)
    }

    def queryMttyDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMttyDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryMttyDeviceAction, c)
    }

    def queryNasFileSystem(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNasFileSystemAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryNasFileSystemAction, c)
    }

    def queryNasMountTarget(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNasMountTargetAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryNasMountTargetAction, c)
    }

    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction, c)
    }

    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryNetworkServiceProviderAction, c)
    }

    def queryNvmeLun(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNvmeLunAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryNvmeLunAction, c)
    }

    def queryNvmeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNvmeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryNvmeServerAction, c)
    }

    def queryNvmeTarget(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNvmeTargetAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryNvmeTargetAction, c)
    }

    def queryPciDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPciDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPciDeviceAction, c)
    }

    def queryPciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPciDeviceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPciDeviceOfferingAction, c)
    }

    def queryPciDevicePciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPciDevicePciDeviceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPciDevicePciDeviceOfferingAction, c)
    }

    def queryPciDeviceSpec(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPciDeviceSpecAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPciDeviceSpecAction, c)
    }

    def queryPhysicalDriveSelfTestHistory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPhysicalDriveSelfTestHistoryAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPhysicalDriveSelfTestHistoryAction, c)
    }

    def queryPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPortForwardingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPortForwardingRuleAction, c)
    }

    def queryPortGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPortGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPortGroupAction, c)
    }

    def queryPreconfigurationTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPreconfigurationTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPreconfigurationTemplateAction, c)
    }

    def queryPriceTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPriceTableAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPriceTableAction, c)
    }

    def queryPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryPrimaryStorageAction, c)
    }

    def queryQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryQuotaAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryQuotaAction, c)
    }

    def queryResourceConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryResourceConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryResourceConfigAction, c)
    }

    def queryResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryResourcePriceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryResourcePriceAction, c)
    }

    def queryResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryResourceStackAction, c)
    }

    def querySchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerJobAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySchedulerJobAction, c)
    }

    def querySchedulerJobGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerJobGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySchedulerJobGroupAction, c)
    }

    def querySchedulerJobHistory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerJobHistoryAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySchedulerJobHistoryAction, c)
    }

    def querySchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySchedulerTriggerAction, c)
    }

    def queryScsiLun(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryScsiLunAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryScsiLunAction, c)
    }

    def querySecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySecurityGroupAction, c)
    }

    def querySecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySecurityGroupRuleAction, c)
    }

    def querySftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySftpBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySftpBackupStorageAction, c)
    }

    def queryShareableVolumeVmInstanceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryShareableVolumeVmInstanceRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryShareableVolumeVmInstanceRefAction, c)
    }

    def querySharedBlock(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySharedBlockAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySharedBlockAction, c)
    }

    def querySharedBlockGroupPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySharedBlockGroupPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySharedBlockGroupPrimaryStorageAction, c)
    }

    def querySharedBlockGroupPrimaryStorageHostRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySharedBlockGroupPrimaryStorageHostRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySharedBlockGroupPrimaryStorageHostRefAction, c)
    }

    def querySnmpAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySnmpAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySnmpAgentAction, c)
    }

    def querySshKeyPair(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySshKeyPairAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySshKeyPairAction, c)
    }

    def queryStackTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryStackTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryStackTemplateAction, c)
    }

    def querySystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySystemTagAction.class) Closure c) {
        runAction(org.zstack.sdk.QuerySystemTagAction, c)
    }

    def queryTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryTagAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryTagAction, c)
    }

    def queryTemplateConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryTemplateConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryTemplateConfigAction, c)
    }

    def queryTemplatedVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryTemplatedVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryTemplatedVmInstanceAction, c)
    }

    def queryTwoFactorAuthentication(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryTwoFactorAuthenticationAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryTwoFactorAuthenticationAction, c)
    }

    def queryUplinkGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUplinkGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryUplinkGroupAction, c)
    }

    def queryUsbDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUsbDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryUsbDeviceAction, c)
    }

    def queryUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserTagAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryUserTagAction, c)
    }

    def queryVRouterOspfArea(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVRouterOspfAreaAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVRouterOspfAreaAction, c)
    }

    def queryVRouterOspfNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVRouterOspfNetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVRouterOspfNetworkAction, c)
    }

    def queryVRouterRouteEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVRouterRouteEntryAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVRouterRouteEntryAction, c)
    }

    def queryVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVRouterRouteTableAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVRouterRouteTableAction, c)
    }

    def queryVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVipAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVipAction, c)
    }

    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVirtualRouterOfferingAction, c)
    }

    def queryVirtualRouterVRouterRouteTableRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVRouterRouteTableRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVirtualRouterVRouterRouteTableRefAction, c)
    }

    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVirtualRouterVmAction, c)
    }

    def queryVmCdRom(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmCdRomAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmCdRomAction, c)
    }

    def queryVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmInstanceAction, c)
    }

    def queryVmInstanceMdevDeviceSpecRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceMdevDeviceSpecRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmInstanceMdevDeviceSpecRefAction, c)
    }

    def queryVmInstancePciDeviceSpecRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstancePciDeviceSpecRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmInstancePciDeviceSpecRefAction, c)
    }

    def queryVmInstanceResourceMetadataArchive(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceResourceMetadataArchiveAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmInstanceResourceMetadataArchiveAction, c)
    }

    def queryVmInstanceResourceMetadataGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceResourceMetadataGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmInstanceResourceMetadataGroupAction, c)
    }

    def queryVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmNicAction, c)
    }

    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmNicInSecurityGroupAction, c)
    }

    def queryVmNicSecurityPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicSecurityPolicyAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmNicSecurityPolicyAction, c)
    }

    def queryVmPriorityConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmPriorityConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmPriorityConfigAction, c)
    }

    def queryVmSchedHistory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmSchedHistoryAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmSchedHistoryAction, c)
    }

    def queryVmSchedulingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmSchedulingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmSchedulingRuleAction, c)
    }

    def queryVmSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVmSchedulingRuleGroupAction, c)
    }

    def queryVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVniRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVniRangeAction, c)
    }

    def queryVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVolumeAction, c)
    }

    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVolumeSnapshotAction, c)
    }

    def queryVolumeSnapshotGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVolumeSnapshotGroupAction, c)
    }

    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVolumeSnapshotTreeAction, c)
    }

    def queryVpcHaGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcHaGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVpcHaGroupAction, c)
    }

    def queryVpcHaGroupNetworkServiceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcHaGroupNetworkServiceRefAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVpcHaGroupNetworkServiceRefAction, c)
    }

    def queryVpcRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVpcRouterAction, c)
    }

    def queryVpcSnatState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcSnatStateAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVpcSnatStateAction, c)
    }

    def queryVtep(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVtepAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryVtepAction, c)
    }

    def queryWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryWebhookAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryWebhookAction, c)
    }

    def queryXskyBlockVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryXskyBlockVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryXskyBlockVolumeAction, c)
    }

    def queryZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.QueryZoneAction, c)
    }

    def rebootBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.RebootBaremetalInstanceAction, c)
    }

    def rebootVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.RebootVmInstanceAction, c)
    }

    def reclaimSpaceFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReclaimSpaceFromImageStoreAction.class) Closure c) {
        runAction(org.zstack.sdk.ReclaimSpaceFromImageStoreAction, c)
    }

    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectBackupStorageAction, c)
    }

    def reconnectBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBaremetalPxeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectBaremetalPxeServerAction, c)
    }

    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectConsoleProxyAgentAction, c)
    }

    def reconnectHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectHostAction, c)
    }

    def reconnectIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectIPsecConnectionAction, c)
    }

    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectImageStoreBackupStorageAction, c)
    }

    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectPrimaryStorageAction, c)
    }

    def reconnectSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectSftpBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectSftpBackupStorageAction, c)
    }

    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.ReconnectVirtualRouterAction, c)
    }

    def recoverBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.RecoverBaremetalInstanceAction, c)
    }

    def recoverDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.RecoverDataVolumeAction, c)
    }

    def recoverImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        runAction(org.zstack.sdk.RecoverImageAction, c)
    }

    def recoverVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.RecoverVmInstanceAction, c)
    }

    def recoveryImageFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoveryImageFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.RecoveryImageFromImageStoreBackupStorageAction, c)
    }

    def refreshCaptcha(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshCaptchaAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshCaptchaAction, c)
    }

    def refreshFiberChannelStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshFiberChannelStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshFiberChannelStorageAction, c)
    }

    def refreshIscsiServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshIscsiServerAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshIscsiServerAction, c)
    }

    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshLoadBalancerAction, c)
    }

    def refreshLocalRaid(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLocalRaidAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshLocalRaidAction, c)
    }

    def refreshNvmeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshNvmeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshNvmeServerAction, c)
    }

    def refreshNvmeTarget(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshNvmeTargetAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshNvmeTargetAction, c)
    }

    def refreshSearchIndexes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshSearchIndexesAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshSearchIndexesAction, c)
    }

    def refreshSharedblockDeviceCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshSharedblockDeviceCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.RefreshSharedblockDeviceCapacityAction, c)
    }

    def registerVmInstanceFromMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RegisterVmInstanceFromMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.RegisterVmInstanceFromMetadataAction, c)
    }

    def reimageVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReimageVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.ReimageVmInstanceAction, c)
    }

    def removeAccessControlListEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveAccessControlListEntryAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveAccessControlListEntryAction, c)
    }

    def removeAccessControlListFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveAccessControlListFromLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveAccessControlListFromLoadBalancerAction, c)
    }

    def removeBackendServerFromServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveBackendServerFromServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveBackendServerFromServerGroupAction, c)
    }

    def removeCertificateFromLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveCertificateFromLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveCertificateFromLoadBalancerListenerAction, c)
    }

    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveDnsFromL3NetworkAction, c)
    }

    def removeDnsFromVpcRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromVpcRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveDnsFromVpcRouterAction, c)
    }

    def removeHostRouteFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveHostRouteFromL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveHostRouteFromL3NetworkAction, c)
    }

    def removeMdevDeviceSpecFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMdevDeviceSpecFromVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveMdevDeviceSpecFromVmInstanceAction, c)
    }

    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveMonFromCephBackupStorageAction, c)
    }

    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction, c)
    }

    def removePciDeviceSpecFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemovePciDeviceSpecFromVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.RemovePciDeviceSpecFromVmInstanceAction, c)
    }

    def removeRemoteCidrsFromIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveRemoteCidrsFromIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveRemoteCidrsFromIPsecConnectionAction, c)
    }

    def removeResourcesFromDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveResourcesFromDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveResourcesFromDirectoryAction, c)
    }

    def removeSchedulerJobFromSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveSchedulerJobFromSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveSchedulerJobFromSchedulerTriggerAction, c)
    }

    def removeSchedulerJobGroupFromSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveSchedulerJobGroupFromSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveSchedulerJobGroupFromSchedulerTriggerAction, c)
    }

    def removeSchedulerJobsFromSchedulerJobGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveSchedulerJobsFromSchedulerJobGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveSchedulerJobsFromSchedulerJobGroupAction, c)
    }

    def removeServerGroupFromLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveServerGroupFromLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveServerGroupFromLoadBalancerListenerAction, c)
    }

    def removeVRouterNetworksFromOspfArea(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVRouterNetworksFromOspfAreaAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveVRouterNetworksFromOspfAreaAction, c)
    }

    def removeVmFromAffinityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmFromAffinityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveVmFromAffinityGroupAction, c)
    }

    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveVmNicFromLoadBalancerAction, c)
    }

    def removeVmSchedulingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmSchedulingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.RemoveVmSchedulingRuleAction, c)
    }

    def renewSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RenewSessionAction.class) Closure c) {
        runAction(org.zstack.sdk.RenewSessionAction, c)
    }

    def requestConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        runAction(org.zstack.sdk.RequestConsoleAccessAction, c)
    }

    def rerunLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RerunLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.RerunLongJobAction, c)
    }

    def resetGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResetGlobalConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.ResetGlobalConfigAction, c)
    }

    def resetTemplateConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResetTemplateConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.ResetTemplateConfigAction, c)
    }

    def resetTwoFactorAuthenticationSecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResetTwoFactorAuthenticationSecretAction.class) Closure c) {
        runAction(org.zstack.sdk.ResetTwoFactorAuthenticationSecretAction, c)
    }

    def resizeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResizeDataVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.ResizeDataVolumeAction, c)
    }

    def resizeRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResizeRootVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.ResizeRootVolumeAction, c)
    }

    def restartResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RestartResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.RestartResourceStackAction, c)
    }

    def resumeLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.ResumeLongJobAction, c)
    }

    def resumeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.ResumeVmInstanceAction, c)
    }

    def revertTemplateConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertTemplateConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.RevertTemplateConfigAction, c)
    }

    def revertVmFromSnapshotGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVmFromSnapshotGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.RevertVmFromSnapshotGroupAction, c)
    }

    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.RevertVolumeFromSnapshotAction, c)
    }

    def revokeResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        runAction(org.zstack.sdk.RevokeResourceSharingAction, c)
    }

    def runSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RunSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.RunSchedulerTriggerAction, c)
    }

    def scanVmInstanceMetadataFromPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ScanVmInstanceMetadataFromPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.ScanVmInstanceMetadataFromPrimaryStorageAction, c)
    }

    def selfTestLocalRaid(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SelfTestLocalRaidAction.class) Closure c) {
        runAction(org.zstack.sdk.SelfTestLocalRaidAction, c)
    }

    def setImageBootMode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageBootModeAction.class) Closure c) {
        runAction(org.zstack.sdk.SetImageBootModeAction, c)
    }

    def setImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageQgaAction.class) Closure c) {
        runAction(org.zstack.sdk.SetImageQgaAction, c)
    }

    def setImageSecurityLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageSecurityLevelAction.class) Closure c) {
        runAction(org.zstack.sdk.SetImageSecurityLevelAction, c)
    }

    def setImageStoreBackupStorageQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageStoreBackupStorageQuotaAction.class) Closure c) {
        runAction(org.zstack.sdk.SetImageStoreBackupStorageQuotaAction, c)
    }

    def setIpOnHostNetworkBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetIpOnHostNetworkBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.SetIpOnHostNetworkBondingAction, c)
    }

    def setIpOnHostNetworkInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetIpOnHostNetworkInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.SetIpOnHostNetworkInterfaceAction, c)
    }

    def setL3NetworkMtu(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetL3NetworkMtuAction.class) Closure c) {
        runAction(org.zstack.sdk.SetL3NetworkMtuAction, c)
    }

    def setL3NetworkRouterInterfaceIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetL3NetworkRouterInterfaceIpAction.class) Closure c) {
        runAction(org.zstack.sdk.SetL3NetworkRouterInterfaceIpAction, c)
    }

    def setNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetNicQosAction.class) Closure c) {
        runAction(org.zstack.sdk.SetNicQosAction, c)
    }

    def setServiceTypeOnHostNetworkBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetServiceTypeOnHostNetworkBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.SetServiceTypeOnHostNetworkBondingAction, c)
    }

    def setServiceTypeOnHostNetworkInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetServiceTypeOnHostNetworkInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.SetServiceTypeOnHostNetworkInterfaceAction, c)
    }

    def setVRouterRouterId(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVRouterRouterIdAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVRouterRouterIdAction, c)
    }

    def setVipQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVipQosAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVipQosAction, c)
    }

    def setVmBootMode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootModeAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmBootModeAction, c)
    }

    def setVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmBootOrderAction, c)
    }

    def setVmBootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmBootVolumeAction, c)
    }

    def setVmCleanTraffic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmCleanTrafficAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmCleanTrafficAction, c)
    }

    def setVmClockTrack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmClockTrackAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmClockTrackAction, c)
    }

    def setVmConsoleMode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsoleModeAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmConsoleModeAction, c)
    }

    def setVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmConsolePasswordAction, c)
    }

    def setVmDns(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmDnsAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmDnsAction, c)
    }

    def setVmEmulatorPinning(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmEmulatorPinningAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmEmulatorPinningAction, c)
    }

    def setVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmHostnameAction, c)
    }

    def setVmInstanceDefaultCdRom(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceDefaultCdRomAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmInstanceDefaultCdRomAction, c)
    }

    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmInstanceHaLevelAction, c)
    }

    def setVmMonitorNumber(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmMonitorNumberAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmMonitorNumberAction, c)
    }

    def setVmNicSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmNicSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmNicSecurityGroupAction, c)
    }

    def setVmNuma(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmNumaAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmNumaAction, c)
    }

    def setVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQgaAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmQgaAction, c)
    }

    def setVmQxlMemory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQxlMemoryAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmQxlMemoryAction, c)
    }

    def setVmRDP(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmRDPAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmRDPAction, c)
    }

    def setVmSecurityLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSecurityLevelAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmSecurityLevelAction, c)
    }

    def setVmSoundType(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSoundTypeAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmSoundTypeAction, c)
    }

    def setVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmSshKeyAction, c)
    }

    def setVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmStaticIpAction, c)
    }

    def setVmUsbRedirect(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmUsbRedirectAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmUsbRedirectAction, c)
    }

    def setVmUserDefinedXml(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmUserDefinedXmlAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmUserDefinedXmlAction, c)
    }

    def setVmUserDefinedXmlHookScript(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmUserDefinedXmlHookScriptAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVmUserDefinedXmlHookScriptAction, c)
    }

    def setVolumeIoThreadPin(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeIoThreadPinAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVolumeIoThreadPinAction, c)
    }

    def setVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeQosAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVolumeQosAction, c)
    }

    def setVpcVRouterDistributedRoutingEnabled(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVpcVRouterDistributedRoutingEnabledAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVpcVRouterDistributedRoutingEnabledAction, c)
    }

    def setVpcVRouterNetworkServiceState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVpcVRouterNetworkServiceStateAction.class) Closure c) {
        runAction(org.zstack.sdk.SetVpcVRouterNetworkServiceStateAction, c)
    }

    def shareResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        runAction(org.zstack.sdk.ShareResourceAction, c)
    }

    def shrinkVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShrinkVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.ShrinkVolumeSnapshotAction, c)
    }

    def shutdownHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShutdownHostAction.class) Closure c) {
        runAction(org.zstack.sdk.ShutdownHostAction, c)
    }

    def startBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.StartBaremetalInstanceAction, c)
    }

    def startBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartBaremetalPxeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.StartBaremetalPxeServerAction, c)
    }

    def startSnmpAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartSnmpAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.StartSnmpAgentAction, c)
    }

    def startVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.StartVmInstanceAction, c)
    }

    def stopBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.StopBaremetalInstanceAction, c)
    }

    def stopBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopBaremetalPxeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.StopBaremetalPxeServerAction, c)
    }

    def stopSnmpAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopSnmpAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.StopSnmpAgentAction, c)
    }

    def stopVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.StopVmInstanceAction, c)
    }

    def submitLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SubmitLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.SubmitLongJobAction, c)
    }

    def syncChronyServers(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncChronyServersAction.class) Closure c) {
        runAction(org.zstack.sdk.SyncChronyServersAction, c)
    }

    def syncImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageAction.class) Closure c) {
        runAction(org.zstack.sdk.SyncImageAction, c)
    }

    def syncImageFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.SyncImageFromImageStoreBackupStorageAction, c)
    }

    def syncImageSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        runAction(org.zstack.sdk.SyncImageSizeAction, c)
    }

    def syncPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncPrimaryStorageCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.SyncPrimaryStorageCapacityAction, c)
    }

    def syncVmClock(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVmClockAction.class) Closure c) {
        runAction(org.zstack.sdk.SyncVmClockAction, c)
    }

    def syncVolumeSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVolumeSizeAction.class) Closure c) {
        runAction(org.zstack.sdk.SyncVolumeSizeAction, c)
    }

    def takeVmConsoleScreenshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TakeVmConsoleScreenshotAction.class) Closure c) {
        runAction(org.zstack.sdk.TakeVmConsoleScreenshotAction, c)
    }

    def takeoverPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TakeoverPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.TakeoverPrimaryStorageAction, c)
    }

    def triggerGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TriggerGCJobAction.class) Closure c) {
        runAction(org.zstack.sdk.TriggerGCJobAction, c)
    }

    def undoSnapshotCreation(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UndoSnapshotCreationAction.class) Closure c) {
        runAction(org.zstack.sdk.UndoSnapshotCreationAction, c)
    }

    def unexportNbdVolumes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UnexportNbdVolumesAction.class) Closure c) {
        runAction(org.zstack.sdk.UnexportNbdVolumesAction, c)
    }

    def ungenerateMdevDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UngenerateMdevDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.UngenerateMdevDevicesAction, c)
    }

    def ungenerateSeMdevDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UngenerateSeMdevDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.UngenerateSeMdevDevicesAction, c)
    }

    def ungenerateSriovPciDevices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UngenerateSriovPciDevicesAction.class) Closure c) {
        runAction(org.zstack.sdk.UngenerateSriovPciDevicesAction, c)
    }

    def ungroupVolumeSnapshotGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UngroupVolumeSnapshotGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UngroupVolumeSnapshotGroupAction, c)
    }

    def unlockIdentity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UnlockIdentityAction.class) Closure c) {
        runAction(org.zstack.sdk.UnlockIdentityAction, c)
    }

    def updateAccessControlRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccessControlRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAccessControlRuleAction, c)
    }

    def updateAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAccountAction, c)
    }

    def updateAffinityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAffinityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAffinityGroupAction, c)
    }

    def updateAutoScalingGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAutoScalingGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAutoScalingGroupAction, c)
    }

    def updateAutoScalingGroupAddingNewInstanceRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAutoScalingGroupAddingNewInstanceRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAutoScalingGroupAddingNewInstanceRuleAction, c)
    }

    def updateAutoScalingGroupInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAutoScalingGroupInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAutoScalingGroupInstanceAction, c)
    }

    def updateAutoScalingGroupRemovalInstanceRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAutoScalingGroupRemovalInstanceRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAutoScalingGroupRemovalInstanceRuleAction, c)
    }

    def updateAutoScalingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAutoScalingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAutoScalingRuleAction, c)
    }

    def updateAutoScalingVmTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAutoScalingVmTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateAutoScalingVmTemplateAction, c)
    }

    def updateBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateBackupStorageAction, c)
    }

    def updateBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalChassisAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateBaremetalChassisAction, c)
    }

    def updateBaremetalInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateBaremetalInstanceAction, c)
    }

    def updateBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalPxeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateBaremetalPxeServerAction, c)
    }

    def updateBlockPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBlockPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateBlockPrimaryStorageAction, c)
    }

    def updateBlockVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBlockVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateBlockVolumeAction, c)
    }

    def updateBonding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBondingAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateBondingAction, c)
    }

    def updateCasClient(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCasClientAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateCasClientAction, c)
    }

    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateCephBackupStorageMonAction, c)
    }

    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateCephPrimaryStorageMonAction, c)
    }

    def updateCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStoragePoolAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateCephPrimaryStoragePoolAction, c)
    }

    def updateCertificate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCertificateAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateCertificateAction, c)
    }

    def updateChronyServers(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateChronyServersAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateChronyServersAction, c)
    }

    def updateCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateClusterAction, c)
    }

    def updateClusterDRS(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterDRSAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateClusterDRSAction, c)
    }

    def updateClusterOS(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterOSAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateClusterOSAction, c)
    }

    def updateConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateConsoleProxyAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateConsoleProxyAgentAction, c)
    }

    def updateDirectory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDirectoryAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateDirectoryAction, c)
    }

    def updateDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateDiskOfferingAction, c)
    }

    def updateEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateEipAction, c)
    }

    def updateEmailMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEmailMediaAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateEmailMediaAction, c)
    }

    def updateEmailMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEmailMonitorTriggerActionAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateEmailMonitorTriggerActionAction, c)
    }

    def updateExternalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateExternalPrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateExternalPrimaryStorageAction, c)
    }

    def updateFactoryModeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFactoryModeStateAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateFactoryModeStateAction, c)
    }

    def updateGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateGlobalConfigAction, c)
    }

    def updateHaStrategyCondition(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHaStrategyConditionAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHaStrategyConditionAction, c)
    }

    def updateHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostAction, c)
    }

    def updateHostIommuState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostIommuStateAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostIommuStateAction, c)
    }

    def updateHostIpmi(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostIpmiAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostIpmiAction, c)
    }

    def updateHostIscsiInitiatorName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostIscsiInitiatorNameAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostIscsiInitiatorNameAction, c)
    }

    def updateHostKernelInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostKernelInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostKernelInterfaceAction, c)
    }

    def updateHostNetworkInterface(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostNetworkInterfaceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostNetworkInterfaceAction, c)
    }

    def updateHostNqn(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostNqnAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostNqnAction, c)
    }

    def updateHostSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostSchedulingRuleGroupAction, c)
    }

    def updateHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostnameAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateHostnameAction, c)
    }

    def updateIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIPsecConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateIPsecConnectionAction, c)
    }

    def updateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateImageAction, c)
    }

    def updateImagePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImagePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateImagePackageAction, c)
    }

    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateImageStoreBackupStorageAction, c)
    }

    def updateInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateInstanceOfferingAction, c)
    }

    def updateIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateIpRangeAction, c)
    }

    def updateIscsiServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIscsiServerAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateIscsiServerAction, c)
    }

    def updateKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateKVMHostAction, c)
    }

    def updateL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateL2NetworkAction, c)
    }

    def updateL2NetworkVirtualNetworkId(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkVirtualNetworkIdAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateL2NetworkVirtualNetworkIdAction, c)
    }

    def updateL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateL3NetworkAction, c)
    }

    def updateLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLoadBalancerAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateLoadBalancerAction, c)
    }

    def updateLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLoadBalancerListenerAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateLoadBalancerListenerAction, c)
    }

    def updateLoadBalancerServerGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLoadBalancerServerGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateLoadBalancerServerGroupAction, c)
    }

    def updateLogConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLogConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateLogConfigurationAction, c)
    }

    def updateLongJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLongJobAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateLongJobAction, c)
    }

    def updateMdevDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateMdevDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateMdevDeviceAction, c)
    }

    def updateMdevDeviceSpec(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateMdevDeviceSpecAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateMdevDeviceSpecAction, c)
    }

    def updateMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateMonitorTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateMonitorTriggerAction, c)
    }

    def updateNasFileSystem(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNasFileSystemAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateNasFileSystemAction, c)
    }

    def updateNasMountTarget(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNasMountTargetAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateNasMountTargetAction, c)
    }

    def updateNvmeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNvmeServerAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateNvmeServerAction, c)
    }

    def updateOAuthClient(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateOAuthClientAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateOAuthClientAction, c)
    }

    def updatePciDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePciDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePciDeviceAction, c)
    }

    def updatePciDeviceSpec(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePciDeviceSpecAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePciDeviceSpecAction, c)
    }

    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePortForwardingRuleAction, c)
    }

    def updatePortGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePortGroupAction, c)
    }

    def updatePreconfigurationTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePreconfigurationTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePreconfigurationTemplateAction, c)
    }

    def updatePriceTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePriceTableAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePriceTableAction, c)
    }

    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePrimaryStorageAction, c)
    }

    def updatePriorityConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePriorityConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdatePriorityConfigAction, c)
    }

    def updateQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateQuotaAction, c)
    }

    def updateResourceConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateResourceConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateResourceConfigAction, c)
    }

    def updateResourceConfigs(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateResourceConfigsAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateResourceConfigsAction, c)
    }

    def updateResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateResourcePriceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateResourcePriceAction, c)
    }

    def updateResourceStack(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateResourceStackAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateResourceStackAction, c)
    }

    def updateSSORedirectTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSSORedirectTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSSORedirectTemplateAction, c)
    }

    def updateSchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerJobAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSchedulerJobAction, c)
    }

    def updateSchedulerJobGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerJobGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSchedulerJobGroupAction, c)
    }

    def updateSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerTriggerAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSchedulerTriggerAction, c)
    }

    def updateScsiLun(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateScsiLunAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateScsiLunAction, c)
    }

    def updateSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSecurityGroupAction, c)
    }

    def updateSecurityGroupRulePriority(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupRulePriorityAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSecurityGroupRulePriorityAction, c)
    }

    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSftpBackupStorageAction, c)
    }

    def updateSharedBlock(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSharedBlockAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSharedBlockAction, c)
    }

    def updateSnmpAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSnmpAgentAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSnmpAgentAction, c)
    }

    def updateSshKeyPair(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSshKeyPairAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSshKeyPairAction, c)
    }

    def updateStackTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateStackTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateStackTemplateAction, c)
    }

    def updateSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateSystemTagAction, c)
    }

    def updateTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateTagAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateTagAction, c)
    }

    def updateTemplateConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateTemplateConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateTemplateConfigAction, c)
    }

    def updateTemplatedVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateTemplatedVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateTemplatedVmInstanceAction, c)
    }

    def updateUsbDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUsbDeviceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateUsbDeviceAction, c)
    }

    def updateVRouterOspfArea(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVRouterOspfAreaAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVRouterOspfAreaAction, c)
    }

    def updateVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVRouterRouteTableAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVRouterRouteTableAction, c)
    }

    def updateVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVipAction, c)
    }

    def updateVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVirtualRouterAction, c)
    }

    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVirtualRouterOfferingAction, c)
    }

    def updateVirtualRouterSoftwareVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterSoftwareVersionAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVirtualRouterSoftwareVersionAction, c)
    }

    def updateVirtualSwitchUplinkBondings(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualSwitchUplinkBondingsAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVirtualSwitchUplinkBondingsAction, c)
    }

    def updateVirtualSwitchUplinkGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualSwitchUplinkGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVirtualSwitchUplinkGroupAction, c)
    }

    def updateVmCdRom(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmCdRomAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmCdRomAction, c)
    }

    def updateVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmInstanceAction, c)
    }

    def updateVmInstanceMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmInstanceMetadataAction, c)
    }

    def updateVmNicDriver(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmNicDriverAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmNicDriverAction, c)
    }

    def updateVmNicMac(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmNicMacAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmNicMacAction, c)
    }

    def updateVmPriority(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmPriorityAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmPriorityAction, c)
    }

    def updateVmSchedulingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmSchedulingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmSchedulingRuleAction, c)
    }

    def updateVmSchedulingRuleGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmSchedulingRuleGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVmSchedulingRuleGroupAction, c)
    }

    def updateVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVniRangeAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVniRangeAction, c)
    }

    def updateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVolumeAction, c)
    }

    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVolumeSnapshotAction, c)
    }

    def updateVolumeSnapshotGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVolumeSnapshotGroupAction, c)
    }

    def updateVpcHaGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVpcHaGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateVpcHaGroupAction, c)
    }

    def updateWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateWebhookAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateWebhookAction, c)
    }

    def updateXskyBlockVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateXskyBlockVolumeAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateXskyBlockVolumeAction, c)
    }

    def updateZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        runAction(org.zstack.sdk.UpdateZoneAction, c)
    }

    def validateClusterSupportDRS(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateClusterSupportDRSAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidateClusterSupportDRSAction, c)
    }

    def validateDiskOfferingUserConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateDiskOfferingUserConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidateDiskOfferingUserConfigAction, c)
    }

    def validateInstanceOfferingUserConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateInstanceOfferingUserConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidateInstanceOfferingUserConfigAction, c)
    }

    def validatePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidatePasswordAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidatePasswordAction, c)
    }

    def validatePriceUserConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidatePriceUserConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidatePriceUserConfigAction, c)
    }

    def validateSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSecurityGroupRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidateSecurityGroupRuleAction, c)
    }

    def validateSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidateSessionAction, c)
    }

    def validateVmSchedulingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateVmSchedulingRuleAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidateVmSchedulingRuleAction, c)
    }

    def validateVolumeSnapshotChain(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateVolumeSnapshotChainAction.class) Closure c) {
        runAction(org.zstack.sdk.ValidateVolumeSnapshotChainAction, c)
    }

    def zQLQuery(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ZQLQueryAction.class) Closure c) {
        runAction(org.zstack.sdk.ZQLQueryAction, c)
    }

    def createAllocatorDomain(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.allocator.domain.api.CreateAllocatorDomainAction.class) Closure c) {
        runAction(org.zstack.sdk.allocator.domain.api.CreateAllocatorDomainAction, c)
    }

    def deleteAllocatorDomain(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.allocator.domain.api.DeleteAllocatorDomainAction.class) Closure c) {
        runAction(org.zstack.sdk.allocator.domain.api.DeleteAllocatorDomainAction, c)
    }

    def getAllocatorDomainCandidates(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.allocator.domain.api.GetAllocatorDomainCandidatesAction.class) Closure c) {
        runAction(org.zstack.sdk.allocator.domain.api.GetAllocatorDomainCandidatesAction, c)
    }

    def getAllocatorDomainVms(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.allocator.domain.api.GetAllocatorDomainVmsAction.class) Closure c) {
        runAction(org.zstack.sdk.allocator.domain.api.GetAllocatorDomainVmsAction, c)
    }

    def queryAllocatorDomain(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.allocator.domain.api.QueryAllocatorDomainAction.class) Closure c) {
        runAction(org.zstack.sdk.allocator.domain.api.QueryAllocatorDomainAction, c)
    }

    def updateAllocatorDomain(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.allocator.domain.api.UpdateAllocatorDomainAction.class) Closure c) {
        runAction(org.zstack.sdk.allocator.domain.api.UpdateAllocatorDomainAction, c)
    }

    def createResourceAttributeKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.attribute.api.CreateResourceAttributeKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.attribute.api.CreateResourceAttributeKeyAction, c)
    }

    def createResourceAttributeValue(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.attribute.api.CreateResourceAttributeValueAction.class) Closure c) {
        runAction(org.zstack.sdk.attribute.api.CreateResourceAttributeValueAction, c)
    }

    def deleteResourceAttributeKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.attribute.api.DeleteResourceAttributeKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.attribute.api.DeleteResourceAttributeKeyAction, c)
    }

    def deleteResourceAttributeValue(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.attribute.api.DeleteResourceAttributeValueAction.class) Closure c) {
        runAction(org.zstack.sdk.attribute.api.DeleteResourceAttributeValueAction, c)
    }

    def queryResourceAttributeKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.attribute.api.QueryResourceAttributeKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.attribute.api.QueryResourceAttributeKeyAction, c)
    }

    def queryResourceAttributeValue(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.attribute.api.QueryResourceAttributeValueAction.class) Closure c) {
        runAction(org.zstack.sdk.attribute.api.QueryResourceAttributeValueAction, c)
    }

    def updateResourceAttributeKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.attribute.api.UpdateResourceAttributeKeyAction.class) Closure c) {
        runAction(org.zstack.sdk.attribute.api.UpdateResourceAttributeKeyAction, c)
    }

    def createDatabaseBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.CreateDatabaseBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.CreateDatabaseBackupAction, c)
    }

    def deleteDatabaseBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.DeleteDatabaseBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.DeleteDatabaseBackupAction, c)
    }

    def deleteExportedDatabaseBackupFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.DeleteExportedDatabaseBackupFromBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.DeleteExportedDatabaseBackupFromBackupStorageAction, c)
    }

    def exportDatabaseBackupFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.ExportDatabaseBackupFromBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.ExportDatabaseBackupFromBackupStorageAction, c)
    }

    def getDatabaseBackupFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.GetDatabaseBackupFromImageStoreAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.GetDatabaseBackupFromImageStoreAction, c)
    }

    def queryDatabaseBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.QueryDatabaseBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.QueryDatabaseBackupAction, c)
    }

    def recoverDatabaseFromBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.RecoverDatabaseFromBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.RecoverDatabaseFromBackupAction, c)
    }

    def syncDatabaseBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.SyncDatabaseBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.SyncDatabaseBackupAction, c)
    }

    def syncDatabaseBackupFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.databasebackup.SyncDatabaseBackupFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.databasebackup.SyncDatabaseBackupFromImageStoreBackupStorageAction, c)
    }

    def addExternalServiceConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.external.service.AddExternalServiceConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.external.service.AddExternalServiceConfigurationAction, c)
    }

    def deleteExternalServiceConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.external.service.DeleteExternalServiceConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.external.service.DeleteExternalServiceConfigurationAction, c)
    }

    def getExternalServices(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.external.service.GetExternalServicesAction.class) Closure c) {
        runAction(org.zstack.sdk.external.service.GetExternalServicesAction, c)
    }

    def queryExternalServiceConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.external.service.QueryExternalServiceConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.external.service.QueryExternalServiceConfigurationAction, c)
    }

    def reloadExternalService(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.external.service.ReloadExternalServiceAction.class) Closure c) {
        runAction(org.zstack.sdk.external.service.ReloadExternalServiceAction, c)
    }

    def updateExternalServiceConfiguration(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.external.service.UpdateExternalServiceConfigurationAction.class) Closure c) {
        runAction(org.zstack.sdk.external.service.UpdateExternalServiceConfigurationAction, c)
    }

    def attachGuestToolsIsoToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.AttachGuestToolsIsoToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.AttachGuestToolsIsoToVmAction, c)
    }

    def detachGuestToolsIsoFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.DetachGuestToolsIsoFromVmAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.DetachGuestToolsIsoFromVmAction, c)
    }

    def getLatestGuestToolsForVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.GetLatestGuestToolsForVmAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.GetLatestGuestToolsForVmAction, c)
    }

    def getVmGuestToolsInfo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.GetVmGuestToolsInfoAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.GetVmGuestToolsInfoAction, c)
    }

    def queryGuestToolsState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.QueryGuestToolsStateAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.QueryGuestToolsStateAction, c)
    }

    def updateGuestToolsState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.UpdateGuestToolsStateAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.UpdateGuestToolsStateAction, c)
    }

    def updateVmNetworkConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.UpdateVmNetworkConfigAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.UpdateVmNetworkConfigAction, c)
    }

    def createVmCustomSpecification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.advanced.CreateVmCustomSpecificationAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.advanced.CreateVmCustomSpecificationAction, c)
    }

    def deleteVmCustomSpecification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.advanced.DeleteVmCustomSpecificationAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.advanced.DeleteVmCustomSpecificationAction, c)
    }

    def queryVmCustomSpecification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.advanced.QueryVmCustomSpecificationAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.advanced.QueryVmCustomSpecificationAction, c)
    }

    def updateVmCustomSpecification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.guesttools.advanced.UpdateVmCustomSpecificationAction.class) Closure c) {
        runAction(org.zstack.sdk.guesttools.advanced.UpdateVmCustomSpecificationAction, c)
    }

    def addAccountToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.AddAccountToGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.AddAccountToGroupAction, c)
    }

    def attachRoleToAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.AttachRoleToAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.AttachRoleToAccountGroupAction, c)
    }

    def createAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.CreateAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.CreateAccountGroupAction, c)
    }

    def deleteAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.DeleteAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.DeleteAccountGroupAction, c)
    }

    def detachRoleFromAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.DetachRoleFromAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.DetachRoleFromAccountGroupAction, c)
    }

    def getAccountGroupTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.GetAccountGroupTreeAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.GetAccountGroupTreeAction, c)
    }

    def getResourceInAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.GetResourceInAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.GetResourceInAccountGroupAction, c)
    }

    def getRolesForAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.GetRolesForAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.GetRolesForAccountGroupAction, c)
    }

    def moveAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.MoveAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.MoveAccountGroupAction, c)
    }

    def queryAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.QueryAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.QueryAccountGroupAction, c)
    }

    def removeAccountFromGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.RemoveAccountFromGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.RemoveAccountFromGroupAction, c)
    }

    def revokeResourceSharingToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.RevokeResourceSharingToGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.RevokeResourceSharingToGroupAction, c)
    }

    def shareResourceToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.ShareResourceToGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.ShareResourceToGroupAction, c)
    }

    def updateAccountGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.accounts.UpdateAccountGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.accounts.UpdateAccountGroupAction, c)
    }

    def getResourceEnsembleMembers(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.ensemble.GetResourceEnsembleMembersAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.ensemble.GetResourceEnsembleMembersAction, c)
    }

    def getResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.iam1.ensemble.GetResourceSharingAction.class) Closure c) {
        runAction(org.zstack.sdk.iam1.ensemble.GetResourceSharingAction, c)
    }

    def queryThirdPartyAccountSourceBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.imports.api.QueryThirdPartyAccountSourceBindingAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.imports.api.QueryThirdPartyAccountSourceBindingAction, c)
    }

    def addLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.AddLdapServerAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.AddLdapServerAction, c)
    }

    def createLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.CreateLdapBindingAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.CreateLdapBindingAction, c)
    }

    def deleteLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.DeleteLdapBindingAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.DeleteLdapBindingAction, c)
    }

    def deleteLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.DeleteLdapServerAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.DeleteLdapServerAction, c)
    }

    def getCandidateLdapEntryForBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.GetCandidateLdapEntryForBindingAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.GetCandidateLdapEntryForBindingAction, c)
    }

    def getLdapEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.GetLdapEntryAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.GetLdapEntryAction, c)
    }

    def queryLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.QueryLdapServerAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.QueryLdapServerAction, c)
    }

    def syncAccountsFromLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.SyncAccountsFromLdapServerAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.SyncAccountsFromLdapServerAction, c)
    }

    def updateLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.ldap.api.UpdateLdapServerAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.ldap.api.UpdateLdapServerAction, c)
    }

    def attachRoleToAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.AttachRoleToAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.AttachRoleToAccountAction, c)
    }

    def createRole(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.CreateRoleAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.CreateRoleAction, c)
    }

    def deleteRole(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.DeleteRoleAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.DeleteRoleAction, c)
    }

    def detachRoleFromAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.DetachRoleFromAccountAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.DetachRoleFromAccountAction, c)
    }

    def getRolePolicysAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.GetRolePolicyActionsAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.GetRolePolicyActionsAction, c)
    }

    def queryRoleAccountRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.QueryRoleAccountRefAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.QueryRoleAccountRefAction, c)
    }

    def queryRole(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.QueryRoleAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.QueryRoleAction, c)
    }

    def updateRole(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.identity.role.api.UpdateRoleAction.class) Closure c) {
        runAction(org.zstack.sdk.identity.role.api.UpdateRoleAction, c)
    }

    def queryKeyProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.api.QueryKeyProviderAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.api.QueryKeyProviderAction, c)
    }

    def rekeyKeyProviderRefs(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.api.RekeyKeyProviderRefsAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.api.RekeyKeyProviderRefsAction, c)
    }

    def createKms(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.CreateKmsAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.CreateKmsAction, c)
    }

    def deleteKms(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.DeleteKmsAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.DeleteKmsAction, c)
    }

    def getKmsServerCertFromKms(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.GetKmsServerCertFromKmsAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.GetKmsServerCertFromKmsAction, c)
    }

    def queryKms(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.QueryKmsAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.QueryKmsAction, c)
    }

    def updateKms(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.UpdateKmsAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.UpdateKmsAction, c)
    }

    def uploadKmsClientCsr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.UploadKmsClientCsrAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.UploadKmsClientCsrAction, c)
    }

    def uploadKmsClientIdentity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.UploadKmsClientIdentityAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.UploadKmsClientIdentityAction, c)
    }

    def uploadKmsClientSignedCert(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.UploadKmsClientSignedCertAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.UploadKmsClientSignedCertAction, c)
    }

    def uploadKmsServerCert(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.kms.api.UploadKmsServerCertAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.kms.api.UploadKmsServerCertAction, c)
    }

    def backupNkp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.nkp.api.BackupNkpAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.nkp.api.BackupNkpAction, c)
    }

    def createNkp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.nkp.api.CreateNkpAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.nkp.api.CreateNkpAction, c)
    }

    def deleteNkp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.nkp.api.DeleteNkpAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.nkp.api.DeleteNkpAction, c)
    }

    def parseNkpRestore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.nkp.api.ParseNkpRestoreAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.nkp.api.ParseNkpRestoreAction, c)
    }

    def queryNkp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.nkp.api.QueryNkpAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.nkp.api.QueryNkpAction, c)
    }

    def restoreNkp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.nkp.api.RestoreNkpAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.nkp.api.RestoreNkpAction, c)
    }

    def updateNkp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.keyprovider.nkp.api.UpdateNkpAction.class) Closure c) {
        runAction(org.zstack.sdk.keyprovider.nkp.api.UpdateNkpAction, c)
    }

    def addLogServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.logserver.AddLogServerAction.class) Closure c) {
        runAction(org.zstack.sdk.logserver.AddLogServerAction, c)
    }

    def deleteLogServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.logserver.DeleteLogServerAction.class) Closure c) {
        runAction(org.zstack.sdk.logserver.DeleteLogServerAction, c)
    }

    def queryLogServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.logserver.QueryLogServerAction.class) Closure c) {
        runAction(org.zstack.sdk.logserver.QueryLogServerAction, c)
    }

    def updateLogServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.logserver.UpdateLogServerAction.class) Closure c) {
        runAction(org.zstack.sdk.logserver.UpdateLogServerAction, c)
    }

    def getManagementNodesStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.managements.common.GetManagementNodesStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.managements.common.GetManagementNodesStatusAction, c)
    }

    def getZSha2Status(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.managements.ha2.GetZSha2StatusAction.class) Closure c) {
        runAction(org.zstack.sdk.managements.ha2.GetZSha2StatusAction, c)
    }

    def zSha2Demote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.managements.ha2.ZSha2DemoteAction.class) Closure c) {
        runAction(org.zstack.sdk.managements.ha2.ZSha2DemoteAction, c)
    }

    def addSNSSmsReceiver(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.AddSNSSmsReceiverAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.AddSNSSmsReceiverAction, c)
    }

    def changeSNSApplicationEndpointState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.ChangeSNSApplicationEndpointStateAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.ChangeSNSApplicationEndpointStateAction, c)
    }

    def changeSNSApplicationPlatformState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.ChangeSNSApplicationPlatformStateAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.ChangeSNSApplicationPlatformStateAction, c)
    }

    def changeSNSTopicState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.ChangeSNSTopicStateAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.ChangeSNSTopicStateAction, c)
    }

    def createSNSTopic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.CreateSNSTopicAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.CreateSNSTopicAction, c)
    }

    def deleteSNSApplicationEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.DeleteSNSApplicationEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.DeleteSNSApplicationEndpointAction, c)
    }

    def deleteSNSApplicationPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.DeleteSNSApplicationPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.DeleteSNSApplicationPlatformAction, c)
    }

    def deleteSNSTopic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.DeleteSNSTopicAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.DeleteSNSTopicAction, c)
    }

    def querySNSApplicationEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.QuerySNSApplicationEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.QuerySNSApplicationEndpointAction, c)
    }

    def querySNSApplicationPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.QuerySNSApplicationPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.QuerySNSApplicationPlatformAction, c)
    }

    def querySNSSmsEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.QuerySNSSmsEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.QuerySNSSmsEndpointAction, c)
    }

    def querySNSTopic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.QuerySNSTopicAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.QuerySNSTopicAction, c)
    }

    def querySNSTopicSubscriber(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.QuerySNSTopicSubscriberAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.QuerySNSTopicSubscriberAction, c)
    }

    def removeSNSSmsReceiver(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.RemoveSNSSmsReceiverAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.RemoveSNSSmsReceiverAction, c)
    }

    def subscribeSNSTopic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.SubscribeSNSTopicAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.SubscribeSNSTopicAction, c)
    }

    def unsubscribeSNSTopic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.UnsubscribeSNSTopicAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.UnsubscribeSNSTopicAction, c)
    }

    def updateSNSApplicationEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.UpdateSNSApplicationEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.UpdateSNSApplicationEndpointAction, c)
    }

    def updateSNSApplicationPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.UpdateSNSApplicationPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.UpdateSNSApplicationPlatformAction, c)
    }

    def updateSNSTopic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.UpdateSNSTopicAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.UpdateSNSTopicAction, c)
    }

    def addSNSDingTalkAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.AddSNSDingTalkAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.AddSNSDingTalkAtPersonAction, c)
    }

    def createSNSDingTalkEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.CreateSNSDingTalkEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.CreateSNSDingTalkEndpointAction, c)
    }

    def querySNSDingTalkAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.QuerySNSDingTalkAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.QuerySNSDingTalkAtPersonAction, c)
    }

    def querySNSDingTalkEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.QuerySNSDingTalkEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.QuerySNSDingTalkEndpointAction, c)
    }

    def removeSNSDingTalkAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.RemoveSNSDingTalkAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.RemoveSNSDingTalkAtPersonAction, c)
    }

    def sNSDingTalkTestConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.SNSDingTalkTestConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.SNSDingTalkTestConnectionAction, c)
    }

    def updateAtPersonOfAtDingTalkEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.UpdateAtPersonOfAtDingTalkEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.UpdateAtPersonOfAtDingTalkEndpointAction, c)
    }

    def updateSNSDingTalkEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.dingtalk.UpdateSNSDingTalkEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.dingtalk.UpdateSNSDingTalkEndpointAction, c)
    }

    def addEmailAddressToSNSEmailEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.AddEmailAddressToSNSEmailEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.AddEmailAddressToSNSEmailEndpointAction, c)
    }

    def createSNSEmailEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.CreateSNSEmailEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.CreateSNSEmailEndpointAction, c)
    }

    def createSNSEmailPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.CreateSNSEmailPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.CreateSNSEmailPlatformAction, c)
    }

    def deleteEmailAddressOfSNSEmailEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.DeleteEmailAddressOfSNSEmailEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.DeleteEmailAddressOfSNSEmailEndpointAction, c)
    }

    def querySNSEmailAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.QuerySNSEmailAddressAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.QuerySNSEmailAddressAction, c)
    }

    def querySNSEmailEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.QuerySNSEmailEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.QuerySNSEmailEndpointAction, c)
    }

    def querySNSEmailPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.QuerySNSEmailPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.QuerySNSEmailPlatformAction, c)
    }

    def sNSEmailTestConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.SNSEmailTestConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.SNSEmailTestConnectionAction, c)
    }

    def updateEmailAddressOfSNSEmailEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.UpdateEmailAddressOfSNSEmailEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.UpdateEmailAddressOfSNSEmailEndpointAction, c)
    }

    def validateSNSEmailPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.email.ValidateSNSEmailPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.email.ValidateSNSEmailPlatformAction, c)
    }

    def addSNSFeiShuAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.AddSNSFeiShuAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.AddSNSFeiShuAtPersonAction, c)
    }

    def createSNSFeiShuEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.CreateSNSFeiShuEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.CreateSNSFeiShuEndpointAction, c)
    }

    def querySNSFeiShuAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuAtPersonAction, c)
    }

    def querySNSFeiShuEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.QuerySNSFeiShuEndpointAction, c)
    }

    def removeSNSFeiShuAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.RemoveSNSFeiShuAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.RemoveSNSFeiShuAtPersonAction, c)
    }

    def sNSFeiShuTestConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.SNSFeiShuTestConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.SNSFeiShuTestConnectionAction, c)
    }

    def updateAtPersonOfAtFeiShuEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.UpdateAtPersonOfAtFeiShuEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.UpdateAtPersonOfAtFeiShuEndpointAction, c)
    }

    def updateSNSFeiShuEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.feishu.UpdateSNSFeiShuEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.feishu.UpdateSNSFeiShuEndpointAction, c)
    }

    def createSNSHttpEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.http.CreateSNSHttpEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.http.CreateSNSHttpEndpointAction, c)
    }

    def querySNSHttpEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.http.QuerySNSHttpEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.http.QuerySNSHttpEndpointAction, c)
    }

    def sNSHttpTestConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.http.SNSHttpTestConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.http.SNSHttpTestConnectionAction, c)
    }

    def updateSNSHttpEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.http.UpdateSNSHttpEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.http.UpdateSNSHttpEndpointAction, c)
    }

    def createSNSMicrosoftTeamsEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.microsoftteams.CreateSNSMicrosoftTeamsEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.microsoftteams.CreateSNSMicrosoftTeamsEndpointAction, c)
    }

    def querySNSMicrosoftTeamsEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.microsoftteams.QuerySNSMicrosoftTeamsEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.microsoftteams.QuerySNSMicrosoftTeamsEndpointAction, c)
    }

    def sNSMicrosoftTeamsTestConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.microsoftteams.SNSMicrosoftTeamsTestConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.microsoftteams.SNSMicrosoftTeamsTestConnectionAction, c)
    }

    def updateSNSMicrosoftTeamsEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.microsoftteams.UpdateSNSMicrosoftTeamsEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.microsoftteams.UpdateSNSMicrosoftTeamsEndpointAction, c)
    }

    def createSNSSnmpEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.snmp.CreateSNSSnmpEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.snmp.CreateSNSSnmpEndpointAction, c)
    }

    def createSNSSnmpPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.snmp.CreateSNSSnmpPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.snmp.CreateSNSSnmpPlatformAction, c)
    }

    def querySNSSnmpPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.snmp.QuerySNSSnmpPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.snmp.QuerySNSSnmpPlatformAction, c)
    }

    def sNSSnmpTestConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.snmp.SNSSnmpTestConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.snmp.SNSSnmpTestConnectionAction, c)
    }

    def updateSNSSnmpPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.snmp.UpdateSNSSnmpPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.snmp.UpdateSNSSnmpPlatformAction, c)
    }

    def addSNSWeComAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.AddSNSWeComAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.AddSNSWeComAtPersonAction, c)
    }

    def createSNSWeComEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.CreateSNSWeComEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.CreateSNSWeComEndpointAction, c)
    }

    def querySNSWeComAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.QuerySNSWeComAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.QuerySNSWeComAtPersonAction, c)
    }

    def querySNSWeComEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.QuerySNSWeComEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.QuerySNSWeComEndpointAction, c)
    }

    def removeSNSWeComAtPerson(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.RemoveSNSWeComAtPersonAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.RemoveSNSWeComAtPersonAction, c)
    }

    def sNSWeComTestConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.SNSWeComTestConnectionAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.SNSWeComTestConnectionAction, c)
    }

    def updateAtPersonOfAtWeComEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.UpdateAtPersonOfAtWeComEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.UpdateAtPersonOfAtWeComEndpointAction, c)
    }

    def updateSNSWeComEndpoint(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.sns.platform.wecom.UpdateSNSWeComEndpointAction.class) Closure c) {
        runAction(org.zstack.sdk.sns.platform.wecom.UpdateSNSWeComEndpointAction, c)
    }

    def cleanSoftwarePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.CleanSoftwarePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.CleanSoftwarePackageAction, c)
    }

    def cleanUpgradeSoftwarePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.CleanUpgradeSoftwarePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.CleanUpgradeSoftwarePackageAction, c)
    }

    def getDirectoryUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.GetDirectoryUsageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.GetDirectoryUsageAction, c)
    }

    def getUploadSoftwarePackageJobDetails(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.GetUploadSoftwarePackageJobDetailsAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.GetUploadSoftwarePackageJobDetailsAction, c)
    }

    def installSoftwarePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.InstallSoftwarePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.InstallSoftwarePackageAction, c)
    }

    def querySoftwarePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.QuerySoftwarePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.QuerySoftwarePackageAction, c)
    }

    def uninstallSoftwarePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.UninstallSoftwarePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.UninstallSoftwarePackageAction, c)
    }

    def uploadAndExecuteSoftwareUpgradePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.UploadAndExecuteSoftwareUpgradePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.UploadAndExecuteSoftwareUpgradePackageAction, c)
    }

    def uploadSoftwarePackage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.UploadSoftwarePackageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.UploadSoftwarePackageAction, c)
    }

    def uploadSoftwarePackageToBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.UploadSoftwarePackageToBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.UploadSoftwarePackageToBackupStorageAction, c)
    }

    def uploadSoftwarePackageToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.softwarePackage.header.UploadSoftwarePackageToVmAction.class) Closure c) {
        runAction(org.zstack.sdk.softwarePackage.header.UploadSoftwarePackageToVmAction, c)
    }

    def createDataVolumeFromVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.CreateDataVolumeFromVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.CreateDataVolumeFromVolumeBackupAction, c)
    }

    def createDataVolumeTemplateFromVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.CreateDataVolumeTemplateFromVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.CreateDataVolumeTemplateFromVolumeBackupAction, c)
    }

    def createRootVolumeTemplateFromVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.CreateRootVolumeTemplateFromVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.CreateRootVolumeTemplateFromVolumeBackupAction, c)
    }

    def createVmBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.CreateVmBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.CreateVmBackupAction, c)
    }

    def createVmFromVmBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.CreateVmFromVmBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.CreateVmFromVmBackupAction, c)
    }

    def createVmFromVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.CreateVmFromVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.CreateVmFromVolumeBackupAction, c)
    }

    def createVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.CreateVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.CreateVolumeBackupAction, c)
    }

    def deleteVmBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.DeleteVmBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.DeleteVmBackupAction, c)
    }

    def deleteVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.DeleteVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.DeleteVolumeBackupAction, c)
    }

    def queryVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.QueryVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.QueryVolumeBackupAction, c)
    }

    def recoverBackupFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.RecoverBackupFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.RecoverBackupFromImageStoreBackupStorageAction, c)
    }

    def recoverVmBackupFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.RecoverVmBackupFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.RecoverVmBackupFromImageStoreBackupStorageAction, c)
    }

    def revertVmFromVmBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.RevertVmFromVmBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.RevertVmFromVmBackupAction, c)
    }

    def revertVolumeFromVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.RevertVolumeFromVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.RevertVolumeFromVolumeBackupAction, c)
    }

    def syncBackupFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.SyncBackupFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.SyncBackupFromImageStoreBackupStorageAction, c)
    }

    def syncVmBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.SyncVmBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.SyncVmBackupAction, c)
    }

    def syncVmBackupFromImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.SyncVmBackupFromImageStoreBackupStorageAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.SyncVmBackupFromImageStoreBackupStorageAction, c)
    }

    def syncVolumeBackup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.storage.volumebackup.SyncVolumeBackupAction.class) Closure c) {
        runAction(org.zstack.sdk.storage.volumebackup.SyncVolumeBackupAction, c)
    }

    def addTpm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.tpm.api.AddTpmAction.class) Closure c) {
        runAction(org.zstack.sdk.tpm.api.AddTpmAction, c)
    }

    def getTpmCapability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.tpm.api.GetTpmCapabilityAction.class) Closure c) {
        runAction(org.zstack.sdk.tpm.api.GetTpmCapabilityAction, c)
    }

    def queryTpm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.tpm.api.QueryTpmAction.class) Closure c) {
        runAction(org.zstack.sdk.tpm.api.QueryTpmAction, c)
    }

    def removeTpm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.tpm.api.RemoveTpmAction.class) Closure c) {
        runAction(org.zstack.sdk.tpm.api.RemoveTpmAction, c)
    }

    def updateTpm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.tpm.api.UpdateTpmAction.class) Closure c) {
        runAction(org.zstack.sdk.tpm.api.UpdateTpmAction, c)
    }

    def getZMigrateGatewayVmInstances(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zmigrate.api.GetZMigrateGatewayVmInstancesAction.class) Closure c) {
        runAction(org.zstack.sdk.zmigrate.api.GetZMigrateGatewayVmInstancesAction, c)
    }

    def getZMigrateImages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zmigrate.api.GetZMigrateImagesAction.class) Closure c) {
        runAction(org.zstack.sdk.zmigrate.api.GetZMigrateImagesAction, c)
    }

    def getZMigrateInfos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zmigrate.api.GetZMigrateInfosAction.class) Closure c) {
        runAction(org.zstack.sdk.zmigrate.api.GetZMigrateInfosAction, c)
    }

    def getNodeRoles(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zsv.core.api.GetNodeRolesAction.class) Closure c) {
        runAction(org.zstack.sdk.zsv.core.api.GetNodeRolesAction, c)
    }

    def checkCephPlugin(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zsv.storage.api.CheckCephPluginAction.class) Closure c) {
        runAction(org.zstack.sdk.zsv.storage.api.CheckCephPluginAction, c)
    }

    def checkTelemetryUpdate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zsv.telemetry.api.CheckTelemetryUpdateAction.class) Closure c) {
        runAction(org.zstack.sdk.zsv.telemetry.api.CheckTelemetryUpdateAction, c)
    }

    def getTelemetryConsent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zsv.telemetry.api.GetTelemetryConsentAction.class) Closure c) {
        runAction(org.zstack.sdk.zsv.telemetry.api.GetTelemetryConsentAction, c)
    }

    def getTelemetrySetting(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zsv.telemetry.api.GetTelemetrySettingAction.class) Closure c) {
        runAction(org.zstack.sdk.zsv.telemetry.api.GetTelemetrySettingAction, c)
    }

    def updateTelemetryConsent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zsv.telemetry.api.UpdateTelemetryConsentAction.class) Closure c) {
        runAction(org.zstack.sdk.zsv.telemetry.api.UpdateTelemetryConsentAction, c)
    }

    def ackAlarmData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.AckAlarmDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.AckAlarmDataAction, c)
    }

    def ackEventData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.AckEventDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.AckEventDataAction, c)
    }

    def addToAlarmAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.AddActionToAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.AddActionToAlarmAction, c)
    }

    def addToEventSubscriptionAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.AddActionToEventSubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.AddActionToEventSubscriptionAction, c)
    }

    def addLabelToAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.AddLabelToAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.AddLabelToAlarmAction, c)
    }

    def addLabelToEventSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.AddLabelToEventSubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.AddLabelToEventSubscriptionAction, c)
    }

    def changeAlarmState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.ChangeAlarmStateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.ChangeAlarmStateAction, c)
    }

    def changeEventSubscriptionState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.ChangeEventSubscriptionStateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.ChangeEventSubscriptionStateAction, c)
    }

    def createAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.CreateAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.CreateAlarmAction, c)
    }

    def deleteAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.DeleteAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.DeleteAlarmAction, c)
    }

    def getTextTemplateArg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.GetTextTemplateArgAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.GetTextTemplateArgAction, c)
    }

    def queryAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.QueryAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.QueryAlarmAction, c)
    }

    def queryAlertDataAck(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.QueryAlertDataAckAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.QueryAlertDataAckAction, c)
    }

    def queryEventSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.QueryEventSubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.QueryEventSubscriptionAction, c)
    }

    def removeFromAlarmAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.RemoveActionFromAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.RemoveActionFromAlarmAction, c)
    }

    def removeFromEventSubscriptionAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.RemoveActionFromEventSubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.RemoveActionFromEventSubscriptionAction, c)
    }

    def removeLabelFromAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.RemoveLabelFromAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.RemoveLabelFromAlarmAction, c)
    }

    def removeLabelFromEventSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.RemoveLabelFromEventSubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.RemoveLabelFromEventSubscriptionAction, c)
    }

    def subscribeEvent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.SubscribeEventAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.SubscribeEventAction, c)
    }

    def unsubscribeEvent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.UnsubscribeEventAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.UnsubscribeEventAction, c)
    }

    def updateAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.UpdateAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.UpdateAlarmAction, c)
    }

    def updateAlarmLabel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.UpdateAlarmLabelAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.UpdateAlarmLabelAction, c)
    }

    def updateAlertDataAck(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.UpdateAlertDataAckAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.UpdateAlertDataAckAction, c)
    }

    def updateEventSubscriptionLabel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.UpdateEventSubscriptionLabelAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.UpdateEventSubscriptionLabelAction, c)
    }

    def updateSubscribeEvent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.UpdateSubscribeEventAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.UpdateSubscribeEventAction, c)
    }

    def changeActiveAlarmState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.activealarm.api.ChangeActiveAlarmStateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.activealarm.api.ChangeActiveAlarmStateAction, c)
    }

    def getActiveAlarmStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.activealarm.api.GetActiveAlarmStatusAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.activealarm.api.GetActiveAlarmStatusAction, c)
    }

    def queryActiveAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.activealarm.api.QueryActiveAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.activealarm.api.QueryActiveAlarmAction, c)
    }

    def queryActiveAlarmTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.activealarm.api.QueryActiveAlarmTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.activealarm.api.QueryActiveAlarmTemplateAction, c)
    }

    def updateActiveAlarmTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.activealarm.api.UpdateActiveAlarmTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.activealarm.api.UpdateActiveAlarmTemplateAction, c)
    }

    def createSNSTextTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.sns.CreateSNSTextTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.sns.CreateSNSTextTemplateAction, c)
    }

    def deleteSNSTextTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.sns.DeleteSNSTextTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.sns.DeleteSNSTextTemplateAction, c)
    }

    def querySNSTextTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.sns.QuerySNSTextTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.sns.QuerySNSTextTemplateAction, c)
    }

    def updateSNSTextTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.sns.UpdateSNSTextTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.sns.UpdateSNSTextTemplateAction, c)
    }

    def createAliyunSmsSNSTextTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.sns.template.aliyunsms.CreateAliyunSmsSNSTextTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.sns.template.aliyunsms.CreateAliyunSmsSNSTextTemplateAction, c)
    }

    def queryAliyunSmsSNSTextTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.sns.template.aliyunsms.QueryAliyunSmsSNSTextTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.sns.template.aliyunsms.QueryAliyunSmsSNSTextTemplateAction, c)
    }

    def updateAliyunSmsSNSTextTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.alarm.sns.template.aliyunsms.UpdateAliyunSmsSNSTextTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.alarm.sns.template.aliyunsms.UpdateAliyunSmsSNSTextTemplateAction, c)
    }

    def createMetricDataHttpReceiver(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.CreateMetricDataHttpReceiverAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.CreateMetricDataHttpReceiverAction, c)
    }

    def createMetricTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.CreateMetricTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.CreateMetricTemplateAction, c)
    }

    def deleteMetricData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.DeleteMetricDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.DeleteMetricDataAction, c)
    }

    def deleteMetricDataHttpReceiver(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.DeleteMetricDataHttpReceiverAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.DeleteMetricDataHttpReceiverAction, c)
    }

    def deleteMetricTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.DeleteMetricTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.DeleteMetricTemplateAction, c)
    }

    def getAlarmData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetAlarmDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetAlarmDataAction, c)
    }

    def getAllEventMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetAllEventMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetAllEventMetadataAction, c)
    }

    def getAllMetricMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetAllMetricMetadataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetAllMetricMetadataAction, c)
    }

    def getAuditData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetAuditDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetAuditDataAction, c)
    }

    def getEventData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetEventDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetEventDataAction, c)
    }

    def getManagementNodeDirCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetManagementNodeDirCapacityAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetManagementNodeDirCapacityAction, c)
    }

    def getMetricData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetMetricDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetMetricDataAction, c)
    }

    def getMetricLabelValue(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetMetricLabelValueAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetMetricLabelValueAction, c)
    }

    def getPrometheusMetricLabelValue(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetPrometheusMetricLabelValueAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetPrometheusMetricLabelValueAction, c)
    }

    def getZWatchAlertHistogram(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.GetZWatchAlertHistogramAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.GetZWatchAlertHistogramAction, c)
    }

    def putMetricData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.PutMetricDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.PutMetricDataAction, c)
    }

    def queryAlarmRecord(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.QueryAlarmRecordAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.QueryAlarmRecordAction, c)
    }

    def queryAudit(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.QueryAuditAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.QueryAuditAction, c)
    }

    def queryEventRecord(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.QueryEventRecordAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.QueryEventRecordAction, c)
    }

    def queryMetricDataHttpReceiver(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.QueryMetricDataHttpReceiverAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.QueryMetricDataHttpReceiverAction, c)
    }

    def queryMetricTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.QueryMetricTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.QueryMetricTemplateAction, c)
    }

    def updateAlarmData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.UpdateAlarmDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.UpdateAlarmDataAction, c)
    }

    def updateEventData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.api.UpdateEventDataAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.api.UpdateEventDataAction, c)
    }

    def addEventRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.AddEventRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.AddEventRuleTemplateAction, c)
    }

    def addInstanceToMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.AddInstanceToMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.AddInstanceToMonitorGroupAction, c)
    }

    def addMetricRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.AddMetricRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.AddMetricRuleTemplateAction, c)
    }

    def applyMonitorTemplateToMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.ApplyMonitorTemplateToMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.ApplyMonitorTemplateToMonitorGroupAction, c)
    }

    def cloneMonitorTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.CloneMonitorTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.CloneMonitorTemplateAction, c)
    }

    def createMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.CreateMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.CreateMonitorGroupAction, c)
    }

    def createMonitorTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.CreateMonitorTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.CreateMonitorTemplateAction, c)
    }

    def deleteEventRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.DeleteEventRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.DeleteEventRuleTemplateAction, c)
    }

    def deleteMetricRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.DeleteMetricRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.DeleteMetricRuleTemplateAction, c)
    }

    def deleteMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.DeleteMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.DeleteMonitorGroupAction, c)
    }

    def deleteMonitorTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.DeleteMonitorTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.DeleteMonitorTemplateAction, c)
    }

    def queryEventRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryEventRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryEventRuleTemplateAction, c)
    }

    def queryMetricRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryMetricRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryMetricRuleTemplateAction, c)
    }

    def queryMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupAction, c)
    }

    def queryMonitorGroupAlarm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupAlarmAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupAlarmAction, c)
    }

    def queryMonitorGroupEventSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupEventSubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupEventSubscriptionAction, c)
    }

    def queryMonitorGroupInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupInstanceAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupInstanceAction, c)
    }

    def queryMonitorGroupTemplateRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupTemplateRefAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorGroupTemplateRefAction, c)
    }

    def queryMonitorTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.QueryMonitorTemplateAction, c)
    }

    def removeInstanceFromMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.RemoveInstanceFromMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.RemoveInstanceFromMonitorGroupAction, c)
    }

    def revokeMonitorTemplateFromMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.RevokeMonitorTemplateFromMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.RevokeMonitorTemplateFromMonitorGroupAction, c)
    }

    def updateEventRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.UpdateEventRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.UpdateEventRuleTemplateAction, c)
    }

    def updateMetricRuleTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.UpdateMetricRuleTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.UpdateMetricRuleTemplateAction, c)
    }

    def updateMonitorGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.UpdateMonitorGroupAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.UpdateMonitorGroupAction, c)
    }

    def updateMonitorTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.monitorgroup.api.UpdateMonitorTemplateAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.monitorgroup.api.UpdateMonitorTemplateAction, c)
    }

    def deleteResNotifySubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.resnotify.DeleteResNotifySubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.resnotify.DeleteResNotifySubscriptionAction, c)
    }

    def queryResNotifySubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.resnotify.QueryResNotifySubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.resnotify.QueryResNotifySubscriptionAction, c)
    }

    def subscribeResNotify(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.resnotify.SubscribeResNotifyAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.resnotify.SubscribeResNotifyAction, c)
    }

    def updateResNotifySubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.resnotify.UpdateResNotifySubscriptionAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.resnotify.UpdateResNotifySubscriptionAction, c)
    }

    def addThirdpartyPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.thirdparty.api.AddThirdpartyPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.thirdparty.api.AddThirdpartyPlatformAction, c)
    }

    def deleteThirdpartyPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.thirdparty.api.DeleteThirdpartyPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.thirdparty.api.DeleteThirdpartyPlatformAction, c)
    }

    def querySNSEndpointThirdpartyAlertHistory(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.thirdparty.api.QuerySNSEndpointThirdpartyAlertHistoryAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.thirdparty.api.QuerySNSEndpointThirdpartyAlertHistoryAction, c)
    }

    def queryThirdpartyAlert(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.thirdparty.api.QueryThirdpartyAlertAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.thirdparty.api.QueryThirdpartyAlertAction, c)
    }

    def queryThirdpartyPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.thirdparty.api.QueryThirdpartyPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.thirdparty.api.QueryThirdpartyPlatformAction, c)
    }

    def updateThirdpartyAlerts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.thirdparty.api.UpdateThirdpartyAlertsAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.thirdparty.api.UpdateThirdpartyAlertsAction, c)
    }

    def updateThirdpartyPlatform(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.zwatch.thirdparty.api.UpdateThirdpartyPlatformAction.class) Closure c) {
        runAction(org.zstack.sdk.zwatch.thirdparty.api.UpdateThirdpartyPlatformAction, c)
    }
}
