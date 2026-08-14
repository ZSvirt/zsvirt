package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIListVmSchedulingRulesFromExecuteStateReply

doc {
	title "ListVmSchedulingRulesFromExecuteState"

	category "vmSchedulingRule"

	desc """根据调度状态获取对应的VM"""

	rest {
		request {
			url "POST /v1/list/vmSchedulingRules/from/conflict/state"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIListVmSchedulingRulesFromExecuteStateMsg.class

			desc """"""

			params {

				column {
					name "executeStates"
					enclosedIn "params"
					desc "调度状态(Conflict,Normal,Invalid)"
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
			clz APIListVmSchedulingRulesFromExecuteStateReply.class
		}
	}
}