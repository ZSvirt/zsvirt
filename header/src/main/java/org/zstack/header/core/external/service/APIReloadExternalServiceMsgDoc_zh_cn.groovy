package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIReloadExternalServiceEvent

doc {
	title "ReloadExternalService"

	category "externalService"

	desc """重新加载External Service"""

	rest {
		request {
			url "PUT /v1/external/services"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReloadExternalServiceMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "reloadExternalService"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIReloadExternalServiceEvent.class
		}
	}
}