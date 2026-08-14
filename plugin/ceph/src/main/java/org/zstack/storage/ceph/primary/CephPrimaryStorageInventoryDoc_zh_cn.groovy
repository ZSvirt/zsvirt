package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.CephPrimaryStorageMonInventory
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	ref {
		name "mons"
		path "org.zstack.storage.ceph.primary.CephPrimaryStorageInventory.mons"
		desc "null"
		type "List"
		since "0.6"
		clz CephPrimaryStorageMonInventory.class
	}
	field {
		name "fsid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "rootVolumePoolName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "dataVolumePoolName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "imageCachePoolName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "url"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "totalCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "totalPhysicalCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "availablePhysicalCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "systemUsedCapacity"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "mountPath"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "attachedClusterUuids"
		desc ""
		type "List"
		since "0.6"
	}
}
