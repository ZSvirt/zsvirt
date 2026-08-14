package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmNicAttachedNetworkServiceReply

doc {
	title "获取网卡加载的网络服务名称(GetVmNicAttachedNetworkService)"

	category "vmInstance"

	desc """获取一个网卡已加载的网络服务名称"""

	rest {
		request {
			url "GET /v1/vm-instances/nics/{vmNicUuid}/attached-networkservices"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmNicAttachedNetworkServiceMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.11.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
			}
		}

		response {
			clz APIGetVmNicAttachedNetworkServiceReply.class
		}
	}
}