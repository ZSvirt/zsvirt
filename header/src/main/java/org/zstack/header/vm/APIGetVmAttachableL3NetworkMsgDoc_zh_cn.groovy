package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmAttachableL3NetworkReply

doc {
	title "获取云主机可加载三层网络列表(GetVmAttachableL3Network)"

	category "vmInstance"

	desc """获取一个云主机可加载网络三层网络列表"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/l3-networks-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmAttachableL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
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
			clz APIGetVmAttachableL3NetworkReply.class
		}
	}
}