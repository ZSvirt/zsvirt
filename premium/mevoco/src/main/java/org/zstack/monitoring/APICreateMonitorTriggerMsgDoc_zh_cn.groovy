package org.zstack.monitoring

import org.zstack.monitoring.APICreateMonitorTriggerEvent

doc {
	title "CreateMonitorTrigger"

	category "monitoring"

	desc """创建报警器"""

	rest {
		request {
			url "POST /v1/monitoring/triggers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateMonitorTriggerMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "expression"
					enclosedIn "params"
					desc "报警表达式"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "duration"
					enclosedIn "params"
					desc "持续时间。当报警表达式被触发超过时间后，发出报警"
					location "body"
					type "Integer"
					optional false
					since "2.1"
				}
				column {
					name "recoveryExpression"
					enclosedIn "params"
					desc "保留字段，未使用"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "targetResourceUuid"
					enclosedIn "params"
					desc "目标资源UUID。例如对虚拟机创建报警器，则该字段为虚拟机UUID"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
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
			}
		}

		response {
			clz APICreateMonitorTriggerEvent.class
		}
	}
}