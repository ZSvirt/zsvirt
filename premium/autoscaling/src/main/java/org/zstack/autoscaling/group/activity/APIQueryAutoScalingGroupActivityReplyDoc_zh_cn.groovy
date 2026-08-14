package org.zstack.autoscaling.group.activity

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.group.activity.AutoScalingGroupActivityInventory

doc {

	title "查询伸缩组活动返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.group.activity.APIQueryAutoScalingGroupActivityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.autoscaling.group.activity.APIQueryAutoScalingGroupActivityReply.inventories"
		desc "null"
		type "List"
		since "3.1.0"
		clz AutoScalingGroupActivityInventory.class
	}
}
