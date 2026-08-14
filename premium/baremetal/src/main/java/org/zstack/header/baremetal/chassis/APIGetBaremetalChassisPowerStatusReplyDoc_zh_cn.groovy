package org.zstack.header.baremetal.chassis

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取裸机设备电源状态返回"

	ref {
		name "error"
		path "org.zstack.header.baremetal.chassis.APIGetBaremetalChassisPowerStatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6.0"
		clz ErrorCode.class
	}
	field {
		name "status"
		desc "电源状态"
		type "String"
		since "2.6.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "2.6.0"
	}
}
