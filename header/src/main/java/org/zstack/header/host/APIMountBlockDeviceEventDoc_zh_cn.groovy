package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "挂载硬盘到挂载点返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIMountBlockDeviceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.3.0"
		clz ErrorCode.class
	}
}
