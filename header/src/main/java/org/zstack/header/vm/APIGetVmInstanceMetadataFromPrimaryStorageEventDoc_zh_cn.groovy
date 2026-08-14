package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取云主机元数据返回"

	field {
		name "metadata"
		desc "云主机元数据内容"
		type "String"
		since "zsv 5.0.0"
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "zsv 5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmInstanceMetadataFromPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
