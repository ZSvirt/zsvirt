package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIDetachHostFromHostSchedulingRuleGroupEvent

doc {
	title "DetachHostFromHostSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """从物理机调度组解绑物理机"""

	rest {
		request {
			url "DELETE /v1/hostSchedulingRuleGroup/{hostGroupUuid}/host"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachHostFromHostSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "hostGroupUuid"
					enclosedIn ""
					desc "物理机调度组uuid"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APIDetachHostFromHostSchedulingRuleGroupEvent.class
		}
	}
}