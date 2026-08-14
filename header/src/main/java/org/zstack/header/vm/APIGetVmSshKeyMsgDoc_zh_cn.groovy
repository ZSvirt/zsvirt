package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmSshKeyReply

doc {
	title "获取云主机SSH KEY(GetVmSshKey)"

	category "vmInstance"

	desc """获取一个云主机的SSH Key，该SSH key是通过SetVmSshKey设置的。"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/ssh-keys"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmSshKeyMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetVmSshKeyReply.class
		}
	}
}