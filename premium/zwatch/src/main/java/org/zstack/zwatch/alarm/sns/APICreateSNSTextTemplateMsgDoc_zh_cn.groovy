package org.zstack.zwatch.alarm.sns

import org.zstack.zwatch.alarm.sns.APICreateSNSTextTemplateEvent

doc {
	title "CreateSNSTextTemplate"

	category "未知类别"

	desc """创建SNS监控模板"""

	rest {
		request {
			url "POST /v1/zwatch/alarms/sns/text-templates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSTextTemplateMsg.class

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
					name "applicationPlatformType"
					enclosedIn "params"
					desc "SNS应用平台类型"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "template"
					enclosedIn "params"
					desc "模板文本"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "defaultTemplate"
					enclosedIn "params"
					desc "是否作为默认模板"
					location "body"
					type "Boolean"
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
					name "recoveryTemplate"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("alarm","event","combined")
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
					name "subject"
					enclosedIn "params"
					desc "主题"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
				column {
					name "recoverySubject"
					enclosedIn "params"
					desc "恢复主题"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
			}
		}

		response {
			clz APICreateSNSTextTemplateEvent.class
		}
	}
}