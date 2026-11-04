package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmStaticIpEvent

doc {
	title "删除虚拟机指定IP(DeleteVmStaticIp)"

	category "vmInstance"

	desc """删除虚拟机三层网络上指定的IP"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{vmInstanceUuid}/static-ips"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmStaticIpMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_0.6"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "query"
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
				column {
					name "staticIp"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv_3.10.0"
				}
			}
		}

		response {
			clz APIDeleteVmStaticIpEvent.class
		}
	}
}