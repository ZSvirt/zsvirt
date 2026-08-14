package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIDeleteVmSchedulingRuleGroupEvent

doc {
	title "DeleteVmSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """删除vm调度组"""

	rest {
		request {
			url "DELETE /v1/vmSchedulingRuleGroup/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
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
			clz APIDeleteVmSchedulingRuleGroupEvent.class
		}
	}
}