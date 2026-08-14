package org.zstack.header.baremetal.network

import org.zstack.header.baremetal.network.APICreateBaremetalBondingEvent

doc {
	title "CreateBaremetalBonding"

	category "baremetal.network"

	desc """创建裸金属网卡绑定"""

	rest {
		request {
			url "POST /v1/baremetal/network/bondings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateBaremetalBondingMsg.class

			desc """创建裸金属网卡绑定"""

			params {

				column {
					name "chassisUuid"
					enclosedIn "params"
					desc "裸金属设备UUID"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "网卡绑定名称"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "mode"
					enclosedIn "params"
					desc "网卡绑定模式"
					location "body"
					type "Integer"
					optional false
					since "3.4.0"
				}
				column {
					name "slaves"
					enclosedIn "params"
					desc "网卡绑定Slaves MAC地址"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "opts"
					enclosedIn "params"
					desc "网卡绑定选项"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
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
			clz APICreateBaremetalBondingEvent.class
		}
	}
}