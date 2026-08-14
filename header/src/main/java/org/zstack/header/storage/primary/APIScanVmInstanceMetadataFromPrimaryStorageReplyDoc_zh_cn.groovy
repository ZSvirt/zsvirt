package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "扫描主存储上的云主机元数据返回"

	ref {
		name "vmInstanceMetadata"
		path "org.zstack.header.storage.primary.APIScanVmInstanceMetadataFromPrimaryStorageReply.vmInstanceMetadata"
		desc "云主机元数据摘要列表"
		type "List"
		since "zsv 5.0.0"
		clz VmMetadataScanEntry.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "zsv 5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIScanVmInstanceMetadataFromPrimaryStorageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
