package org.zstack.simulator2.config

import org.zstack.core.Platform
import org.zstack.simulator2.api.CreateResourceCmd
import org.zstack.simulator2.config.host.KvmHost
import org.zstack.simulator2.config.primaryStorage.CephPrimaryStorage
import org.zstack.simulator2.config.primaryStorage.NfsPrimaryStorage
import org.zstack.utils.gson.JSONObjectUtil

import java.lang.reflect.Modifier

/**
 * Created by xing5 on 2017/9/15.
 */
class Configuration implements JsonNode {
    List<NfsPrimaryStorage> nfsPrimaryStorage = []
    List<CephPrimaryStorage> cephPrimaryStorage = []
    List<KvmHost> kvmHosts = []

    transient Map<String, List> stubResourceReference = [:]
    transient static Map<String, Class> resourceMap = [:]

    static {
        def classes = Platform.reflections.getSubTypesOf(VO.class).findAll {
            return Modifier.isAbstract(it.modifiers) ? null : it
        }

        classes.each { resourceMap[it.simpleName] = it }
    }

    Configuration() {
        stubResourceReference[NfsPrimaryStorage.class.simpleName] = nfsPrimaryStorage
        stubResourceReference[CephPrimaryStorage.class.simpleName] = cephPrimaryStorage
        stubResourceReference[KvmHost.class.simpleName] = kvmHosts
    }

    void createResource(CreateResourceCmd cmd) {
        def clz = resourceMap[cmd.type]
        assert clz != null : "cannot find resource with type[${cmd.type}"

        def res = JSONObjectUtil.rehashObject(cmd.data, clz)

        if (cmd.parentId == null) {
            List stubResources = stubResourceReference.get(cmd.type)
            assert stubResources != null : "cannot find stub resource with the type[${cmd.type}]"

            stubResources.add(res)
            children.add(res)
            return
        }

        JsonNode p = find { it.id == cmd.parentId }
        assert p != null : "cannot find parent with the id[${cmd.parentId}]"

        p.add(res)
    }
}
