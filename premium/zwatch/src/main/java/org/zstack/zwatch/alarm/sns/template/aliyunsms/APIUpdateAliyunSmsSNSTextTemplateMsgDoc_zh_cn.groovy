package org.zstack.zwatch.alarm.sns.template.aliyunsms

import org.zstack.zwatch.alarm.sns.template.aliyunsms.APIUpdateAliyunSmsSNSTextTemplateEvent

doc {
	title "UpdateAliyunSmsSNSTextTemplate"

	category "alarm.sns"

	desc """更新SNS阿里云短信文本模板"""

	rest {
		request {
			url "PUT /v1/zwatch/alarms/sns/text-templates/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAliyunSmsSNSTextTemplateMsg.class

			desc """"""

			params {

				column {
					name "alarmTemplateCode"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "资源报警器模板Code"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "sign"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "短信签名名称"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "eventTemplateCode"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "事件报警器模板Code"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "eventTemplate"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "事件报警器模板文本"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "uuid"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "模板的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "name"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "模板名称"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "description"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "模板的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "template"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "资源报警器模板文本"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "recoveryTemplate"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "恢复模板文本"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "defaultTemplate"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "是否为默认模板"
					location "body"
					type "Boolean"
					optional true
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "subject"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "主题"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
				column {
					name "recoverySubject"
					enclosedIn "updateAliyunSmsSNSTextTemplate"
					desc "恢复主题"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
			}
		}

		response {
			clz APIUpdateAliyunSmsSNSTextTemplateEvent.class
		}
	}
}