package org.zstack.header.host

import org.zstack.header.host.Sensor
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取主机传感器信息返回"

	ref {
		name "sensors"
		path "org.zstack.header.host.APIGetHostSensorsReply.sensors"
		desc "null"
		type "List"
		since "zsv 4.10.0"
		clz Sensor.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostSensorsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
}
