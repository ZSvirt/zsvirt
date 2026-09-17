package org.zstack.ha

import org.zstack.ha.APIDeleteVmInstanceHaLevelEvent

doc {
	title "DeleteVmInstanceHaLevel"

	category "ha"

	desc """删除高可用是否成功"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{uuid}/ha-levels"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmInstanceHaLevelMsg.class

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
			clz APIDeleteVmInstanceHaLevelEvent.class
		}
	}
}