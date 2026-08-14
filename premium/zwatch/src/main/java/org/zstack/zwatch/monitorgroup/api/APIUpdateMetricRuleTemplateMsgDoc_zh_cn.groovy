package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIUpdateMetricRuleTemplateEvent

doc {
	title "UpdateMetricRuleTemplate"

	category "zwatch"

	desc """更新资源报警模板"""

	rest {
		request {
			url "PUT /v1/zwatch/monitortemplates/metricrules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateMetricRuleTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateMetricRuleTemplate"
					desc "资源报警模板UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "name"
					enclosedIn "updateMetricRuleTemplate"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "comparisonOperator"
					enclosedIn "updateMetricRuleTemplate"
					desc "阈值比较符"
					location "body"
					type "String"
					optional true
					since "3.10.0"
					values ("GreaterThanOrEqualTo","GreaterThan","LessThan","LessThanOrEqualTo")
				}
				column {
					name "period"
					enclosedIn "updateMetricRuleTemplate"
					desc "阈值持续时间"
					location "body"
					type "Integer"
					optional true
					since "3.10.0"
				}
				column {
					name "threshold"
					enclosedIn "updateMetricRuleTemplate"
					desc "阈值"
					location "body"
					type "Double"
					optional true
					since "3.10.0"
				}
				column {
					name "repeatInterval"
					enclosedIn "updateMetricRuleTemplate"
					desc "报警重复时间"
					location "body"
					type "Integer"
					optional true
					since "3.10.0"
				}
				column {
					name "labels"
					enclosedIn "updateMetricRuleTemplate"
					desc "标签列表"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "repeatCount"
					enclosedIn "updateMetricRuleTemplate"
					desc "报警次数"
					location "body"
					type "Integer"
					optional true
					since "3.10.0"
				}
				column {
					name "enableRecovery"
					enclosedIn "updateMetricRuleTemplate"
					desc "开启恢复通知"
					location "body"
					type "Boolean"
					optional true
					since "3.10.0"
				}
				column {
					name "emergencyLevel"
					enclosedIn "updateMetricRuleTemplate"
					desc "报警等级"
					location "body"
					type "String"
					optional true
					since "3.10.0"
					values ("Emergent","Important","Normal")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIUpdateMetricRuleTemplateEvent.class
		}
	}
}