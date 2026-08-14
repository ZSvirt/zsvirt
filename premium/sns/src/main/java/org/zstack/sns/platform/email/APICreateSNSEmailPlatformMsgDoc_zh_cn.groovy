package org.zstack.sns.platform.email

import org.zstack.sns.APICreateSNSApplicationPlatformEvent

doc {
	title "CreateSNSEmailPlatform"

	category "sns"

	desc """创建SNS邮件平台"""

	rest {
		request {
			url "POST /v1/sns/application-platforms/email"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSEmailPlatformMsg.class

			desc """"""

			params {

				column {
					name "smtpServer"
					enclosedIn "params"
					desc "SMTP服务器地址"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "smtpPort"
					enclosedIn "params"
					desc "SMTP端口"
					location "body"
					type "Integer"
					optional false
					since "2.3"
				}
				column {
					name "username"
					enclosedIn "params"
					desc "用户名"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
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
					name "encryptType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("SSL","STARTTLS","NONE")
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
			clz APICreateSNSApplicationPlatformEvent.class
		}
	}
}