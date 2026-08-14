package org.zstack.test.integration.core.async

import org.zstack.core.cascade.AbstractAsyncCascadeExtension
import org.zstack.core.cascade.CascadeAction
import org.zstack.core.cascade.CascadeExtensionPoint
import org.zstack.core.cascade.CascadeFacade
import org.zstack.core.cascade.CascadeFacadeImpl
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Created by heathhose on 17-3-24.
 */
class AsyncCascadeCase extends SubCase{

    CascadeFacade casf 
    boolean success1 
    boolean success2 
    boolean success3 
    boolean success4 
    boolean success5 

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    private void bootstrap(Map<String, CascadeExtensionPoint> exts) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = CascadeFacadeImpl.class.getDeclaredMethod("populateCascadeNodes", Map.class) 
        method.setAccessible(true) 
        method.invoke(casf, exts) 

        method = CascadeFacadeImpl.class.getDeclaredMethod("populateTree") 
        method.setAccessible(true) 
        method.invoke(casf) 
    }

    @Override
    void test() {
        success1 = false
        success2 = false
        success3 = false
        success4 = false
        success5 = false

        casf = bean(CascadeFacade.class)
        asyncCascade()
    }

    void asyncCascade(){
        Map<String, CascadeExtensionPoint> map = new HashMap<String, CascadeExtensionPoint>() 
        map.put("zone", new AbstractAsyncCascadeExtension() {

            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                success1 = true 
                completion.success() 
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList() 
            }

            @Override
            public String getCascadeResourceName() {
                return "zone" 
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("zone") 
            }
        }) 

        map.put("cluster", new AbstractAsyncCascadeExtension() {

            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (Arrays.asList("zone", "primaryStorage").contains(action.getParentIssuer())) {
                    success2 = true 
                }
                completion.success() 
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone", "primaryStorage") 
            }

            @Override
            public String getCascadeResourceName() {
                return "cluster" 
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                if (action.getParentIssuer().equals("zone")) {
                    return action.copy().setParentIssuer("cluster") 
                }

                return null 
            }
        }) 

        map.put("primaryStorage", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (Arrays.asList("zone", "cluster").contains(action.getParentIssuer())) {
                    success3 = true 
                }
                completion.success() 
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("zone", "cluster") 
            }

            @Override
            public String getCascadeResourceName() {
                return "primaryStorage" 
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                if (action.getParentIssuer().equals("zone")) {
                    return action.copy().setParentIssuer("primaryStorage") 
                }

                return null 
            }
        }) 

        map.put("host", new AbstractAsyncCascadeExtension() {
            @Override
            public void asyncCascade(CascadeAction action, Completion completion) {
                if (action.getParentIssuer().equals("cluster")) {
                    success4 = (!success4 ? true : false) 
                }
                completion.success() 
            }

            @Override
            public List<String> getEdgeNames() {
                return Arrays.asList("cluster") 
            }

            @Override
            public String getCascadeResourceName() {
                return "host" 
            }

            @Override
            public CascadeAction createActionForChildResource(CascadeAction action) {
                return action.copy().setParentIssuer("host") 
            }
        }) 

        bootstrap(map) 
        casf.asyncCascade("test", "zone", null, new Completion(null) {
            @Override
            public void success() {
                success5 = true 
            }

            @Override
            public void fail(ErrorCode errorCode) {
            }
        }) 

        assert success1
        assert success2
        assert success3
        assert success4
        assert success5
    }

    @Override
    void clean() {

    }
}