package org.zstack.header.host

import org.zstack.header.host.APIGetCandidateInterfaceVlanIdsReply

doc {
	title "GetCandidateInterfaceVlanIds"

	category "host"

	desc """获取网卡交集子接口信息"""

	rest {
		request {
			url "GET /v1/host/network-interface-vlan-ids"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateInterfaceVlanIdsMsg.class

			desc """"""

			params {

				column {
					name "interfaceUuids"
					enclosedIn ""
					desc "网口UUIDs"
					location "query"
					type "List"
					optional false
					since "3.17.11"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.17.11"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIGetCandidateInterfaceVlanIdsReply.class
		}
	}
}