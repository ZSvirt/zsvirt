package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIDetachVmFromVmSchedulingRuleGroupEvent

doc {
	title "DetachVmFromVmSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """从vm调度组解绑vm"""

	rest {
		request {
			url "DELETE /v1/vmSchedulingRuleGroup/{vmGroupUuid}/vmInstance/"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachVmFromVmSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "vmGroupUuid"
					enclosedIn ""
					desc "vm调度组uuid"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "vmUuid"
					enclosedIn ""
					desc "vm uuid"
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
			clz APIDetachVmFromVmSchedulingRuleGroupEvent.class
		}
	}
}