package org.zstack.header.host

import org.zstack.header.host.APIGetInterfaceServiceTypeStatisticReply

doc {
	title "GetInterfaceServiceTypeStatistic"

	category "host"

	desc """获取物理机网卡服务情况统计"""

	rest {
		request {
			url "GET /v1/hosts/hosts-network-interfaces/service-type-statistic"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetInterfaceServiceTypeStatisticMsg.class

			desc """"""

			params {

				column {
					name "interfaceUuid"
					enclosedIn ""
					desc "物理机网卡UUID"
					location "query"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "vlanId"
					enclosedIn ""
					desc "vlan接口ID"
					location "query"
					type "Integer"
					optional true
					since "3.17.11"
				}
				column {
					name "interfaceType"
					enclosedIn ""
					desc "网卡类型"
					location "query"
					type "String"
					optional true
					since "3.17.11"
					values ("All","Interface","Bonding")
				}
				column {
					name "serviceType"
					enclosedIn ""
					desc "统计网络服务类型"
					location "query"
					type "List"
					optional true
					since "3.17.11"
					values ("All","ManagementNetwork","TenantNetwork","StorageNetwork","MigrationNetwork","BackupNetwork")
				}
				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "query"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "sortBy"
					enclosedIn ""
					desc "排序方式"
					location "query"
					type "String"
					optional true
					since "3.17.11"
					values ("InterfaceName","VlanId","HostIp","HostName","ClusterName","CreateDate")
				}
				column {
					name "sortDirection"
					enclosedIn ""
					desc "排序方向"
					location "query"
					type "String"
					optional true
					since "3.17.11"
					values ("asc","desc")
				}
				column {
					name "start"
					enclosedIn ""
					desc "统计结果起始位置"
					location "query"
					type "Integer"
					optional true
					since "3.17.11"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "统计结果数量"
					location "query"
					type "Integer"
					optional true
					since "3.17.11"
				}
				column {
					name "replyWithCount"
					enclosedIn ""
					desc "同时返回统计结果总数"
					location "query"
					type "boolean"
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
			clz APIGetInterfaceServiceTypeStatisticReply.class
		}
	}
}