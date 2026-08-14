package org.zstack.zmigrate.api

import org.zstack.zmigrate.api.APIGetZMigrateInfosReply

doc {
	title "GetZMigrateInfos"

	category "ZMigratePlugin"

	desc """获取ZMigrate管理节点信息"""

	rest {
		request {
			url "GET /v1/zmigrate/management/infos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetZMigrateInfosMsg.class

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
			clz APIGetZMigrateInfosReply.class
		}
	}
}