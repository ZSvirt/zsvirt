package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIListVmsFromSchedulingStateReply

doc {
	title "ListVmsFromSchedulingState"

	category "vmSchedulingRule"

	desc """根据调度规则uuid以及调度状态获取VM uuid"""

	rest {
		request {
			url "POST /v1/list/vms/from/executeState"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIListVmsFromSchedulingStateMsg.class

			desc """"""

			params {

				column {
					name "ruleUuid"
					enclosedIn "params"
					desc "调度规则uudi"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
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
			clz APIListVmsFromSchedulingStateReply.class
		}
	}
}