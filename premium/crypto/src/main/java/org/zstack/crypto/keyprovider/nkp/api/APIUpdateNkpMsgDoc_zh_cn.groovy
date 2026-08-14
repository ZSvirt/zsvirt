package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.crypto.keyprovider.nkp.api.APIUpdateNkpEvent

doc {
	title "UpdateNkp"

	category "keyProvider"

	desc """更新原生密钥提供程序"""

	rest {
		request {
			url "PUT /v1/key-providers/nkp/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateNkpMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateNkp"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "description"
					enclosedIn "updateNkp"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APIUpdateNkpEvent.class
		}
	}
}