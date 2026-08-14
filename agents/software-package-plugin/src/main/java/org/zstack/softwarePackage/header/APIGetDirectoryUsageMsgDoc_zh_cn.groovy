package org.zstack.softwarePackage.header

doc {
	title "GetDirectoryUsage"

	category "softwarePackage"

	desc """获取目录容量信息"""

	rest {
		request {
			url "GET /v1/software-package/directory/usage"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetDirectoryUsageMsg.class

			desc """"""

			params {

				column {
					name "managementNodeUuid"
					enclosedIn ""
					desc "管理节点UUID"
					location "query"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "directoryPath"
					enclosedIn ""
					desc "目录绝对路径"
					location "query"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIGetDirectoryUsageReply.class
		}
	}
}