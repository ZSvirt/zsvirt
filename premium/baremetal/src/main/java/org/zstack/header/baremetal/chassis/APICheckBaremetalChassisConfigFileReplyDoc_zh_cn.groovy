package org.zstack.header.baremetal.chassis

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "批量添加裸金属设备文件合法性检查结果"

	ref {
		name "error"
		path "org.zstack.header.baremetal.chassis.APICheckBaremetalChassisConfigFileReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.1"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc "是否成功"
		type "boolean"
		since "3.1.1"
	}
}
