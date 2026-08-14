package org.zstack.autoscaling.group

import org.zstack.autoscaling.group.APIUpdateAutoScalingGroupEvent

doc {
	title "UpdateAutoScalingGroup"

	category "autoscaling"

	desc """修改伸缩组"""

	rest {
		request {
			url "PUT /v1/autoscaling/groups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAutoScalingGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAutoScalingGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "name"
					enclosedIn "updateAutoScalingGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "description"
					enclosedIn "updateAutoScalingGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "minResourceSize"
					enclosedIn "updateAutoScalingGroup"
					desc "伸缩组内最少云主机数量"
					location "body"
					type "Integer"
					optional true
					since "3.1.0"
				}
				column {
					name "maxResourceSize"
					enclosedIn "updateAutoScalingGroup"
					desc "伸缩组内最大云主机数量"
					location "body"
					type "Integer"
					optional true
					since "3.1.0"
				}
				column {
					name "removalPolicy"
					enclosedIn "updateAutoScalingGroup"
					desc "删除云主机策略：创建时间最久的主机优先，新创建的云主机优先"
					location "body"
					type "String"
					optional true
					since "3.1.0"
					values ("OldestInstance","NewestInstance","OldestScalingConfiguration","MinimumCPUUsageInstance","MinimumMemoryUsageInstance")
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
			clz APIUpdateAutoScalingGroupEvent.class
		}
	}
}