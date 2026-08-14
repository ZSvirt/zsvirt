package org.zstack.header.identity

import org.zstack.header.identity.APIDeleteAccountEvent

doc {
	title "DeleteAccount"

	category "identity"

	desc """删除账户"""

	rest {
		request {
			url "DELETE /v1/accounts/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAccountMsg.class

			desc """删除账户"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteAccountEvent.class
		}
	}
}