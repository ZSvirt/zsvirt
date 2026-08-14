package org.zstack.header.network.l3

import org.zstack.header.network.l3.APICreateL3NetworkEvent

doc {
	title "创建三层网络(CreateL3Network)"

	category "三层网络"

	desc """创建三层网络"""

	rest {
		request {
			url "POST /v1/l3-networks"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "三层网络的名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "三层网络的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "三层网络类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "l2NetworkUuid"
					enclosedIn "params"
					desc "二层网络UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "ipVersion"
					enclosedIn "params"
					desc "ip协议号"
					location "body"
					type "Integer"
					optional true
					since "zsv 3.1"
					values ("4","6")
				}
				column {
					name "system"
					enclosedIn "params"
					desc "是否用于系统云主机"
					location "body"
					type "boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "dnsDomain"
					enclosedIn "params"
					desc "DNS域"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，三层网络会使用该字段值作为UUID"
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
					name "category"
					enclosedIn "params"
					desc "网络类型，需要与system标签搭配使用，system为true时可设置为Public、Private"
					location "body"
					type "String"
					optional true
					since "zsv 2.2"
					values ("Public","Private","System")
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
				column {
					name "enableIPAM"
					enclosedIn "params"
					desc "IP地址管理是否启用"
					location "body"
					type "Boolean"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APICreateL3NetworkEvent.class
		}
	}
}