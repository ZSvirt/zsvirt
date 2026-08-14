package org.zstack.header.vm.cdrom

import org.zstack.header.vm.cdrom.APIUpdateVmCdRomEvent

doc {
	title "UpdateVmCdRom"

	category "vmInstance"

	desc """修改CDROM"""

	rest {
		request {
			url "PUT /v1/vm-instances/cdroms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmCdRomMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVmCdRom"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.3"
				}
				column {
					name "description"
					enclosedIn "updateVmCdRom"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.3"
				}
				column {
					name "name"
					enclosedIn "updateVmCdRom"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 3.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
			}
		}

		response {
			clz APIUpdateVmCdRomEvent.class
		}
	}
}