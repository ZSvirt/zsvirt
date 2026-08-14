package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIAddMetricRuleTemplateEvent

doc {
	title "AddMetricRuleTemplate"

	category "zwatch"

	desc """添加资源报警模板"""

	rest {
		request {
			url "POST /v1/zwatch/monitortemplates/{monitorTemplateUuid}/metricrules"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddMetricRuleTemplateMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "monitorTemplateUuid"
					enclosedIn "params"
					desc "模板UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "comparisonOperator"
					enclosedIn "params"
					desc "阈值比较符"
					location "body"
					type "String"
					optional false
					since "3.10.0"
					values ("GreaterThanOrEqualTo","GreaterThan","LessThan","LessThanOrEqualTo")
				}
				column {
					name "period"
					enclosedIn "params"
					desc "阈值持续时间"
					location "body"
					type "Integer"
					optional true
					since "3.10.0"
				}
				column {
					name "namespace"
					enclosedIn "params"
					desc "名字空间"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "metricName"
					enclosedIn "params"
					desc "监控项名称"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "threshold"
					enclosedIn "params"
					desc "阈值"
					location "body"
					type "Double"
					optional false
					since "3.10.0"
				}
				column {
					name "repeatInterval"
					enclosedIn "params"
					desc "报警重复时间"
					location "body"
					type "Integer"
					optional true
					since "3.10.0"
				}
				column {
					name "labels"
					enclosedIn "params"
					desc "标签列表"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "repeatCount"
					enclosedIn "params"
					desc "报警次数"
					location "body"
					type "Integer"
					optional true
					since "3.10.0"
				}
				column {
					name "enableRecovery"
					enclosedIn "params"
					desc "开启恢复通知"
					location "body"
					type "Boolean"
					optional true
					since "3.10.0"
				}
				column {
					name "emergencyLevel"
					enclosedIn "params"
					desc "报警分级"
					location "body"
					type "String"
					optional true
					since "3.10.0"
					values ("Emergent","Important","Normal")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.10.0"
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
			clz APIAddMetricRuleTemplateEvent.class
		}
	}
}