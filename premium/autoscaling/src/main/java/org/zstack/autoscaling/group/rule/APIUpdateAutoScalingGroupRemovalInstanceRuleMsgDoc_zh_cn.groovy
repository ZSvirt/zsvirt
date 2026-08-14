package org.zstack.autoscaling.group.rule

import org.zstack.autoscaling.group.rule.APIUpdateAutoScalingRuleEvent

doc {
	title "UpdateAutoScalingGroupRemovalInstanceRule"

	category "autoscaling"

	desc """修改伸缩组缩容规则"""

	rest {
		request {
			url "PUT /v1/autoscaling/rules/removal-instance/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAutoScalingGroupRemovalInstanceRuleMsg.class

			desc """"""

			params {

				column {
					name "adjustmentType"
					enclosedIn "updateAutoScalingGroupRemovalInstanceRule"
					desc "缩容方式：减少指定数量云主机，按百分比减少云主机，减少云主机数量到指定值"
					location "body"
					type "String"
					optional true
					since "3.1.0"
					values ("QuantityChangeInCapacity","PercentChangeInCapacity","TotalCapacity")
				}
				column {
					name "adjustmentValue"
					enclosedIn "updateAutoScalingGroupRemovalInstanceRule"
					desc "缩容数值"
					location "body"
					type "Integer"
					optional true
					since "3.1.0"
				}
				column {
					name "removalPolicy"
					enclosedIn "updateAutoScalingGroupRemovalInstanceRule"
					desc "删除云主机策略"
					location "body"
					type "String"
					optional true
					since "3.1.0"
					values ("OldestInstance","NewestInstance","OldestScalingConfiguration","MinimumCPUUsageInstance","MinimumMemoryUsageInstance")
				}
				column {
					name "uuid"
					enclosedIn "updateAutoScalingGroupRemovalInstanceRule"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "name"
					enclosedIn "updateAutoScalingGroupRemovalInstanceRule"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "description"
					enclosedIn "updateAutoScalingGroupRemovalInstanceRule"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "cooldown"
					enclosedIn "updateAutoScalingGroupRemovalInstanceRule"
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