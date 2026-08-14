package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmConsolePasswordEvent

doc {
	title "删除云主机控制台密码(DeleteVmConsolePassword)"

	category "vmInstance"

	desc """删除一个云主机的控制台密码"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{uuid}/console-password"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmConsolePasswordMsg.class

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
			clz APIDeleteVmConsolePasswordEvent.class
		}
	}
}