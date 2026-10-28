package org.zstack.autoscaling.group.instance

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceInventory

doc {

	title "查询伸缩组组内虚拟机列表返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.group.instance.APIQueryAutoScalingGroupInstanceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.autoscaling.group.instance.APIQueryAutoScalingGroupInstanceReply.inventories"
		desc "null"
		type "List"
		since "zsv_3.1.0"
		clz AutoScalingGroupInstanceInventory.class
	}
}
