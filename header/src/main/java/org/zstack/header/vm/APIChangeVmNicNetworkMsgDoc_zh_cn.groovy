package org.zstack.header.vm

import org.zstack.header.vm.APIChangeVmNicNetworkEvent

doc {
	title "改变网卡的L3网络"

	category "vmInstance"

	desc """改变Stopped的云主机的网卡的L3网络"""

	rest {
		request {
			url "POST /v1/vm-instances/nics/{vmNicUuid}/l3-networks/{destL3NetworkUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVmNicNetworkMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn "params"
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.11.0"
				}
				column {
					name "destL3NetworkUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "zsv 3.11.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "staticIp"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc "网卡信息"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APIChangeVmNicNetworkEvent.class
		}
	}
}