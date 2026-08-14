package org.zstack.zwatch.alarm.sns.template.aliyunsms

import org.zstack.zwatch.alarm.sns.APICreateSNSTextTemplateEvent

doc {
	title "CreateAliyunSmsSNSTextTemplate"

	category "alarm.sns"

	desc """创建SNS监控阿里云短信模板"""

	rest {
		request {
			url "POST /v1/zwatch/alarms/sns/text-templates/aliyun-sms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAliyunSmsSNSTextTemplateMsg.class

			desc """"""

			params {

				column {
					name "sign"
					enclosedIn "params"
					desc "短信签名名称"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "alarmTemplateCode"
					enclosedIn "params"
					desc "资源报警器模板Code"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "eventTemplateCode"
					enclosedIn "params"
					desc "事件报警器模板Code"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "eventTemplate"
					enclosedIn "params"
					desc "事件报警器模板文本"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "模板名称"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "模板的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "applicationPlatformType"
					enclosedIn "params"
					desc "SNS应用平台类型"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "template"
					enclosedIn "params"
					desc "资源报警器模板文本"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "recoveryTemplate"
					enclosedIn "params"
					desc "恢复模板文本"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "defaultTemplate"
					enclosedIn "params"
					desc "是否作为默认模板"
					location "body"
					type "Boolean"
					optional true
					since "3.7.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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