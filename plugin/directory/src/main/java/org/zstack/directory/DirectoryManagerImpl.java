package org.zstack.directory;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * @author shenjin
 * @date 2022/11/28 17:54
 */
public class DirectoryManagerImpl extends AbstractService implements DirectoryManager, Component {
    private static final CLogger logger = Utils.getLogger(DirectoryManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    private static final List<String> DIRECTORY_TYPES = asList(
            DirectoryConstant.DEFAULT_DIRECTORY, DirectoryConstant.VCENTER_DIRECTORY
    );

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DirectoryMessage) {
            passThrough((DirectoryMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateDirectoryMsg) {
            handle((APICreateDirectoryMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(DirectoryMessage msg) {
        DirectoryVO vo = dbf.findByUuid(msg.getDirectoryUuid(), DirectoryVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find directory[uuid=%s]", msg.getDirectoryUuid()));
            return;
        }

        DirectoryBase base = new DirectoryBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handle(APICreateDirectoryMsg msg) {
        APICreateDirectoryEvent event = new APICreateDirectoryEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {

            @Override
            public String getSyncSignature() {
                return DirectoryConstant.OPERATE_DIRECTORY_THREAD_NAME;
            }

            @Override
            public void run(SyncTaskChain chain) {
                createDirectory(msg, event, new Completion(chain) {
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
                return String.format("update-directory-name-%s", msg.getName());
            }
        });
    }

    private void createDirectory(APICreateDirectoryMsg msg, APICreateDirectoryEvent event, Completion completion) {
        DirectoryVO vo = new DirectoryVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        String name = msg.getName();
        vo.setName(name);
        List<DirectoryVO> directoryVOS;
        if(StringUtils.isEmpty(msg.getParentUuid())){
            //first-level directory
            vo.setGroupName(name);
            //judge whether the same level directory has the same name
            directoryVOS = Q.New(DirectoryVO.class).isNull(DirectoryVO_.parentUuid).list();
            vo.setRootDirectoryUuid(Platform.getUuid());
        } else {
            String groupName = String.format("/%s", msg.getName());
            DirectoryVO parentVO = dbf.findByUuid(msg.getParentUuid(), DirectoryVO.class);
            vo.setRootDirectoryUuid(parentVO.getRootDirectoryUuid());
            vo.setGroupName(parentVO.getGroupName() + groupName);
            vo.setParentUuid(msg.getParentUuid());
            //judge whether the same level directory has the same name
            directoryVOS = Q.New(DirectoryVO.class).eq(DirectoryVO_.parentUuid, parentVO.getUuid()).list();
        }
        List<DirectoryVO> list = directoryVOS.stream()
                .filter(s -> s.getName().equals(name) && s.getType().equals(msg.getType()))
                .collect(Collectors.toList());
        if (!list.isEmpty()) {
            completion.fail(operr("duplicate directory name, directory[uuid: %s] with name %s already exists", list.get(0).getUuid(), msg.getName()));
            return;
        }
        //judge whether the maximum level is exceeded
        String[] split = vo.getGroupName().split("/");
        // the directory cannot exceed 4 floors (contains the default directory)
        if(split.length > 3) {
            completion.fail(operr("fail to create directory, directories are up to four levels"));
            return;
        }
        if (!DIRECTORY_TYPES.contains(msg.getType())) {
            completion.fail(operr("the type of directory %s is not supported, the supported directory types are %s", msg.getType(), DIRECTORY_TYPES));
            return;
        }
        vo.setType(msg.getType());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setCreateDate(new Timestamp(new Date().getTime()));
        dbf.persist(vo);
        event.setInventory(DirectoryInventory.valueOf(vo));
        completion.success();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
