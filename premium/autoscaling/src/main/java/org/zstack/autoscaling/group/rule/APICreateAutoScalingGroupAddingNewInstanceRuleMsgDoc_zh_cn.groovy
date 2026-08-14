package org.zstack.autoscaling.group.rule

import org.zstack.autoscaling.group.rule.APICreateAutoScalingRuleEvent

doc {
	title "CreateAutoScalingGroupAddingNewInstanceRule"

	category "autoscaling"

	desc """创建伸缩组云主机扩容规则"""

	rest {
		request {
			url "POST /v1/autoscaling/rules/adding-new-instance"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAutoScalingGroupAddingNewInstanceRuleMsg.class

			desc """"""

			params {

				column {
					name "adjustmentType"
					enclosedIn "params"
					desc "扩容方式：增加指定数量云主机，按百分比增加云主机，增加云主机数量到指定值"
					location "body"
					type "String"
					optional false
					since "3.1.0"
					values ("QuantityChangeInCapacity","PercentChangeInCapacity","TotalCapacity")
				}
				column {
					name "adjustmentValue"
					enclosedIn "params"
					desc "增加大小"
					location "body"
					type "Integer"
					optional false
					since "3.1.0"
				}
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
					name "autoScalingGroupUuid"
					enclosedIn "params"
					desc "伸缩组UUID"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "伸缩规则类型"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "cooldown"
					enclosedIn "params"
					desc "伸缩规则触发后的冷却时间"
					location "body"
					type "Long"
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
			clz APICreateAutoScalingRuleEvent.class
		}
	}
}