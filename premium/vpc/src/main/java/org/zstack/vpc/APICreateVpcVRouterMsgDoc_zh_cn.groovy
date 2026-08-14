package org.zstack.vpc

import org.zstack.vpc.APICreateVpcVRouterEvent

doc {
	title "CreateVpcVRouter"

	category "vpc"

	desc """创建VPC云路由"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVpcVRouterMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.2.2"
				}
				column {
					name "virtualRouterOfferingUuid"
					enclosedIn "params"
					desc "云路由规格"
					location "body"
					type "String"
					optional false
					since "2.2.2"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.2.2"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源的UUID"
					location "body"
					type "String"
					optional true
					since "2.2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.2.2"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "params"
					desc "主存储UUID。若指定，路由器的根云盘会在指定主存储创建。"
					location "body"
					type "String"
					optional true
					since "3.14.12"
				}
				column {
					name "rootVolumeSystemTags"
					enclosedIn "params"
					desc "路由器根盘所需要的系统标签"
					location "body"
					type "List"
					optional true
					since "3.14.12"
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
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc "网卡信息"
					location "body"
					type "String"
					optional true
					since "4.2.0"
				}
			}
		}

		response {
			clz APICreateVpcVRouterEvent.class
		}
	}
}