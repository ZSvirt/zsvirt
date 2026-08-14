package org.zstack.zmigrate.api

import org.zstack.zmigrate.api.APIGetZMigrateImagesReply

doc {
	title "GetZMigrateImages"

	category "ZMigratePlugin"

	desc """获取ZMigrate网关镜像"""

	rest {
		request {
			url "GET /v1/zmigrate/images"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetZMigrateImagesMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APIGetZMigrateImagesReply.class
		}
	}
}