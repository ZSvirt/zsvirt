package org.zstack.zwatch.namespace.event

import org.zstack.core.db.Q
import org.zstack.header.host.HostCanonicalEvents
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.storage.primary.sharedblock.SharedBlockGroupVO
import org.zstack.storage.primary.sharedblock.SharedBlockGroupVO_
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.PrimaryStorageNamespace

class PrimaryStorageNamespaceEvent {
    PrimaryStorageNamespaceEvent() {
        PrimaryStorageNamespace.PrimaryStorageDisconnected.onCanonicalEvent(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DISCONNECTED) { PrimaryStorageCanonicalEvent.DisconnectedData data ->
            return new EventFamily.Event(data.primaryStorageUuid, data.getReason().getReadableDetails())
        }.onEventBarrier(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DISCONNECTED) { PrimaryStorageCanonicalEvent.DisconnectedData data ->
            return data.primaryStorageUuid
        }

        PrimaryStorageNamespace.PrimaryStorageConnected.onCanonicalEvent(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATUS_CHANGED_PATH) { PrimaryStorageCanonicalEvent.PrimaryStorageStatusChangedData data ->
            if (data.newStatus == PrimaryStorageStatus.Connected.toString()) {
                return new EventFamily.Event(data.primaryStorageUuid, data.oldStatus, data.newStatus)
            }
            return null
        }.onRecoverReturnResourceId { PrimaryStorageCanonicalEvent.PrimaryStorageStatusChangedData data ->
            if (data.newStatus == PrimaryStorageStatus.Connected.toString()) {
                return data.primaryStorageUuid
            }
            return null
        }.onEventBarriersCleaner(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATUS_CHANGED_PATH) { PrimaryStorageCanonicalEvent.PrimaryStorageStatusChangedData data ->
            if (data.newStatus == PrimaryStorageStatus.Connected.toString()) {
                return data.primaryStorageUuid
            }
            return null
        }

        PrimaryStorageNamespace.PrimaryStorageHostDisconnected.onCanonicalEvent(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH) { PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData data ->
            if (data.newStatus == PrimaryStorageHostStatus.Disconnected && data.oldStatus == PrimaryStorageHostStatus.Connected) {
                return new EventFamily.Event(data.primaryStorageUuid, data.getReason() != null ? data.getReason().getReadableDetails() : "", data.getHostUuid())
            }

            return null
        }.onEventBarrier(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH) { PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData data ->
            if (data.newStatus == PrimaryStorageHostStatus.Disconnected
                    || data.newStatus == PrimaryStorageHostStatus.Connected) {
                return makePrimaryStorageHostKey(data.getPrimaryStorageUuid(), data.getHostUuid())
            }

            return null
        }.onEventBarriersCleaner(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH) { PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData data ->
            if (data.newStatus == PrimaryStorageHostStatus.Connected) {
                return makePrimaryStorageHostKey(data.getPrimaryStorageUuid(), data.getHostUuid())
            }

            return null
        }

        PrimaryStorageNamespace.SharedBlockStateAbnormal.onCanonicalEvent(HostCanonicalEvents.HOST_PHYSICAL_VOLUME_STATE_ABNORMAL) { HostCanonicalEvents.HostPhysicalVolumeStateAbnormalData data ->
            if (Q.New(SharedBlockGroupVO.class).eq(SharedBlockGroupVO_.uuid, data.vgName).isExists()) {
                return new EventFamily.Event(data.vgName, data.hostUuid, data.diskUuids, data.diskName, data.state)
            }
            return null
        }
    }

    private static String makePrimaryStorageHostKey(String primaryStorageUuid, String hostUuid) {
        return String.format("%s.%s", primaryStorageUuid, hostUuid)
    }
}
