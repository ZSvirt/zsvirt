package org.zstack.zmigrate.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取ZMigrate管理节点信息返回"

	field {
		name "zmigrateVmInstanceStatus"
		desc "ZMigrate虚拟机实例状态"
		type "String"
		since "5.0.0"
	}
	field {
		name "version"
		desc "ZMigrate版本"
		type "String"
		since "5.0.0"
	}
	field {
		name "platformsCount"
		desc "ZMigrate平台数量"
		type "long"
		since "5.0.0"
	}
	field {
		name "gatewaysCount"
		desc "ZMigrate网关数量"
		type "long"
		since "5.0.0"
	}
	field {
		name "migrateJobsCount"
		desc "ZMigrate迁移任务数量"
		type "long"
		since "5.0.0"
	}
	field {
		name "zmigrateStartTime"
		desc "ZMigrate管理节点启动时间"
		type "long"
		since "5.0.0"
	}
	field {
		name "vddkUploaded"
		desc "ZMigrate管理虚拟机是否已成功安装VDDK"
		type "boolean"
		since "5.1.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.zmigrate.api.APIGetZMigrateInfosReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
