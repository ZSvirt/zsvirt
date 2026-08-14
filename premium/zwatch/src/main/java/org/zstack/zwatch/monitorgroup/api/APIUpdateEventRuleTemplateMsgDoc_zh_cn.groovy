package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIUpdateEventRuleTemplateEvent

doc {
	title "UpdateEventRuleTemplate"

	category "zwatch"

	desc """更新事件报警模板"""

	rest {
		request {
			url "PUT /v1/zwatch/monitortemplates/evenrules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateEventRuleTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateEventRuleTemplate"
					desc "事件报警模板UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "name"
					enclosedIn "updateEventRuleTemplate"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "emergencyLevel"
					enclosedIn "updateEventRuleTemplate"
					desc "报警等级"
					location "body"
					type "String"
					optional true
					since "3.10.0"
					values ("Emergent","Important","Normal")
				}
				column {
					name "labels"
					enclosedIn "updateEventRuleTemplate"
					desc "事件标签"
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
			clz APIUpdateEventRuleTemplateEvent.class
		}
	}
}