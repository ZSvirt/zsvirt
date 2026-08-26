package org.zstack.log.server

import org.zstack.log.server.APIAddLogServerEvent

doc {
	title "AddLogServer"

	category "logServer"

	desc """添加日志服务器"""

	rest {
		request {
			url "POST /v1/log/servers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddLogServerMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "category"
					enclosedIn "params"
					desc "日志类别(管理节点日志、平台操作日志)"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv 5.1.0"
					values ("ManagementNodeLog","PlatformOperationLog")
				}
				column {
					name "type"
					enclosedIn "params"
					desc "日志类型"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv 5.1.0"
					values ("Log4j2","FluentBit")
				}
				column {
					name "level"
					enclosedIn "params"
					desc "级别"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv 5.1.0"
					values ("OFF","FATAL","ERROR","WARN","INFO","DEBUG","TRACE","ALL")
				}
				column {
					name "configuration"
					enclosedIn "params"
					desc "日志具体配置"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
			}
		}

		response {
			clz APIAddLogServerEvent.class
		}
	}
}