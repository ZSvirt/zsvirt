package org.zstack.header.network.l2

import org.zstack.header.network.l2.APICreateL2NetworkEvent

doc {
	title "创建普通二层网络(CreateL2NoVlanNetwork)"

	category "二层网络"

	desc """创建普通二层网络"""

	rest {
		request {
			url "POST /v1/l2-networks/no-vlan"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateL2NoVlanNetworkMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "普通二层网络名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "普通二层网络的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "physicalInterface"
					enclosedIn "params"
					desc "物理网卡"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "二层网络类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "vSwitchType"
					enclosedIn "params"
					desc "虚拟交换机类型"
					location "body"
					type "String"
					optional true
					since "zsv 3.11.0"
					values ("LinuxBridge","OvsDpdk","MacVlan")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，二层网络会使用该字段值作为UUID"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
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
			clz APICreateL2NetworkEvent.class
		}
	}
}