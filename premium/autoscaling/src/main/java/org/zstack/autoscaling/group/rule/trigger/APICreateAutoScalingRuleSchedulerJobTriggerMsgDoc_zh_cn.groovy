package org.zstack.autoscaling.group.rule.trigger

import org.zstack.autoscaling.group.rule.trigger.APICreateAutoScalingRuleTriggerEvent

doc {
	title "CreateAutoScalingRuleSchedulerJobTrigger"

	category "autoscaling"

	desc """创建伸缩规则触发器"""

	rest {
		request {
			url "POST /v1/scheduler/jobs/{schedulerJobUuid}/autoscaling/rules/{ruleUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAutoScalingRuleSchedulerJobTriggerMsg.class

			desc """"""

			params {

				column {
					name "schedulerJobUuid"
					enclosedIn "params"
					desc "定时任务Uuid"
					location "url"
					type "String"
					optional false
					since "3.10.4"
				}
				column {
					name "triggerType"
					enclosedIn "params"
					desc "触发器类型"
					location "body"
					type "String"
					optional true
					since "3.10.4"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "触发器名称"
					location "body"
					type "String"
					optional false
					since "3.10.4"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.10.4"
				}
				column {
					name "ruleUuid"
					enclosedIn "params"
					desc "伸缩规则UUID"
					location "url"
					type "String"
					optional false
					since "3.10.4"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10.4"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.10.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10.4"
				}
			}
		}

		response {
			clz APICreateAutoScalingRuleTriggerEvent.class
		}
	}
}