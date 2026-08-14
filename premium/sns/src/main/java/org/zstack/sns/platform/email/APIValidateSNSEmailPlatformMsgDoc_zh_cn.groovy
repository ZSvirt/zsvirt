package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APIValidateSNSEmailPlatformEvent

doc {
	title "ValidateSNSEmailPlatform"

	category "sns"

	desc """验证SNS邮件平台"""

	rest {
		request {
			url "PUT /v1/sns/application-platforms/email/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIValidateSNSEmailPlatformMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "validateSNSEmailPlatform"
					desc "资源的UUID，唯一标示该资源"
					location "url"
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
					name "smtpServer"
					enclosedIn "validateSNSEmailPlatform"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "smtpPort"
					enclosedIn "validateSNSEmailPlatform"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "4.10.0"
				}
				column {
					name "username"
					enclosedIn "validateSNSEmailPlatform"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "password"
					enclosedIn "validateSNSEmailPlatform"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIValidateSNSEmailPlatformEvent.class
		}
	}
}