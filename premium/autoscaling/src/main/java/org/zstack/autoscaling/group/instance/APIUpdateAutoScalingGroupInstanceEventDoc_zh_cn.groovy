package org.zstack.autoscaling.group.instance

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceInventory

doc {

	title "更新伸缩组实例信息返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.group.instance.APIUpdateAutoScalingGroupInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.autoscaling.group.instance.APIUpdateAutoScalingGroupInstanceEvent.inventory"
		desc "null"
		type "AutoScalingGroupInstanceInventory"
		since "3.9.0"
		clz AutoScalingGroupInstanceInventory.class
	}
}
