package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIUpdateHostSchedulingRuleGroupEvent

doc {
	title "UpdateHostSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """更新物理机调度组"""

	rest {
		request {
			url "PUT /v1/hostSchedulingRuleGroup/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHostSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateHostSchedulingRuleGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "name"
					enclosedIn "updateHostSchedulingRuleGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "description"
					enclosedIn "updateHostSchedulingRuleGroup"
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
			clz APIUpdateHostSchedulingRuleGroupEvent.class
		}
	}
}