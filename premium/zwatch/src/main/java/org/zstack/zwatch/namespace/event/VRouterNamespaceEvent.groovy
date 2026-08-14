package org.zstack.zwatch.namespace.event

import org.zstack.appliancevm.ApplianceVmCanonicalEvents
import org.zstack.appliancevm.ApplianceVmStatus
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vpc.VpcConstants
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.VRouterNamespace

class VRouterNamespaceEvent {
    VRouterNamespaceEvent() {
        VRouterNamespace.VRouterDisconnected.onCanonicalEvent(ApplianceVmCanonicalEvents.DISCONNECTED_PATH) { ApplianceVmCanonicalEvents.DisconnectedData data ->
            if (data.getApplianceVmType() == VyosConstants.VYOS_VM_TYPE || data.getApplianceVmType() == VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE || data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) {
                return new EventFamily.Event(data.applianceVmUuid, data.reason.getReadableDetails())
            }
            return null
        }.onEventBarrier(ApplianceVmCanonicalEvents.DISCONNECTED_PATH) { ApplianceVmCanonicalEvents.DisconnectedData data ->
            return getVRouterUuid(data.applianceVmUuid)
        }

        VRouterNamespace.VRouterPaused.onCanonicalEvent(ApplianceVmCanonicalEvents.APPLIANCEVM_STATE_CHANGED_PATH) { ApplianceVmCanonicalEvents.ApplianceVmStateChangeData data  ->
            if (data.getNewState() != data.getOldState() && data.getNewState() == VmInstanceState.Paused.toString()) {
                return new EventFamily.Event(data.applianceVmUuid, data.getOldState(), data.getNewState(), data.getInv().getApplianceVmType())
            }
            return null
        }.onRecoverReturnResourceId { ApplianceVmCanonicalEvents.ApplianceVmStateChangeData data ->
            if (data.getNewState() != data.getOldState() && data.getNewState() == VmInstanceState.Paused.toString()) {
                return getVRouterUuid(data.getApplianceVmUuid())
            }
            return null
        }.onEventBarrier(ApplianceVmCanonicalEvents.APPLIANCEVM_STATE_CHANGED_PATH)  { ApplianceVmCanonicalEvents.ApplianceVmStateChangeData data   ->
            if (data.getNewState() != data.getOldState() && data.getNewState() == VmInstanceState.Paused.toString()) {
                return getVRouterUuid(data.getApplianceVmUuid())
            }
            return null
        }

        VRouterNamespace.VRouterConnected.onCanonicalEvent(ApplianceVmCanonicalEvents.APPLIANCEVM_STATUS_CHANGED_PATH) { ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData data ->
            if ((data.getApplianceVmType() == VyosConstants.VYOS_VM_TYPE || data.getApplianceVmType() == VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE || data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) && data.newStatus == ApplianceVmStatus.Connected.toString()) {
                return new EventFamily.Event(data.applianceVmUuid, data.oldStatus, data.newStatus)
            }
            return null
        }.onRecoverReturnResourceId { ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData data ->
            if ((data.getApplianceVmType() == VyosConstants.VYOS_VM_TYPE || data.getApplianceVmType() == VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE || data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) && data.newStatus == ApplianceVmStatus.Connected.toString()) {
                return getVRouterUuid(data.applianceVmUuid)
            }
            return null
        }.onEventBarriersCleaner(ApplianceVmCanonicalEvents.APPLIANCEVM_STATUS_CHANGED_PATH) { ApplianceVmCanonicalEvents.ApplianceVmStatusChangedData data ->
            if ((data.getApplianceVmType() == VyosConstants.VYOS_VM_TYPE || data.getApplianceVmType() == VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE || data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) && data.newStatus == ApplianceVmStatus.Connected.toString()) {
                return getVRouterUuid(data.applianceVmUuid)
            }
            return null
        }

        VRouterNamespace.VRouterServiceUnhealthy.onCanonicalEvent(ApplianceVmCanonicalEvents.SERVICE_UNHEALTHY_PATH) { ApplianceVmCanonicalEvents.ServiceHealthData data ->
            if (data.getApplianceVmType() == VyosConstants.VYOS_VM_TYPE || data.getApplianceVmType() == VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE || data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) {
                return new EventFamily.Event(data.applianceVmUuid, data.reason.getReadableDetails())
            }
            return null
        }.onEventBarrier(ApplianceVmCanonicalEvents.SERVICE_UNHEALTHY_PATH) { ApplianceVmCanonicalEvents.ServiceHealthData data ->
            return getVRouterServiceHealthKey(data.applianceVmUuid)
        }

        VRouterNamespace.VRouterServiceHealthy.onCanonicalEvent(ApplianceVmCanonicalEvents.SERVICE_HEALTHY_PATH) { ApplianceVmCanonicalEvents.ServiceHealthData data ->
            if (data.healthy) {
                return new EventFamily.Event(data.applianceVmUuid, "Healthy")
            }

            return null
        }.onRecoverReturnResourceId { ApplianceVmCanonicalEvents.ServiceHealthData data ->
            if (data.healthy) {
                return getVRouterServiceHealthKey(data.applianceVmUuid)
            }
            return null
        }.onEventBarriersCleaner(ApplianceVmCanonicalEvents.SERVICE_HEALTHY_PATH) { ApplianceVmCanonicalEvents.ServiceHealthData data ->
            if (data.healthy) {
                return getVRouterServiceHealthKey(data.applianceVmUuid)
            }
            return null
        }

        VRouterNamespace.MasterVpcRouterChanged.onCanonicalEvent(ApplianceVmCanonicalEvents.APPLIANCEVM_HASTATUS_CHANGED_PATH) { ApplianceVmCanonicalEvents.ApplianceVmHaStatusChangedData data ->
            if (data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) {
                return new EventFamily.Event(data.applianceVmUuid, data.reason.getReadableDetails())
            }
            return null
        }.onRecoverReturnResourceId { ApplianceVmCanonicalEvents.ApplianceVmHaStatusChangedData data ->
            if (data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) {
                return getVpcRouterUuid(data.applianceVmUuid)
            }
            return null
        }.onEventBarrier(ApplianceVmCanonicalEvents.APPLIANCEVM_HASTATUS_CHANGED_PATH) { ApplianceVmCanonicalEvents.ApplianceVmHaStatusChangedData data ->
            return getVpcRouterUuid(data.applianceVmUuid)
        }
        
        VRouterNamespace.VRouterAbnormalFilesExists.onCanonicalEvent(ApplianceVmCanonicalEvents.APPLIANCEVM_ABNORMAL_FILE_REPORT_PATH) { ApplianceVmCanonicalEvents.ApplianceVmAbnormalFilesDate data ->
            if (data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) {
                return new EventFamily.Event(data.applianceVmUuid, data.diskTotal, data.diskUsed, data.diskUsedutilization, data.abnormalFiles)
            }
            return null
        }.onRecoverReturnResourceId { ApplianceVmCanonicalEvents.ApplianceVmAbnormalFilesDate data ->
            if (data.getApplianceVmType() == VpcConstants.VPC_VROUTER_VM_TYPE) {
                return getVpcRouterUuid(data.applianceVmUuid)
            }
            return null
        }.onEventBarrier(ApplianceVmCanonicalEvents.APPLIANCEVM_ABNORMAL_FILE_REPORT_PATH) { ApplianceVmCanonicalEvents.ApplianceVmAbnormalFilesDate data ->
            return getVpcRouterUuid(data.applianceVmUuid)
        }

    }

    private static String getVRouterServiceHealthKey(String uuid) {
        return String.format("Health for %s", getVRouterUuid(uuid))
    }

    private static String getVRouterUuid (String uuid) {
        return String.format("vrouter:%s", uuid)
    }

    private static String getVpcRouterUuid (String uuid) {
        return String.format("master vpc router:%s", uuid)
    }
}
