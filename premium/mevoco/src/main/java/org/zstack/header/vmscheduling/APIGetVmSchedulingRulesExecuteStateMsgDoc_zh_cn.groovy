package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIGetVmSchedulingRulesExecuteStateReply

doc {
	title "GetVmSchedulingRulesExecuteState"

	category "vmSchedulingRule"

	desc """获取vm调度组的执行状态"""

	rest {
		request {
			url "POST /v1/get/vmSchedulingRules/conflict/state"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmSchedulingRulesExecuteStateMsg.class

			desc """"""

			params {

				column {
					name "uuids"
					enclosedIn "params"
					desc "vm调度组uuid"
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
			clz APIGetVmSchedulingRulesExecuteStateReply.class
		}
	}
}