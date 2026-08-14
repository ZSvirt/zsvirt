package org.zstack.autoscaling.group

import org.zstack.autoscaling.group.APIDeleteAutoScalingGroupEvent

doc {
	title "DeleteAutoScalingGroup"

	category "autoscaling"

	desc """删除伸缩组"""

	rest {
		request {
			url "DELETE /v1/autoscaling/groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAutoScalingGroupMsg.class

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
					since "3.1.0"
				}
			}
		}

		response {
			clz APIDeleteAutoScalingGroupEvent.class
		}
	}
}