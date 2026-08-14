package org.zstack.vrouterRoute

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.vrouterRoute.VirtualRouterVRouterRouteTableRefInventory
import org.zstack.vrouterRoute.VRouterRouteEntryInventory

doc {

	title "云路由路由表清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.1"
	}
	field {
		name "name"
		desc "资源名称"
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
	ref {
		name "attachedRouterRefs"
		path "org.zstack.vrouterRoute.VRouterRouteTableInventory.attachedRouterRefs"
		desc "相关联的云路由设备"
		type "List"
		since "2.1"
		clz VirtualRouterVRouterRouteTableRefInventory.class
	}
	ref {
		name "routeEntries"
		path "org.zstack.vrouterRoute.VRouterRouteTableInventory.routeEntries"
		desc "相关联的云路由条目"
		type "List"
		since "2.1"
		clz VRouterRouteEntryInventory.class
	}
}
