package org.zstack.zmigrate.api

import org.zstack.header.image.ImageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取ZMigrate网关虚拟机实例返回"

	field {
		name "managementVmInstanceUuid"
		desc ""
		type "String"
		since "5.0.0"
	}
	field {
		name "gatewayVmInstances"
		desc ""
		type "List"
		since "5.0.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.zmigrate.api.APIGetZMigrateGatewayVmInstancesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
