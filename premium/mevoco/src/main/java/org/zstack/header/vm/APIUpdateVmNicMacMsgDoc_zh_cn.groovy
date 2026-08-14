package org.zstack.header.vm

import org.zstack.header.vm.APIUpdateVmNicMacEvent

doc {
	title "UpdateVmNicMac"

	category "mevoco"

	desc """更新云主机mac地址"""

	rest {
		request {
			url "PUT /v1/vm-instances/nics/{vmNicUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmNicMacMsg.class

			desc """更新云主机mac地址"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn "updateVmNicMac"
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "mac"
					enclosedIn "updateVmNicMac"
					desc "mac地址"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateVmNicMacEvent.class
		}
	}
}