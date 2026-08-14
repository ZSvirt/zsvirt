package org.zstack.header.vm


import org.zstack.header.errorcode.ErrorCode

doc {

	title "清理云主机元数据返回"

	field {
		name "totalCleaned"
		desc "成功清理的元数据数量"
		type "Integer"
		since "zsv 5.0.0"
	}
	field {
		name "totalFailed"
		desc "清理失败的元数据数量"
		type "Integer"
		since "zsv 5.0.0"
	}
	field {
		name "failedVmUuids"
		desc "清理失败的云主机UUID列表"
		type "List"
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
		path "org.zstack.header.vm.APICleanupVmInstanceMetadataEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
