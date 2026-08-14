package org.zstack.directory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StringUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * @author shenjin
 * @date 2022/12/5 11:18
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DirectoryBase {


    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    protected DirectoryVO self;
    protected String syncThreadName;

    public DirectoryBase(DirectoryVO self) {
        this.self = self;
        this.syncThreadName = "Directory-" + self.getUuid();
    }

    @MessageSafe
    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteDirectoryMsg) {
            handle((APIDeleteDirectoryMsg) msg);
        } else if (msg instanceof APIMoveDirectoryMsg) {
            handle((APIMoveDirectoryMsg) msg);
        } else if (msg instanceof APIUpdateDirectoryMsg) {
            handle((APIUpdateDirectoryMsg) msg);
        } else if (msg instanceof APIAddResourcesToDirectoryMsg) {
            handle((APIAddResourcesToDirectoryMsg) msg);
        } else if (msg instanceof APIMoveResourcesToDirectoryMsg) {
            handle((APIMoveResourcesToDirectoryMsg) msg);
        } else if (msg instanceof APIRemoveResourcesFromDirectoryMsg) {
            handle((APIRemoveResourcesFromDirectoryMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRemoveResourcesFromDirectoryMsg msg) {
        APIRemoveResourcesFromDirectoryEvent event = new APIRemoveResourcesFromDirectoryEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                removeResourcesFromDirectory(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("remove-resources-from-directory-%s", msg.getDirectoryUuid());
            }
        });
    }

    private void removeResourcesFromDirectory(APIRemoveResourcesFromDirectoryMsg msg, Completion completion) {
        SQL.New(ResourceDirectoryRefVO.class)
                .eq(ResourceDirectoryRefVO_.directoryUuid, msg.getDirectoryUuid())
                .in(ResourceDirectoryRefVO_.resourceUuid, msg.getResourceUuids())
                .delete();
        completion.success();
    }

    private void handle(APIMoveResourcesToDirectoryMsg msg) {
        APIMoveResourcesToDirectoryEvent event = new APIMoveResourcesToDirectoryEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                moveResourcesToDirectory(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("move-resource-to-directory-%s", msg.getDirectoryUuid());
            }
        });
    }

    private void moveResourcesToDirectory(APIMoveResourcesToDirectoryMsg msg, Completion completion) {
        SQL.New(ResourceDirectoryRefVO.class)
                .set(ResourceDirectoryRefVO_.directoryUuid, msg.getDirectoryUuid())
                .in(ResourceDirectoryRefVO_.resourceUuid, msg.getResourceUuids())
                .update();
        completion.success();
    }

    private void handle(APIAddResourcesToDirectoryMsg msg) {
        APIAddResourcesToDirectoryEvent event = new APIAddResourcesToDirectoryEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                addResourcesToDirectory(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("add-resources-to-directory-%s", msg.getDirectoryUuid());
            }
        });
    }

    private void addResourcesToDirectory(APIAddResourcesToDirectoryMsg msg, Completion completion) {
        List<String> resourceUuids = msg.getResourceUuids();
        String resourceType = Q.New(ResourceVO.class)
                .select(ResourceVO_.resourceType)
                .in(ResourceVO_.uuid, resourceUuids)
                .limit(1)
                .findValue();
        List<ResourceDirectoryRefVO> list = new ArrayList<>();
        for (String resourceUuid : resourceUuids) {
            ResourceDirectoryRefVO refVO = new ResourceDirectoryRefVO();
            refVO.setDirectoryUuid(msg.getDirectoryUuid());
            refVO.setResourceUuid(resourceUuid);
            refVO.setResourceType(resourceType);
            refVO.setCreateDate(new Timestamp(new Date().getTime()));
            list.add(refVO);
        }
        dbf.persistCollection(list);
        completion.success();
    }

    private void handle(APIUpdateDirectoryMsg msg) {
        APIUpdateDirectoryEvent event = new APIUpdateDirectoryEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return DirectoryConstant.OPERATE_DIRECTORY_THREAD_NAME;
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateDirectory(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("update-directory-name-%s", msg.getUuid());
            }
        });
    }

    private void updateDirectory(APIUpdateDirectoryMsg msg, APIUpdateDirectoryEvent event, Completion completion) {
        DirectoryVO vo = dbf.findByUuid(msg.getUuid(), DirectoryVO.class);
        //if the renamed name is unchanged, it is returned successfully
        if (msg.getName().equals(vo.getName())) {
            completion.success();
            return;
        }
        //judge whether the same level directory has the same name
        List<DirectoryVO> directoryVOS;
        if (StringUtils.isEmpty(vo.getParentUuid())) {
            directoryVOS = Q.New(DirectoryVO.class).isNull(DirectoryVO_.parentUuid).list();
        } else {
            directoryVOS = Q.New(DirectoryVO.class).eq(DirectoryVO_.parentUuid, vo.getParentUuid()).list();
        }
        List<DirectoryVO> list = directoryVOS.stream()
                .filter(s -> s.getName().equals(msg.getName()) && s.getType().equals(vo.getType()))
                .collect(Collectors.toList());
        if (!list.isEmpty()) {
            completion.fail(operr("duplicate directory name, directory[uuid: %s] with name %s already exists", list.get(0).getUuid(), msg.getName()));
            return;
        }
        updateGroupName(msg, vo);
        vo.setName(msg.getName());
        dbf.update(vo);
        event.setInventory(DirectoryInventory.valueOf(vo));
        completion.success();
    }

    private void updateGroupName(APIUpdateDirectoryMsg msg, DirectoryVO vo) {
        String oldGroupName = vo.getGroupName();
        String oldName = vo.getName();
        String substring = oldGroupName.substring(0, oldGroupName.length() - oldName.length());
        String newGroupName = substring + msg.getName();
        vo.setGroupName(newGroupName);
        updateChildrenGroupName(oldGroupName, newGroupName, msg.getUuid());
    }

    private void updateChildrenGroupName(String oldGroupName, String newGroupName, String uuid) {
        List<DirectoryVO> vos = Q.New(DirectoryVO.class).like(DirectoryVO_.groupName, oldGroupName + "/%").list();
        List<DirectoryVO> list = new ArrayList<>();
        if (!vos.isEmpty()) {
            for(DirectoryVO vo : vos) {
                // admin/test/test1 -> admin1/test/test1
                String groupName = vo.getGroupName();
                int length = groupName.length();
                String nowGroupName = newGroupName + groupName.substring(oldGroupName.length(), length);
                vo.setGroupName(nowGroupName);
                list.add(vo);
            }
            dbf.updateCollection(list);
        }
    }

    private void handle(APIMoveDirectoryMsg msg) {
        APIMoveDirectoryEvent event = new APIMoveDirectoryEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return DirectoryConstant.OPERATE_DIRECTORY_THREAD_NAME;
            }

            @Override
            public void run(SyncTaskChain chain) {
                moveDirectory(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("move-directory-%s", msg.getDirectoryUuid());
            }
        });
    }

    private void moveDirectory(APIMoveDirectoryMsg msg, Completion completion) {
        DirectoryVO vo = dbf.findByUuid(msg.getDirectoryUuid(), DirectoryVO.class);
        //avoid looping
        List<String> list = new ArrayList<>();
        List<String> uuids = Q.New(DirectoryVO.class)
                .select(DirectoryVO_.uuid)
                .eq(DirectoryVO_.parentUuid, vo.getUuid())
                .listValues();
        if (!uuids.isEmpty()) {
            list.addAll(uuids);
            findSubUuids(uuids, list);
        }
        if (list.contains(msg.getTargetParentUuid())) {
            completion.fail(operr("circular dependency detected, directory %s and directory %s will cause circular dependency", msg.getDirectoryUuid(), msg.getTargetParentUuid()));
            return;
        }
        vo.setParentUuid(msg.getTargetParentUuid());
        DirectoryVO parentVO = Q.New(DirectoryVO.class).eq(DirectoryVO_.uuid, msg.getTargetParentUuid()).find();
        String newGroupName = String.format("%s/%s", parentVO.getGroupName(), vo.getName());
        vo.setGroupName(newGroupName);
        dbf.update(vo);
        completion.success();
    }

    private void findSubUuids(List<String> uuids, List<String> list) {
        List<String> subUuids = Q.New(DirectoryVO.class)
                .select(DirectoryVO_.uuid)
                .in(DirectoryVO_.parentUuid, uuids)
                .listValues();
        if (!subUuids.isEmpty()) {
            list.addAll(subUuids);
            findSubUuids(subUuids, list);
        }
    }

    private void handle(APIDeleteDirectoryMsg msg) {
        APIDeleteDirectoryEvent event = new APIDeleteDirectoryEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return DirectoryConstant.OPERATE_DIRECTORY_THREAD_NAME;
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteDirectory(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-directory-%s",msg.getUuid());
            }
        });
    }

    private void deleteDirectory(APIDeleteDirectoryMsg msg, Completion completion) {
        doDeleteDirectory(msg.getUuid());
        completion.success();
    }

    //TODO:Limit the number
    private void doDeleteDirectory(String uuid) {
        List<DirectoryVO> subDirectorys;
        List<DirectoryVO> list = new ArrayList<>();
        subDirectorys = Q.New(DirectoryVO.class)
                .eq(DirectoryVO_.parentUuid, uuid)
                .list();
        if(!subDirectorys.isEmpty()) {
            list.addAll(subDirectorys);
            List<String> uuids = subDirectorys.stream().map(DirectoryVO::getUuid).collect(Collectors.toList());
            findSubDirectorys(uuids, list);
            dbf.removeCollection(list, DirectoryVO.class);
        }
        DirectoryVO vo = dbf.findByUuid(uuid, DirectoryVO.class);
        dbf.remove(vo);
    }

    private void findSubDirectorys(List<String> uuids, List<DirectoryVO> list) {
        List<DirectoryVO> subDirectorys = Q.New(DirectoryVO.class).in(DirectoryVO_.parentUuid, uuids).list();
        if (subDirectorys.isEmpty()) {
            return;
        }
        list.addAll(subDirectorys);
        List<String> subUuids = subDirectorys.stream().map(DirectoryVO::getUuid).collect(Collectors.toList());
        findSubDirectorys(subUuids, list);
    }
}

