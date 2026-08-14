package org.zstack.header.host

import org.zstack.header.host.APISetServiceTypeOnHostNetworkBondingEvent

doc {
	title "SetServiceTypeOnHostNetworkBonding"

	category "host"

	desc """在bond网口配置网络服务类型"""

	rest {
		request {
			url "POST /v1/hosts/bondings/service-types"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetServiceTypeOnHostNetworkBondingMsg.class

			desc """"""

			params {

				column {
					name "bondingUuids"
					enclosedIn "params"
					desc "bond网口Uuids"
					location "body"
					type "List"
					optional false
					since "3.17.11"
				}
				column {
					name "vlanIds"
					enclosedIn "params"
					desc "vlan接口Ids"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "serviceTypes"
					enclosedIn "params"
					desc "网络服务类型"
					location "body"
					type "List"
					optional true
					since "3.17.11"
					values ("ManagementNetwork","TenantNetwork","StorageNetwork","BackupNetwork","MigrationNetwork")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APISetServiceTypeOnHostNetworkBondingEvent.class
		}
	}
}