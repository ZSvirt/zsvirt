package org.zstack.autoscaling.group

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.group.AutoScalingGroupInventory

doc {

	title "创建伸缩组返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.group.APICreateAutoScalingGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.autoscaling.group.APICreateAutoScalingGroupEvent.inventory"
		desc "null"
		type "AutoScalingGroupInventory"
		since "3.1.0"
		clz AutoScalingGroupInventory.class
	}
}
