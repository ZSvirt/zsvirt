package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIChangeVmSchedulingRuleStateEvent

doc {
	title "ChangeVmSchedulingRuleState"

	category "vmSchedulingRule"

	desc """改变VM调度策略"""

	rest {
		request {
			url "PUT /v1/vmSchedulingRule/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVmSchedulingRuleStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeVmSchedulingRuleState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "state"
					enclosedIn "changeVmSchedulingRuleState"
					desc "改变为的状态"
					location "body"
					type "String"
					optional false
					since "3.16.0"
					values ("enable","disable")
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
			clz APIChangeVmSchedulingRuleStateEvent.class
		}
	}
}