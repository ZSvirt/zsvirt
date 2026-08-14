package org.zstack.autoscaling.group.instance

import org.zstack.header.errorcode.ErrorCode

doc {

	title "手动删除伸缩组内云主机返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.group.instance.APIDeleteAutoScalingGroupInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
}
