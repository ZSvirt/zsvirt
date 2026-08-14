package org.zstack.header.vm

import org.zstack.header.vm.APIGetCandidateL3NetworksForChangeVmNicNetworkReply

doc {
	title "获取云主机网卡可以加载的L3网络"

	category "vmInstance"

	desc """获取云主机网卡可以加载的L3网络列表"""

	rest {
		request {
			url "GET /v1/vm-instances/nics/{vmNicUuid}/l3-networks-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateL3NetworksForChangeVmNicNetworkMsg.class

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
			clz APIGetCandidateL3NetworksForChangeVmNicNetworkReply.class
		}
	}
}