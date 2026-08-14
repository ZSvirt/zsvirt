package org.zstack.vrouterRoute

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "云路由路由表条目清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.1"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.1"
	}
	field {
		name "type"
		desc "类型，允许用户添加\"静态路由\"、\"黑洞路由\"两种类型，系统会根据是否填下一条自动判断类型，" +
				"在外在获取实时路由表API还会返回\"ZStack\"类型表示由系统添加的路由，\"DirectConnect\"直连路由"
		type "String"
		since "2.1"
	}
	field {
		name "routeTableUuid"
		desc "云路由路由表UUID"
		type "String"
		since "2.1"
	}
	field {
		name "destination"
		desc "目标网络地址"
		type "String"
		since "2.1"
	}
	field {
		name "target"
		desc "下一条地址"
		type "String"
		since "2.1"
	}
	field {
		name "distance"
		desc "路由优先级，在最小匹配下如果有多条路由规则匹配，优先级数字小的规则将会被匹配"
		type "Integer"
		since "2.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.1"
	}
}
