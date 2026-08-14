package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIGetExternalServicesReply

doc {
	title "GetExternalServices"

	category "externalService"

	desc """获取External Services"""

	rest {
		request {
			url "GET /v1/external/services"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetExternalServicesMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIGetExternalServicesReply.class
		}
	}
}