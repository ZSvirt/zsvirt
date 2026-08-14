package org.zstack.autoscaling.group.rule

import org.zstack.autoscaling.group.rule.APIExecuteAutoScalingRuleEvent

doc {
	title "ExecuteAutoScalingRule"

	category "autoscaling"

	desc """手动执行伸缩组规则"""

	rest {
		request {
			url "PUT /v1/autoscaling/rules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExecuteAutoScalingRuleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "executeAutoScalingRule"
					desc "伸缩组规则UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIExecuteAutoScalingRuleEvent.class
		}
	}
}