package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmHostnameEvent

doc {
	title "删除虚拟机Hostname(DeleteVmHostname)"

	category "vmInstance"

	desc """删除虚拟机的Hostname。注意，该删除操作仅仅是删除配置在数据库中以及DHCP服务器上的虚拟机hostname，无法改变虚拟机内部手动配置的hostname。"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{uuid}/hostnames"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmHostnameMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv_0.6"
				}
			}
		}

		response {
			clz APIDeleteVmHostnameEvent.class
		}
	}
}