package org.zstack.autoscaling.group

import org.zstack.autoscaling.group.APIChangeAutoScalingGroupStateEvent

doc {
	title "ChangeAutoScalingGroupState"

	category "autoscaling"

	desc """修改伸缩组启用状态"""

	rest {
		request {
			url "PUT /v1/autoscaling/groups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAutoScalingGroupStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeAutoScalingGroupState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "stateEvent"
					enclosedIn "changeAutoScalingGroupState"
					desc "设置为开启或者关闭"
					location "body"
					type "String"
					optional false
					since "3.1.0"
					values ("enable","disable")
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
			clz APIChangeAutoScalingGroupStateEvent.class
		}
	}
}