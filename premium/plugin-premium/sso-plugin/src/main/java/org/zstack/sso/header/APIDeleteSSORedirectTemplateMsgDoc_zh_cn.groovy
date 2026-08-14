package org.zstack.sso.header

import org.zstack.sso.header.APIDeleteSSORedirectTemplateEvent

doc {
	title "DeleteSSORedirectTemplate"

	category "sso"

	desc """删除认证跳转模板"""

	rest {
		request {
			url "POST /v1/delete/sso/redirect/template"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteSSORedirectTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "模板的 UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "deleteMode"
					enclosedIn "params"
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
			}
		}

		response {
			clz APIDeleteSSORedirectTemplateEvent.class
		}
	}
}