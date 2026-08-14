package org.zstack.zwatch.namespace.event

import org.zstack.header.host.HostStatus
import org.zstack.header.storage.backup.BackupStorageCanonicalEvents
import org.zstack.header.storage.backup.BackupStorageStatus
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.BackupStorageNamespace

class BackupStorageNamespaceEvent {
    BackupStorageNamespaceEvent() {
        BackupStorageNamespace.BackupStorageDisconnected.onCanonicalEvent(BackupStorageCanonicalEvents.BACKUP_STORAGE_DISCONNECTED) { BackupStorageCanonicalEvents.DisconnectedData data ->
            return new EventFamily.Event(data.backupStorageUuid, data.reason.getReadableDetails())
        }.onEventBarrier(BackupStorageCanonicalEvents.BACKUP_STORAGE_DISCONNECTED) { BackupStorageCanonicalEvents.DisconnectedData data ->
            return data.getBackupStorageUuid()
        }

        BackupStorageNamespace.BackupStorageConnected.onCanonicalEvent(BackupStorageCanonicalEvents.BACKUP_STORAGE_STATUS_CHANGED) { BackupStorageCanonicalEvents.BackupStorageStatusChangedData data ->
            if (data.newStatus == BackupStorageStatus.Connected.toString()) {
                return new EventFamily.Event(data.backupStorageUuid, data.oldStatus, data.newStatus)
            }
            return null
        }.onRecoverReturnResourceId { BackupStorageCanonicalEvents.BackupStorageStatusChangedData data ->
            if (data.newStatus == BackupStorageStatus.Connected.toString()) {
                return data.backupStorageUuid
            }
            return null
        }.onEventBarriersCleaner(BackupStorageCanonicalEvents.BACKUP_STORAGE_STATUS_CHANGED) { BackupStorageCanonicalEvents.BackupStorageStatusChangedData data ->
            if (data.newStatus == BackupStorageStatus.Connected.toString()) {
                return data.getBackupStorageUuid()
            }
            return null
        }
    }
}
