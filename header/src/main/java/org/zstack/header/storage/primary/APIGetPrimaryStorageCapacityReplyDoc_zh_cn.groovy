package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "主存储容量信息"

	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageCapacityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "totalCapacity"
		desc ""
		type "long"
		since "zsv 0.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "long"
		since "zsv 0.6"
	}
	field {
		name "totalPhysicalCapacity"
		desc ""
		type "long"
		since "zsv 0.6"
	}
	field {
		name "availablePhysicalCapacity"
		desc ""
		type "long"
		since "zsv 0.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
}
