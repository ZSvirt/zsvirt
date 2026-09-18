package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "清理全部云主机元数据返回"

	field {
		name "failedPrimaryStorageUuids"
		desc "清理失败的主存储UUID列表；具体失败原因汇总见 error 字段，逐条详情见 mn / agent 日志"
		type "List"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "success"
		desc "操作是否成功；任一主存储清理失败则为 false"
		type "boolean"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APICleanupAllVmInstanceMetadataEvent.error"
		desc "错误码；success=false 时聚合所有失败主存储的失败原因"
		type "ErrorCode"
		since "1.0.0, zsv_5.1.0"
		clz ErrorCode.class
	}
}
