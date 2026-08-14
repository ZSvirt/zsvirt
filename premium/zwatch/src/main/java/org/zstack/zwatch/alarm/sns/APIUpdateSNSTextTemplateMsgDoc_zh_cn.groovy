package org.zstack.zwatch.alarm.sns

import org.zstack.zwatch.alarm.sns.APIUpdateSNSTextTemplateEvent

doc {
	title "UpdateSNSTextTemplate"

	category "alarm.sns"

	desc """更新SNS文本模板"""

	rest {
		request {
			url "PUT /v1/zwatch/alarms/sns/text-templates/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSTextTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSNSTextTemplate"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "updateSNSTextTemplate"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "updateSNSTextTemplate"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "template"
					enclosedIn "updateSNSTextTemplate"
					desc "模板文本"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "defaultTemplate"
					enclosedIn "updateSNSTextTemplate"
					desc "是否为默认模板"
					location "body"
					type "Boolean"
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
					name "recoveryTemplate"
					enclosedIn "updateSNSTextTemplate"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "subject"
					enclosedIn "updateSNSTextTemplate"
					desc "主题"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
				column {
					name "recoverySubject"
					enclosedIn "updateSNSTextTemplate"
					desc "恢复主题"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
			}
		}

		response {
			clz APIUpdateSNSTextTemplateEvent.class
		}
	}
}