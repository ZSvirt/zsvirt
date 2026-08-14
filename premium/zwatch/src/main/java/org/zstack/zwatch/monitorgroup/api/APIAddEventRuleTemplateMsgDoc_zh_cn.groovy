package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIAddEventRuleTemplateEvent

doc {
	title "AddEventRuleTemplate"

	category "zwatch"

	desc """添加事件报警模板"""

	rest {
		request {
			url "POST /v1/zwatch/monitortemplates/evenrules"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddEventRuleTemplateMsg.class

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
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "namespace"
					enclosedIn "params"
					desc "命名空间"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "eventName"
					enclosedIn "params"
					desc "事件名"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "emergencyLevel"
					enclosedIn "params"
					desc "报警等级"
					location "body"
					type "String"
					optional true
					since "3.10.0"
					values ("Emergent","Important","Normal")
				}
				column {
					name "labels"
					enclosedIn "params"
					desc "事件标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
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
			clz APIAddEventRuleTemplateEvent.class
		}
	}
}