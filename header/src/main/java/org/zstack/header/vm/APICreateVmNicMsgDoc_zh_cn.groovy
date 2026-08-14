package org.zstack.header.vm

import org.zstack.header.vm.APICreateVmNicEvent

doc {
	title "CreateVmNic"

	category "vmInstance"

	desc """创建云主机网卡"""

	rest {
		request {
			url "POST /v1/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmNicMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "body"
					type "String"
					optional false
					since "zsv 3.10"
				}
				column {
					name "ip"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.10"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.10"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
			}
		}

		response {
			clz APICreateVmNicEvent.class
		}
	}
}