package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmNicEvent

doc {
	title "DeleteVmNic"

	category "vmInstance"

	desc """删除云主机网卡"""

	rest {
		request {
			url "DELETE /v1/nics/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmNicMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.10"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv 3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.10"
				}
			}
		}

		response {
			clz APIDeleteVmNicEvent.class
		}
	}
}