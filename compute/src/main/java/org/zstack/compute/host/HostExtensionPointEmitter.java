package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HostExtensionPointEmitter implements Component {
    private static final CLogger logger = Utils.getLogger(HostExtensionPointEmitter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    private List<HostPingTaskExtensionPoint> pingTaskExts = new ArrayList<>();
    private List<HostDeleteExtensionPoint> deleteHostExts = new ArrayList<>();
    private List<HostChangeStateExtensionPoint> changeStateExts = new ArrayList<>();
    private List<HostConnectionReestablishExtensionPoint> connetionReestablishExts = new ArrayList<>();
    private List<HostAddExtensionPoint> addHostExts = new ArrayList<>();

    public void preDelete(HostInventory hinv) throws HostException {
        for (HostDeleteExtensionPoint extp : deleteHostExts) {
            extp.preDeleteHost(hinv);
        }
    }

    public void preChange(HostVO vo, HostStateEvent event) throws HostException {
        HostState next = vo.getState().nextState(event);
        HostInventory hinv = HostInventory.valueOf(vo);
        for (HostChangeStateExtensionPoint extp : changeStateExts) {
            extp.preChangeHostState(hinv, event, next);
        }
    }

    public void preChange(List<HostVO> vos, HostStateEvent event) throws HostException {
        for (HostVO vo : vos) {
            preChange(vo, event);
        }
    }

    public void beforeChange(HostVO vo, final HostStateEvent event) {
        final HostInventory hinv = HostInventory.valueOf(vo);
        final HostState next = vo.getState().nextState(event);
        CollectionUtils.safeForEach(changeStateExts, new ForEachFunction<HostChangeStateExtensionPoint>() {
            @Override
            public void run(HostChangeStateExtensionPoint extp) {
                extp.beforeChangeHostState(hinv, event, next);
            }
        });
    }

    public void afterChange(HostVO vo, final HostStateEvent event, final HostState prevState) {
        final HostInventory hinv = HostInventory.valueOf(vo);
        CollectionUtils.safeForEach(changeStateExts, new ForEachFunction<HostChangeStateExtensionPoint>() {
            @Override
            public void run(HostChangeStateExtensionPoint extp) {
                extp.afterChangeHostState(hinv, event, prevState);
            }
        });
    }

    public void beforeDelete(final HostInventory hinv) {
        CollectionUtils.safeForEach(deleteHostExts, new ForEachFunction<HostDeleteExtensionPoint>() {
            @Override
            public void run(HostDeleteExtensionPoint extp) {
                extp.beforeDeleteHost(hinv);
            }
        });
    }

    public void afterDelete(final HostInventory hinv) {
        CollectionUtils.safeForEach(deleteHostExts, new ForEachFunction<HostDeleteExtensionPoint>() {
            @Override
            public void run(HostDeleteExtensionPoint extp) {
                extp.afterDeleteHost(hinv);
            }
        });
    }

    public void hostPingTask(final HypervisorType type, final HostInventory inv) {
        CollectionUtils.safeForEach(pingTaskExts, new ForEachFunction<HostPingTaskExtensionPoint>() {
            @Override
            public void run(HostPingTaskExtensionPoint ext) {
                if (ext.getHypervisorType().equals(type)) {
                    ext.executeTaskAlongWithPingTask(inv);
                }
            }
        });
    }

    public void connectionReestablished(HypervisorType hvType, HostInventory host) throws HostException {
        for (HostConnectionReestablishExtensionPoint ext : connetionReestablishExts) {
            if (ext.getHypervisorTypeForReestablishExtensionPoint().equals(hvType)) {
                ext.connectionReestablished(host);
            }
        }
    }

    private void beforeAddHost(final Iterator<HostAddExtensionPoint> it, final HostInventory host, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        HostAddExtensionPoint ext = it.next();
        ext.beforeAddHost(host, new Completion(completion) {
            @Override
            public void success() {
                beforeAddHost(it, host, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void beforeAddHost(HostInventory host, Completion completion) {
        if (addHostExts.isEmpty()) {
            completion.success();
            return;
        }

        beforeAddHost(addHostExts.iterator(), host, completion);
    }

    public void afterAddHost(HostInventory host, Completion completion) {
        if (addHostExts.isEmpty()) {
            completion.success();
            return;
        }

        afterAddHost(addHostExts.iterator(), host, completion);
    }

    private void afterAddHost(final Iterator<HostAddExtensionPoint> it, final HostInventory host, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        HostAddExtensionPoint ext = it.next();
        ext.afterAddHost(host, new Completion(completion) {
            @Override
            public void success() {
                afterAddHost(it, host, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void populateExtensions() {
        pingTaskExts = pluginRgty.getExtensionList(HostPingTaskExtensionPoint.class);
        deleteHostExts = pluginRgty.getExtensionList(HostDeleteExtensionPoint.class);
        changeStateExts = pluginRgty.getExtensionList(HostChangeStateExtensionPoint.class);
        connetionReestablishExts = pluginRgty.getExtensionList(HostConnectionReestablishExtensionPoint.class);
        addHostExts = pluginRgty.getExtensionList(HostAddExtensionPoint.class);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
