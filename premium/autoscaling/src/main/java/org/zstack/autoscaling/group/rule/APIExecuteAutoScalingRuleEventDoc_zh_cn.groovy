package org.zstack.autoscaling.group.rule

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "手动执行伸缩组规则返回值"

	ref {
		name "error"
		path "org.zstack.autoscaling.group.rule.APIExecuteAutoScalingRuleEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	field {
		name "scalingActivityUuid"
		desc "伸缩活动UUID"
		type "String"
		since "3.9.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.9.0"
	}
}
