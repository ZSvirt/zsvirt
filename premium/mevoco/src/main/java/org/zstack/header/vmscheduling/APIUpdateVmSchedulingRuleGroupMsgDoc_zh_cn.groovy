package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIUpdateVmSchedulingRuleGroupEvent

doc {
	title "UpdateVmSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """更新vm调度组"""

	rest {
		request {
			url "PUT /v1/vmSchedulingRuleGroup/{uuid}/update"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVmSchedulingRuleGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "name"
					enclosedIn "updateVmSchedulingRuleGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "description"
					enclosedIn "updateVmSchedulingRuleGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
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
			clz APIUpdateVmSchedulingRuleGroupEvent.class
		}
	}
}