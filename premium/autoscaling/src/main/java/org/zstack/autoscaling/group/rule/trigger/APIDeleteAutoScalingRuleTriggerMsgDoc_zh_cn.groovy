package org.zstack.autoscaling.group.rule.trigger

import org.zstack.autoscaling.group.rule.trigger.APIDeleteAutoScalingRuleTriggerEvent

doc {
	title "DeleteAutoScalingRuleTrigger"

	category "autoscaling"

	desc """删除伸缩规则触发器"""

	rest {
		request {
			url "DELETE /v1/autoscaling/groups/rules/triggers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAutoScalingRuleTriggerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
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
			clz APIDeleteAutoScalingRuleTriggerEvent.class
		}
	}
}