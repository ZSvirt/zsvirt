package org.zstack.ha

import org.zstack.ha.APIGetVmInstanceHaLevelReply

doc {
	title "GetVmInstanceHaLevel"

	category "ha"

	desc """获取云主机高可用级别"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/ha-levels"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmInstanceHaLevelMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机资源Uuid"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVmInstanceHaLevelReply.class
		}
	}
}