package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmBootModeEvent

doc {
	title "DeleteVmBootMode"

	category "vmInstance"

	desc """删除云主机启动模式"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{uuid}/bootmode"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmBootModeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
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
			clz APIDeleteVmBootModeEvent.class
		}
	}
}