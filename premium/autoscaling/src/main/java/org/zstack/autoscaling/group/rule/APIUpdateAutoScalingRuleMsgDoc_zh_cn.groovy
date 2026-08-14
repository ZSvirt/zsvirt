package org.zstack.autoscaling.group.rule

import org.zstack.autoscaling.group.rule.APIUpdateAutoScalingRuleEvent

doc {
	title "UpdateAutoScalingRule"

	category "autoscaling"

	desc """修改伸缩组规则"""

	rest {
		request {
			url "PUT /v1/autoscaling/rules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAutoScalingRuleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAutoScalingRule"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "name"
					enclosedIn "updateAutoScalingRule"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "description"
					enclosedIn "updateAutoScalingRule"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "cooldown"
					enclosedIn "updateAutoScalingRule"
					desc "冷却时间"
					location "body"
					type "Long"
					optional true
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APIUpdateAutoScalingRuleEvent.class
		}
	}
}