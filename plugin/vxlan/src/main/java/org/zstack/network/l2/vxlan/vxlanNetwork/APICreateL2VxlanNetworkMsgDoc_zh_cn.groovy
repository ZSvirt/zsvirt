package org.zstack.network.l2.vxlan.vxlanNetwork

import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkEvent

doc {
	title "创建二层VXLAN网络(CreateL2VxlanNetwork)"

	category "network.l2"

	desc """创建二层VXLAN网络"""

	rest {
		request {
			url "POST /v1/l2-networks/vxlan"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateL2VxlanNetworkMsg.class

			desc """"""

			params {

				column {
					name "vni"
					enclosedIn "params"
					desc "Vni号"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "poolUuid"
					enclosedIn "params"
					desc "VXLAN资源池UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "physicalInterface"
					enclosedIn "params"
					desc "物理网卡"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "二层网络类型"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，二层网络会使用该字段值作为UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "vSwitchType"
					enclosedIn "params"
					desc "虚拟交换机类型"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("LinuxBridge","OvsDpdk","MacVlan")
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateL2VxlanNetworkEvent.class
		}
	}
}