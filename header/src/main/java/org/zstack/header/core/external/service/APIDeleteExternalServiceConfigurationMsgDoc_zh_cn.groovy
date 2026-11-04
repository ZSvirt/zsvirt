package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIDeleteExternalServiceConfigurationEvent

doc {
	title "删除外部服务配置"

	category "externalService"

	desc """删除外部服务配置"""

	rest {
		request {
			url "DELETE /v1/external/service/configurations/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteExternalServiceConfigurationMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIDeleteExternalServiceConfigurationEvent.class
		}
	}
}