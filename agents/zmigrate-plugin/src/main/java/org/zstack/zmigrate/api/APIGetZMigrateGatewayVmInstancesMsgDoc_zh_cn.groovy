package org.zstack.zmigrate.api

import org.zstack.zmigrate.api.APIGetZMigrateGatewayVmInstancesReply

doc {
	title "GetZMigrateGatewayVmInstances"

	category "ZMigratePlugin"

	desc """获取ZMigrate网关虚拟机实例"""

	rest {
		request {
			url "GET /v1/zmigrate/vm-instances"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetZMigrateGatewayVmInstancesMsg.class

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
			clz APIGetZMigrateGatewayVmInstancesReply.class
		}
	}
}