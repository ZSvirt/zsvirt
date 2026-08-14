package org.zstack.autoscaling.group.instance

import org.zstack.autoscaling.group.instance.APIUpdateAutoScalingGroupInstanceEvent

doc {
	title "UpdateAutoScalingGroupInstance"

	category "autoscaling"

	desc """更新伸缩组实例信息"""

	rest {
		request {
			url "PUT /v1/autoscaling/groups/instances/{instanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAutoScalingGroupInstanceMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn "updateAutoScalingGroupInstance"
					desc "伸缩组UUID"
					location "body"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "instanceUuid"
					enclosedIn "updateAutoScalingGroupInstance"
					desc "伸缩组内实例UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "protectionStrategy"
					enclosedIn "updateAutoScalingGroupInstance"
					desc "伸缩组内实例保护策略"
					location "body"
					type "String"
					optional true
					since "3.9.0"
					values ("Protected","Unprotected")
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
			clz APIUpdateAutoScalingGroupInstanceEvent.class
		}
	}
}