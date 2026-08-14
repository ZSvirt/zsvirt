package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmSshKeyEvent

doc {
	title "删除云主机SSH Key(DeleteVmSshKey)"

	category "vmInstance"

	desc """删除云主机SSH Key，该Key是之前通过SetVmSshKey设置的。"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{uuid}/ssh-keys"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmSshKeyMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional true
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
			clz APIDeleteVmSshKeyEvent.class
		}
	}
}