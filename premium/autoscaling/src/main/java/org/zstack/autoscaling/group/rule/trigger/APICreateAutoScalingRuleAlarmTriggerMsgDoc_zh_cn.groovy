package org.zstack.autoscaling.group.rule.trigger

import org.zstack.autoscaling.group.rule.trigger.APICreateAutoScalingRuleTriggerEvent

doc {
	title "CreateAutoScalingRuleAlarmTrigger"

	category "autoscaling"

	desc """创建伸缩规则触发器-基于云主机监控信息"""

	rest {
		request {
			url "POST /v1/zwatch/alarms/{alarmUuid}/autoscaling/rules/{ruleUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAutoScalingRuleAlarmTriggerMsg.class

			desc """"""

			params {

				column {
					name "alarmUuid"
					enclosedIn "params"
					desc "报警UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "triggerType"
					enclosedIn "params"
					desc "触发器类型"
					location "body"
					type "String"
					optional true
					since "3.1.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "触发器名称"
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
					name "ruleUuid"
					enclosedIn "params"
					desc "伸缩规则UUID"
					location "url"
					type "String"
					optional false
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
			clz APICreateAutoScalingRuleTriggerEvent.class
		}
	}
}