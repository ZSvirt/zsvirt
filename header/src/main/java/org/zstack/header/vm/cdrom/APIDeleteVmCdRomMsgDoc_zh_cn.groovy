package org.zstack.header.vm.cdrom

import org.zstack.header.vm.cdrom.APIDeleteVmCdRomEvent

doc {
	title "DeleteVmCdRom"

	category "vmInstance"

	desc """删除CDROM"""

	rest {
		request {
			url "DELETE /v1/vm-instances/cdroms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmCdRomMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.3"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv 3.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.3"
				}
			}
		}

		response {
			clz APIDeleteVmCdRomEvent.class
		}
	}
}