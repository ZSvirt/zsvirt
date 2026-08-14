package org.zstack.header.vm

import org.zstack.header.vm.APIDetachL3NetworkFromVmEvent

doc {
	title "从云主机卸载网络(DetachL3NetworkFromVm)"

	category "vmInstance"

	desc """从Running或Stopped的云主机上卸载一个网络"""

	rest {
		request {
			url "DELETE /v1/vm-instances/nics/{vmNicUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachL3NetworkFromVmMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID，该网卡所在网络会从云主机卸载掉"
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
			clz APIDetachL3NetworkFromVmEvent.class
		}
	}
}