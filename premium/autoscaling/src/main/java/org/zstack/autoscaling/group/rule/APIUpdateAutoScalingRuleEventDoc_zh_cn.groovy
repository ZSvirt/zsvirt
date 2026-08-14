package org.zstack.autoscaling.group.rule

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.group.rule.AutoScalingRuleInventory

doc {

	title "修改伸缩组规则返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.group.rule.APIUpdateAutoScalingRuleEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.autoscaling.group.rule.APIUpdateAutoScalingRuleEvent.inventory"
		desc "null"
		type "AutoScalingRuleInventory"
		since "3.1.0"
		clz AutoScalingRuleInventory.class
	}
}
