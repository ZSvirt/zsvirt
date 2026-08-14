package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIAddHostToHostSchedulingRuleGroupEvent

doc {
	title "AddHostToHostSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """添加host到host调度组"""

	rest {
		request {
			url "POST /v1/hostSchedulingRuleGroup/{hostGroupUuid}/host/{hostUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddHostToHostSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "hostGroupUuid"
					enclosedIn ""
					desc "host调度组uuid"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
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
			clz APIAddHostToHostSchedulingRuleGroupEvent.class
		}
	}
}