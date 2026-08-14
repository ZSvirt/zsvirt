package org.zstack.header.host

import org.zstack.header.host.APIGetPhysicalMachineBlockDevicesReply

doc {
	title "GetPhysicalMachineBlockDevices"

	category "host"

	desc """获取服务器磁盘信息"""

	rest {
		request {
			url "GET /v1/host/get-block-devices"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPhysicalMachineBlockDevicesMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn ""
					desc "主机名"
					location "query"
					type "String"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "password"
					enclosedIn ""
					desc "主机密码"
					location "query"
					type "String"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "sshPort"
					enclosedIn ""
					desc "主机端口"
					location "query"
					type "Integer"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "hostName"
					enclosedIn ""
					desc "主机IP"
					location "query"
					type "String"
					optional false
					since "zsv 4.3.0"
				}
				column {
					name "excludedTypes"
					enclosedIn ""
					desc "指定排除的磁盘类型"
					location "query"
					type "List"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.3.0"
				}
			}
		}

		response {
			clz APIGetPhysicalMachineBlockDevicesReply.class
		}
	}
}