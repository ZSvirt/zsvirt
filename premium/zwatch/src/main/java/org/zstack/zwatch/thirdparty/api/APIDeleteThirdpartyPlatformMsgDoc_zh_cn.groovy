package org.zstack.zwatch.thirdparty.api

import org.zstack.zwatch.thirdparty.api.APIDeleteThirdpartyPlatformEvent

doc {
	title "DeleteThirdpartyPlatform"

	category "zwatch"

	desc """删除第三方报警源"""

	rest {
		request {
			url "DELETE /v1/zwatch/third-party/platforms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteThirdpartyPlatformMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "平台UUID"
					location "url"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteThirdpartyPlatformEvent.class
		}
	}
}