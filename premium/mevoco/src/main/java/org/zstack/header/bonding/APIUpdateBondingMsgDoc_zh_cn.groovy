package org.zstack.header.bonding

import org.zstack.header.bonding.APIUpdateBondingEvent

doc {
	title "UpdateBonding"

	category "bonding"

	desc """更新Bond网口"""

	rest {
		request {
			url "PUT /v1/hosts/bondings/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateBondingMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateBonding"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "slaveUuids"
					enclosedIn "updateBonding"
					desc "slave网卡UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "slaveNames"
					enclosedIn "updateBonding"
					desc "slave网卡名称列表"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "type"
					enclosedIn "updateBonding"
					desc "Bond类型"
					location "body"
					type "String"
					optional true
					since "3.17.0"
					values ("LinuxBonding","OvsBonding")
				}
				column {
					name "mode"
					enclosedIn "updateBonding"
					desc "工作方式"
					location "body"
					type "String"
					optional true
					since "3.17.0"
					values ("802.3ad","active-backup")
				}
				column {
					name "xmitHashPolicy"
					enclosedIn "updateBonding"
					desc "哈希算法策略"
					location "body"
					type "String"
					optional true
					since "3.17.0"
					values ("layer2","layer2+3","layer3+4")
				}
				column {
					name "description"
					enclosedIn "updateBonding"
					desc "资源的详细描述"
					location "body"
					type "String"
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
			clz APIUpdateBondingEvent.class
		}
	}
}