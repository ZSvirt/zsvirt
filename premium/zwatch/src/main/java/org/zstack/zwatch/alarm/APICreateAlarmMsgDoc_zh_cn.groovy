package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APICreateAlarmEvent

doc {
	title "CreateAlarm"

	category "zwatch.alarm"

	desc """创建报警器"""

	rest {
		request {
			url "POST /v1/zwatch/alarms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAlarmMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "comparisonOperator"
					enclosedIn "params"
					desc "阈值比较符"
					location "body"
					type "String"
					optional false
					since "2.3"
					values ("GreaterThanOrEqualTo","GreaterThan","LessThan","LessThanOrEqualTo")
				}
				column {
					name "period"
					enclosedIn "params"
					desc "阈值持续时间"
					location "body"
					type "Integer"
					optional true
					since "2.3"
				}
				column {
					name "namespace"
					enclosedIn "params"
					desc "名字空间"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "metricName"
					enclosedIn "params"
					desc "监控项名"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "threshold"
					enclosedIn "params"
					desc "阈值"
					location "body"
					type "Double"
					optional false
					since "2.3"
				}
				column {
					name "repeatInterval"
					enclosedIn "params"
					desc "报警重复时间"
					location "body"
					type "Integer"
					optional true
					since "2.3"
				}
				column {
					name "labels"
					enclosedIn "params"
					desc "标签列表"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "actions"
					enclosedIn "params"
					desc "报警动作列表"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "repeatCount"
					enclosedIn "params"
					desc "报警重复次数"
					location "body"
					type "Integer"
					optional true
					since "3.2"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "报警器类型"
					location "body"
					type "String"
					optional true
					since "3.1"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "emergencyLevel"
					enclosedIn "params"
					desc "报警等级"
					location "body"
					type "String"
					optional true
					since "3.8"
					values ("Emergent","Important","Normal")
				}
				column {
					name "enableRecovery"
					enclosedIn "params"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APICreateAlarmEvent.class
		}
	}
}