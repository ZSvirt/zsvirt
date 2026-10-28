package org.zstack.autoscaling.group.instance

import org.zstack.autoscaling.group.instance.APIDeleteAutoScalingGroupInstanceEvent

doc {
	title "DeleteAutoScalingGroupInstance"

	category "autoscaling"

	desc """手动删除伸缩组内虚拟机"""

	rest {
		request {
			url "DELETE /v1/autoscaling/groups/instances/{instanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAutoScalingGroupInstanceMsg.class

			desc """"""

			params {

				column {
					name "instanceUuid"
					enclosedIn ""
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv_3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv_3.1.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "zsv_0.6"
				}
			}
		}

		response {
			clz APIDeleteAutoScalingGroupInstanceEvent.class
		}
	}
}