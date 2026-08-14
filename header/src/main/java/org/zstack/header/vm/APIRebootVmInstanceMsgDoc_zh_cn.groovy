package org.zstack.header.vm

import org.zstack.header.vm.APIRebootVmInstanceEvent

doc {
	title "重启云主机(RebootVmInstance)"

	category "vmInstance"

	desc """重启一个云主机"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRebootVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "rebootVmInstance"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIRebootVmInstanceEvent.class
		}
	}
}