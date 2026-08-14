package org.zstack.zsv.storage.api

import org.zstack.zsv.storage.api.APICheckCephPluginReply

doc {
	title "CheckCephPlugin"

	category "ZsvStorage"

	desc """检查环境中的 Ceph 插件"""

	rest {
		request {
			url "PUT /v1/ceph-plugin/check"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckCephPluginMsg.class

			desc """该 API 用于检查指定管理节点及其关联主机上安装的所有 Ceph 环境。它支持扫描 ZStone 或 ZCE-X Ceph 插件，并返回检测结果。可以通过 managementNode 参数指定是否检查所有管理节点，或通过 hostUuidList 或 ipList 参数提供需要检查的特定主机列表。"""

			params {

				column {
					name "managementNode"
					enclosedIn "checkCephPlugin"
					desc "是否对所有管理节点进行检查"
					location "body"
					type "boolean"
					optional true
					since "4.10.7"
				}
				column {
					name "hostUuidList"
					enclosedIn "checkCephPlugin"
					desc "增加检查 host 的信息列表"
					location "body"
					type "List"
					optional true
					since "4.10.7"
				}
				column {
					name "ipList"
					enclosedIn "checkCephPlugin"
					desc "增加检查指定 IP 对应的主机的信息列表"
					location "body"
					type "List"
					optional true
					since "4.10.7"
				}
				column {
					name "externalHosts"
					enclosedIn "checkCephPlugin"
					desc "指定用于检查的外部主机 SSH 连接参数列表"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.7"
				}
			}
		}

		response {
			clz APICheckCephPluginReply.class
		}
	}
}