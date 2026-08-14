package org.zstack.header.storageDevice

import org.zstack.header.host.HostInventory
import org.zstack.header.host.HostInventory
import java.lang.Boolean

doc {

	title "SCSI Lun与集群关系检查结果清单"

	ref {
		name "attachedHosts"
		path "org.zstack.header.storageDevice.ScsiLunClusterStatusInventory.attachedHostUuids"
		desc "与SCSI Lun已加载的物理机"
		type "List"
		since "3.1.0"
		clz HostInventory.class
	}
	ref {
		name "unattachedHosts"
		path "org.zstack.header.storageDevice.ScsiLunClusterStatusInventory.unattachedHostUuids"
		desc "未与SCSI Lun加载的物理机"
		type "List"
		since "3.1.0"
		clz HostInventory.class
	}
	field {
		name "isAllHostsAttached"
		desc "SCSI Lun是否已与集群全部物理机连接"
		type "Boolean"
		since "3.1.0"
	}
}
