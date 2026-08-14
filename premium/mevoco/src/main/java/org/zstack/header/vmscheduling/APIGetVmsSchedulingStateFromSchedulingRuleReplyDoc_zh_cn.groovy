package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.VmSchedulingRuleExecuteState
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机根据调度规则执行的调度状态返回"

	ref {
		name "ruleMapState"
		path "org.zstack.header.vmscheduling.APIGetVmsSchedulingStateFromSchedulingRuleReply.ruleMapState"
		desc "虚拟机调度规则执行状态的映射表"
		type "Map"
		since "3.16.0"
		clz VmSchedulingRuleExecuteState.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIGetVmsSchedulingStateFromSchedulingRuleReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
