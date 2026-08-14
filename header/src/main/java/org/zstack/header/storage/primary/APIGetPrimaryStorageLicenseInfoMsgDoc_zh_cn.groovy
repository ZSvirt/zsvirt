package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIGetPrimaryStorageLicenseInfoReply

doc {
	title "获取主存储的License信息(GetPrimaryStorageLicenseInfo)"

	category "storage.primary"

	desc """获取主存储的License信息"""

	rest {
		request {
			url "GET /v1/primary-storage/{uuid}/license"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPrimaryStorageLicenseInfoMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetPrimaryStorageLicenseInfoReply.class
		}
	}
}