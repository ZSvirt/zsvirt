package org.zstack.autoscaling.group

import org.zstack.autoscaling.group.APICreateAutoScalingGroupEvent

doc {
	title "CreateAutoScalingGroup"

	category "autoscaling"

	desc """创建伸缩组"""

	rest {
		request {
			url "POST /v1/autoscaling/groups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAutoScalingGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "scalingResourceType"
					enclosedIn "params"
					desc "伸缩组伸缩资源类型，目前只支持云主机"
					location "body"
					type "String"
					optional false
					since "3.1.0"
					values ("VmInstance")
				}
				column {
					name "minResourceSize"
					enclosedIn "params"
					desc "伸缩组内云主机最少数量"
					location "body"
					type "Integer"
					optional false
					since "3.1.0"
				}
				column {
					name "maxResourceSize"
					enclosedIn "params"
					desc "伸缩组内云主机最多数量"
					location "body"
					type "Integer"
					optional false
					since "3.1.0"
				}
				column {
					name "defaultCooldown"
					enclosedIn "params"
					desc "伸缩组规则默认冷却时间"
					location "body"
					type "Long"
					optional false
					since "3.1.0"
				}
				column {
					name "removalPolicy"
					enclosedIn "params"
					desc "删除云主机规则"
					location "body"
					type "String"
					optional false
					since "3.1.0"
					values ("OldestInstance","NewestInstance","OldestScalingConfiguration","MinimumCPUUsageInstance","MinimumMemoryUsageInstance")
				}
				column {
					name "defaultEnable"
					enclosedIn "params"
					desc "创建完成后，是否默认启用"
					location "body"
					type "boolean"
					optional true
					since "3.1.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APICreateAutoScalingGroupEvent.class
		}
	}
}