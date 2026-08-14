package org.zstack.sso.header

import org.zstack.sso.header.APIUpdateSSORedirectTemplateEvent

doc {
	title "UpdateSSORedirectTemplate"

	category "sso"

	desc """修改跳转模版"""

	rest {
		request {
			url "POST /v1/update/sso/redirectTemplate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSSORedirectTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "模版的 UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "redirectTemplate"
					enclosedIn "params"
					desc "跳转的模板"
					location "body"
					type "String"
					optional false
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
			clz APIUpdateSSORedirectTemplateEvent.class
		}
	}
}