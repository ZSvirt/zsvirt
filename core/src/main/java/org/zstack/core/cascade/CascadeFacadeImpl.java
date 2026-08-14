package org.zstack.core.cascade;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.workflow.*;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Bucket;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Callable;

/**
 */
public class CascadeFacadeImpl implements CascadeFacade, Component {
    private static final CLogger logger = Utils.getLogger(CascadeFacadeImpl.class);

    private class Node implements Comparable<Node> {
        private CascadeExtensionPoint extension;
        private TreeSet<Node> edges;
        private String name;

        @Override
        public int compareTo(Node n) {
            return this.getName().compareTo(n.getName());
        }

        public CascadeExtensionPoint getExtension() {
            return extension;
        }

        public void setExtension(CascadeExtensionPoint extension) {
            this.extension = extension;
        }

        public TreeSet<Node> getEdges() {
            return edges;
        }

        public void setEdges(TreeSet<Node> edges) {
            this.edges = edges;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static class TreeNode implements Comparable<TreeNode> {
        private Node node;
        private TreeSet<TreeNode> leafs;

        @Override
        public int compareTo(TreeNode t) {
            return this.node.getName().compareTo(t.node.getName());
        }
    }

    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String, Node> nodes = new HashMap<>();
    private Map<String, TreeNode> cascadeTree = new HashMap<>();
    private Map<String, List<AsyncBranchCascadeExtensionPoint>> asyncBranchCascadeExtensionPoints = new HashMap<>();

    private void doSyncCascade(TreeNode treeNode, boolean init, CascadeAction action) throws CascadeException {
        CascadeAction currentAction;
        Node node = treeNode.node;
        if (!init) {
            currentAction = node.getExtension().createActionForChildResource(action);
        } else {
            currentAction = action;
        }

        if (currentAction != null) {
            for (TreeNode tn : treeNode.leafs) {
                doSyncCascade(tn, false, currentAction);
            }
        }

        logger.debug(String.format("[Sync cascade (%s)]: %s --> %s", action.getActionCode(), action.getParentIssuer(), node.getName()));
        node.getExtension().syncCascade(action);
    }


    @Override
    public void syncCascade(String actionCode, String issuer, Object context) throws CascadeException {
        CascadeAction action = new CascadeAction().setRootIssuer(issuer).setRootIssuerContext(context)
                .setParentIssuer(issuer).setParentIssuerContext(context).setActionCode(actionCode);
        syncCascade(action);
    }

    @Override
    public void syncCascade(CascadeAction action) throws CascadeException {
        assert action.getRootIssuer() != null;
        assert action.getParentIssuer() != null;
        assert action.getActionCode() != null;

        TreeNode root = cascadeTree.get(action.getRootIssuer());
        doSyncCascade(root, true, action);
    }

    private void checkForNullElement(Node node, CascadeAction currentAction) {
        Object parentIssuerContext = currentAction.getParentIssuerContext();
        if (parentIssuerContext != null && parentIssuerContext instanceof List) {
            List lst = (List) parentIssuerContext;
            for (Object obj : lst) {
                if (obj == null) {
                    throw new CloudRuntimeException(
                            String.format("CascadeExtensionPoint[%s] returns parent content that is a List but containing NULL element",
                                    node.getExtension().getClass().getName()));
                }
            }
        }

        Object rootIssuerContext = currentAction.getRootIssuerContext();
        if (rootIssuerContext != null && rootIssuerContext instanceof List) {
            List lst = (List) rootIssuerContext;
            for (Object obj : lst) {
                if (obj == null) {
                    throw new CloudRuntimeException(
                            String.format("CascadeExtensionPoint[%s] returns root content that is a List but containing NULL element",
                                    node.getExtension().getClass().getName()));
                }
            }
        }
    }

    private void collectPathsForAsyncCascade(TreeNode treeNode, boolean init, boolean fullTraverse, CascadeAction action, List<Bucket> result) {
        CascadeAction currentAction;
        Node node = treeNode.node;
        if (!init) {
            currentAction = node.getExtension().createActionForChildResource(action);
        } else {
            currentAction = action;
        }

        if (fullTraverse) {
            if (currentAction == null) {
                currentAction = new CascadeAction();
                currentAction.setActionCode(action.getActionCode());
                currentAction.setRootIssuer(action.getRootIssuer());
                currentAction.setRootIssuerContext(action.getRootIssuerContext());
                currentAction.setParentIssuer(node.getName());
                currentAction.setParentIssuerContext(null);
            }

            for (TreeNode tn : treeNode.leafs) {
                collectPathsForAsyncCascade(tn, false, true, currentAction, result);
            }
        } else {
            if (currentAction != null) {
                checkForNullElement(node, currentAction);

                for (TreeNode tn : treeNode.leafs) {
                    collectPathsForAsyncCascade(tn, false, false, currentAction, result);
                }
            }
        }

        result.add(Bucket.newBucket(node, action));
    }

    @Override
    public void asyncCascade(String actionCode, String issuer, Object context, Completion completion) {
        CascadeAction action = new CascadeAction().
                setRootIssuer(issuer).
                setRootIssuerContext(context).
                setParentIssuer(issuer).
                setParentIssuerContext(context).
                setActionCode(actionCode);
        asyncCascade(action, completion);
    }

    @Override
    public void asyncCascadeFull(String actionCode, String issuer, Object context, Completion completion) {
        CascadeAction action = new CascadeAction().
                setRootIssuer(issuer).
                setRootIssuerContext(context).
                setParentIssuer(issuer).
                setParentIssuerContext(context).
                setActionCode(actionCode).
                setFullTraverse(true);
        asyncCascade(action, completion);
    }


    @Override
    public void asyncCascade(CascadeAction action, final Completion completion) {
        assert action.getRootIssuer() != null;
        assert action.getParentIssuer() != null;
        assert action.getActionCode() != null;

        TreeNode root = cascadeTree.get(action.getRootIssuer());
        DebugUtils.Assert(root != null, String.format("found no CascadeExtension for %s", action.getRootIssuer()));
        List<Bucket> paths = new ArrayList<>();
        collectPathsForAsyncCascade(root, true, action.isFullTraverse(), action, paths);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        for (Bucket path : paths) {
            final Node node = path.get(0);
            final CascadeAction caction = path.get(1);
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    logger.debug(String.format("[Async cascade (%s)]: %s --> %s",
                            caction.getActionCode(), caction.getParentIssuer(), node.getName()));

                    runNode(node, caction, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).setName(String.format("Cascade: %s", action.getActionCode())).start();
    }

    private void runNode(Node node, CascadeAction caction, Completion completion) {
        List<AsyncBranchCascadeExtensionPoint> branches = asyncBranchCascadeExtensionPoints.get(node.getName());
        boolean skipNode = false;
        if (branches != null) {
            for (AsyncBranchCascadeExtensionPoint branch : branches) {
                skipNode = branch.skipOriginCascadeExtension(caction);
                if (skipNode) {
                    logger.debug(String.format("the AsyncCascadeExtension[%s, resourceName:%s] is skipped by %s",
                            ((CascadeWrapper)node.getExtension()).origin.getClass(), node.getName(), branch.getClass()));
                }
            }
        }

        if (!skipNode) {
            node.getExtension().asyncCascade(caction, completion);
            return;
        }

        // the origin cascade extension is skipped by an AsyncBranchCascadeExtensionPoint
        // now we execute all AsyncBranchCascadeExtensionPoints

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("branch-cascade-extension-points-for-%s", node.getName()));
        branches.forEach(b -> chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                b.asyncCascade(caction, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }));

        CascadeWrapper wrapper = (CascadeWrapper) node.getExtension();
        CascadeExtensionPoint addon = wrapper.findAddon(caction.getParentIssuer());

        if (addon != null) {
            chain.then(new NoRollbackFlow() {
                String __name__ = String.format("call-addon-%s", addon.getClass().getName());

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    addon.asyncCascade(caction, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void syncCascadeNoException(String actionCode, String issuer, Object context) {
        try {
            syncCascade(actionCode, issuer, context);
        } catch (CascadeException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public void syncCascadeNoException(CascadeAction action) {
        try {
            syncCascade(action);
        } catch (CascadeException e) {
            logger.warn(e.getMessage(), e);
        }
    }


    private void traverse(Node node, List<Node> resolved, List<List<Node>> paths) {
        if (resolved.contains(node)) {
            List<Node> path = new ArrayList<>();
            path.addAll(resolved);
            paths.add(path);
            return;
        }

        resolved.add(node);

        if (!node.getEdges().isEmpty()) {
            for (Node e : node.getEdges()) {
                traverse(e, resolved, paths);
            }
        } else {
            List<Node> path = new ArrayList<>();
            path.addAll(resolved);
            paths.add(path);
        }

        resolved.remove(node);
    }

    private TreeNode createTraversingTree(String issuer) {
        Node node = nodes.get(issuer);

        List<List<Node>> paths = new ArrayList<>();
        if (node.getEdges().isEmpty()) {
            List<Node> resolved = new ArrayList<>();
            resolved.add(node);
            traverse(node, resolved, paths);
        } else {
            for (Node n : node.getEdges()) {
                List<Node> resolved = new ArrayList<>();
                resolved.add(node);
                traverse(n, resolved, paths);
            }
        }


        List<List<String>> ret = new ArrayList<>();
        for (List<Node> path : paths) {
            List<String> spath = new ArrayList<>();
            for (Node n : path) {
                spath.add(n.getName());
            }
            ret.add(spath);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Cascade operation[%s]'s traversing branches (branches will be merged as a tree to reduce duplicate paths):", issuer));
            for (List<String> lst : ret) {
                logger.trace(lst.toString());
            }
        }

        return makeTree(paths);
    }

    private TreeNode makeTree(List<List<Node>> paths) {
        int maxLevel = 0;
        for (List<Node> path : paths) {
            if (path.size() > maxLevel) {
                maxLevel = path.size();
            }
        }

        Map<String, TreeNode> root = new HashMap<>();
        Map<String, TreeNode> prev = root;
        Map<String, TreeNode> curr = prev;
        for (int i = 0; i < maxLevel; i++) {
            for (List<Node> path : paths) {
                if (i >= path.size()) {
                    continue;
                }

                Node n = path.get(i);
                TreeNode tn = curr.get(n.getName());
                if (tn == null) {
                    tn = new TreeNode();
                    tn.node = n;
                    tn.leafs = new TreeSet<>();
                    curr.put(n.getName(), tn);
                }

                int j = i - 1;
                if (j < 0 || j >= path.size()) {
                    continue;
                }

                Node parent = path.get(j);
                TreeNode pn = prev.get(parent.getName());
                pn.leafs.add(tn);
            }
            prev = curr;
            curr = new HashMap<>();
        }

        assert root.size() == 1;
        return root.values().iterator().next();
    }

    private void populateCascadeNodes(Map<String, CascadeExtensionPoint> exts) {
        // resolve tree
        for (CascadeExtensionPoint ext : exts.values()) {
            Node n = nodes.get(ext.getCascadeResourceName());
            if (n == null) {
                n = new Node();
                n.setName(ext.getCascadeResourceName());
                n.setExtension(ext);
                n.setEdges(new TreeSet<>());
                nodes.put(n.getName(), n);
            }

            for (String parent : ext.getEdgeNames()) {
                Node p = nodes.get(parent);
                if (p == null) {
                    p = new Node();
                    p.setName(parent);
                    p.setEdges(new TreeSet<>());
                    CascadeExtensionPoint dext = exts.get(parent);
                    if (dext == null) {
                        throw new CloudRuntimeException(
                                String.format("cannot find parent CascadeExtensionPoint[%s] for CascadeExtensionPoint[name: %s, class: %s]",
                                        parent, ext.getCascadeResourceName(), ext.getClass().getName()));
                    }
                    p.setExtension(dext);
                    nodes.put(parent, p);
                }
                p.getEdges().add(n);
            }
        }

        for (Node n : nodes.values()) {
            if (n.getExtension().getEdgeNames().contains(n.getExtension().getCascadeResourceName())) {
                throw new CloudRuntimeException(
                        String.format("CascadeExtensionPoint[%s] is self referenced, a CascadeExtensionPoint cannot put itself as parent",
                                n.getExtension().getClass().getName()));
            }
        }
    }

    private class CascadeWrapper implements CascadeExtensionPoint {
        CascadeExtensionPoint origin;
        List<CascadeExtensionPoint> addons = new ArrayList<>();

        public CascadeWrapper(CascadeExtensionPoint origin) {
            this.origin = origin;
        }

        private CascadeExtensionPoint findExtensionPointByParent(String parent) {
            if (origin.getCascadeResourceName().equals(parent) || origin.getEdgeNames().contains(parent)) {
                return origin;
            } else {
                CascadeExtensionPoint addon = findAddon(parent);
                DebugUtils.Assert(addon != null, String.format("cannot find any cascade extension point has the parent[%s] for the resource[%s]",
                        parent, origin.getCascadeResourceName()));
                return addon;
            }
        }

        CascadeExtensionPoint findAddon(String parent) {
            Optional<CascadeExtensionPoint> o = addons.stream().filter(c->c.getEdgeNames().contains(parent)).findFirst();
            return o.isPresent() ? o.get() : null;
        }

        @Override
        public void syncCascade(CascadeAction action) throws CascadeException {
            findExtensionPointByParent(action.getParentIssuer()).syncCascade(action);
        }

        @Override
        public void asyncCascade(CascadeAction action, Completion completion) {
            findExtensionPointByParent(action.getParentIssuer()).asyncCascade(action, completion);
        }

        @Override
        public List<String> getEdgeNames() {
            return addons.isEmpty() ? origin.getEdgeNames() : new Callable<List<String>>() {
                @Override
                public List<String> call() {
                    List<String> es = new ArrayList<>();
                    es.addAll(origin.getEdgeNames());
                    for (CascadeExtensionPoint a : addons) {
                        es.addAll(a.getEdgeNames());
                    }
                    return es;
                }
            }.call();
        }

        @Override
        public String getCascadeResourceName() {
            return origin.getCascadeResourceName();
        }

        @Override
        public CascadeAction createActionForChildResource(CascadeAction action) {
            return findExtensionPointByParent(action.getParentIssuer()).createActionForChildResource(action);
        }
    }

    private void populateNodes() {
        Map<String, CascadeExtensionPoint> exts = new HashMap<>();
        for (CascadeExtensionPoint extp : pluginRgty.getExtensionList(CascadeExtensionPoint.class)) {
            CascadeExtensionPoint oext = exts.get(extp.getCascadeResourceName());
            if (oext != null) {
                throw new CloudRuntimeException(String.format("duplicate CascadeExtensionPoint[%s, %s] for type[%s]",
                        extp.getClass().getName(), oext.getClass().getName(), extp.getCascadeResourceName()));
            }

            exts.put(extp.getCascadeResourceName(), new CascadeWrapper(extp));
        }

        for (CascadeExtensionPoint e : exts.values()) {
            CascadeWrapper w = (CascadeWrapper) e;

            for (CascadeAddOnExtensionPoint a : pluginRgty.getExtensionList(CascadeAddOnExtensionPoint.class)) {
                CascadeExtensionPoint c = a.cascadeAddOn(e.getCascadeResourceName());
                if (c != null) {
                    if (!c.getCascadeResourceName().equals(w.getCascadeResourceName())) {
                        throw new CloudRuntimeException(String.format("the resourceName[%s] of the addon returned" +
                                " by %s is not %s. The plugin cannot populate an cascade addon with different" +
                                " resource name", c.getCascadeResourceName(), a.getClass(), w.getCascadeResourceName()));
                    }

                    w.addons.add(c);
                }
            }

        }

        populateCascadeNodes(exts);

        pluginRgty.getExtensionList(AsyncBranchCascadeExtensionPoint.class).forEach(ab -> {
            List<AsyncBranchCascadeExtensionPoint> lst = asyncBranchCascadeExtensionPoints.computeIfAbsent(ab.getCascadeResourceName(), k->new ArrayList<>());
            lst.add(ab);
        });
    }

    private void populateTree() {
        for (Node n : nodes.values()) {
            TreeNode tn = createTraversingTree(n.getName());
            cascadeTree.put(n.getName(), tn);
        }
    }

    @Override
    public boolean start() {
        populateNodes();
        populateTree();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
