package org.zstack.monitoring.media

import org.zstack.monitoring.media.APIUpdateEmailMediaEvent

doc {
	title "UpdateEmailMedia"

	category "未知类别"

	desc """修改Email媒体"""

	rest {
		request {
			url "PUT /v1/media/emails/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateEmailMediaMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateEmailMedia"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "smtpServer"
					enclosedIn "updateEmailMedia"
					desc "smtp服务器地址"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "smtpPort"
					enclosedIn "updateEmailMedia"
					desc "smtp服务器端口"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "username"
					enclosedIn "updateEmailMedia"
					desc "smtp用户名"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "updateEmailMedia"
					desc "smtp密码"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateEmailMedia"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateEmailMedia"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateEmailMediaEvent.class
		}
	}
}