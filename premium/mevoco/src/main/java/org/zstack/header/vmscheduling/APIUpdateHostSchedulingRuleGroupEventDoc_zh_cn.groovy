package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.HostSchedulingRuleGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新主机调度组的请求返回"

	ref {
		name "inventory"
		path "org.zstack.header.vmscheduling.APIUpdateHostSchedulingRuleGroupEvent.inventory"
		desc "主机调度策略组清单"
		type "HostSchedulingRuleGroupInventory"
		since "3.16.0"
		clz HostSchedulingRuleGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIUpdateHostSchedulingRuleGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
