package org.zstack.header.bonding

import org.zstack.header.bonding.APICreateBondingEvent

doc {
	title "CreateBonding"

	category "bonding"

	desc """创建Bond网口"""

	rest {
		request {
			url "POST /v1/hosts/bondings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateBondingMsg.class

			desc """"""

			params {

				column {
					name "hostUuids"
					enclosedIn "params"
					desc "主机UUID列表"
					location "body"
					type "List"
					optional false
					since "3.17.0"
				}
				column {
					name "bondingName"
					enclosedIn "params"
					desc "Bond名称"
					location "body"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "slaveUuids"
					enclosedIn "params"
					desc "slave网卡UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "slaveNames"
					enclosedIn "params"
					desc "slave网卡名称列表"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "Bond类型"
					location "body"
					type "String"
					optional false
					since "3.17.0"
					values ("LinuxBonding","OvsBonding")
				}
				column {
					name "mode"
					enclosedIn "params"
					desc "工作方式"
					location "body"
					type "String"
					optional false
					since "3.17.0"
					values ("802.3ad","active-backup")
				}
				column {
					name "xmitHashPolicy"
					enclosedIn "params"
					desc "哈希算法策略"
					location "body"
					type "String"
					optional true
					since "3.17.0"
					values ("layer2","layer2+3","layer3+4")
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APICreateBondingEvent.class
		}
	}
}