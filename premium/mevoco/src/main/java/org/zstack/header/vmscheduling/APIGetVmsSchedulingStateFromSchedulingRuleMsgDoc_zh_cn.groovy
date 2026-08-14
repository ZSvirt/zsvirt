package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIGetVmsSchedulingStateFromSchedulingRuleReply

doc {
	title "GetVmsSchedulingStateFromSchedulingRule"

	category "vmSchedulingRule"

	desc """根据调度规则获取vm对应的调度状态"""

	rest {
		request {
			url "POST /v1/get/vms/schedulingState/from/SchedulingRule"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmsSchedulingStateFromSchedulingRuleMsg.class

			desc """"""

			params {

				column {
					name "ruleUuid"
					enclosedIn "params"
					desc "调度规则uuid"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "vmUuids"
					enclosedIn "params"
					desc "虚拟机uuid"
					location "body"
					type "List"
					optional false
					since "3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APIGetVmsSchedulingStateFromSchedulingRuleReply.class
		}
	}
}