package org.zstack.storage.primary.local



doc {

	title "主机本地存储云盘容量清单"

	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "zsv_0.6"
	}
	field {
		name "totalCapacity"
		desc ""
		type "long"
		since "zsv_0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "long"
		since "zsv_0.6"
	}
	field {
		name "totalPhysicalCapacity"
		desc ""
		type "long"
		since "zsv_0.6"
	}
	field {
		name "availablePhysicalCapacity"
		desc ""
		type "long"
		since "zsv_0.6"
	}
}
