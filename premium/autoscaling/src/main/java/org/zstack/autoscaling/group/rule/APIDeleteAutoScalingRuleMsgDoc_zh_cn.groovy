package org.zstack.autoscaling.group.rule

import org.zstack.autoscaling.group.rule.APIDeleteAutoScalingRuleEvent

doc {
	title "DeleteAutoScalingRule"

	category "autoscaling"

	desc """删除伸缩规则"""

	rest {
		request {
			url "DELETE /v1/autoscaling/rules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAutoScalingRuleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "伸缩规则UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteAutoScalingRuleEvent.class
		}
	}
}