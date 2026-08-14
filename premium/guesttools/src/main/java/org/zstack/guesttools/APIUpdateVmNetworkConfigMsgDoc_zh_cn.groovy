package org.zstack.guesttools

import org.zstack.guesttools.APIUpdateVmNetworkConfigEvent

doc {
	title "UpdateVmNetworkConfig"

	category "guest.tools"

	desc """同步云主机网络配置应答"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/update-nic-config"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmNetworkConfigMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "updateVmNetworkConfig"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.16.11"
				}
				column {
					name "vmNicUuids"
					enclosedIn "updateVmNetworkConfig"
					desc ""
					location "body"
					type "List"
					optional false
					since "3.16.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16.11"
				}
			}
		}

		response {
			clz APIUpdateVmNetworkConfigEvent.class
		}
	}
}