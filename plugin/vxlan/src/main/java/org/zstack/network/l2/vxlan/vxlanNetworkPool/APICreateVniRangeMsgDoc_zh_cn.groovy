package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVniRangeEvent

doc {
	title "创建VNI范围(CreateVniRange)d"

	category "network.l2"

	desc """创建VNI范围"""

	rest {
		request {
			url "POST /v1/l2-networks/vxlan-pool/{l2NetworkUuid}/vni-ranges"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVniRangeMsg.class

			desc """"""

			params {

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
					name "startVni"
					enclosedIn "params"
					desc "起始VNI"
					location "body"
					type "Integer"
					optional false
					since "0.6"
				}
				column {
					name "endVni"
					enclosedIn "params"
					desc "结束VNI"
					location "body"
					type "Integer"
					optional false
					since "0.6"
				}
				column {
					name "l2NetworkUuid"
					enclosedIn "params"
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
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
			clz APICreateVniRangeEvent.class
		}
	}
}