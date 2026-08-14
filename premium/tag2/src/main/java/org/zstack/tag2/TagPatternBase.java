package org.zstack.tag2;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.tag.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;
import static org.zstack.header.tag.TagErrors.TAG_QUOTA_EXCEEDED;
import static org.zstack.tag2.Tag2ManagerImpl.ensureUniquenessMerge;
import static org.zstack.utils.StringDSL.s;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TagPatternBase {
    private static final CLogger logger = Utils.getLogger(TagPatternBase.class);

    private TagPatternVO self;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private String syncThreadName;

    TagPatternBase(TagPatternVO vo) {
        self = vo;
        syncThreadName = "tag-pattern-" + self.getUuid();
    }

    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AttachTagToResourcesMsg) {
            handle((AttachTagToResourcesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateTagMsg) {
            handle((APIUpdateTagMsg) msg);
        } else if (msg instanceof APIAttachTagToResourcesMsg){
            handle((APIAttachTagToResourcesMsg) msg);
        } else if (msg instanceof APIDetachTagFromResourcesMsg) {
            handle((APIDetachTagFromResourcesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateTagMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("update-tag-pattern-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                updateTag(msg, chain);
            }
        });
    }

    private void updateTag(APIUpdateTagMsg msg, SyncTaskChain chain) {
        APIUpdateTagEvent evt = new APIUpdateTagEvent(msg.getId());
        if (msg.getName() != null) {
            self.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
        }
        if (msg.getValue() != null) {
            self.setValue(msg.getValue());
        }
        if (msg.getColor() != null) {
            self.setColor(msg.getColor());
        }

        ensureUniquenessMerge(self, msg.getSession().getAccountUuid(), null);
        self = dbf.updateAndRefresh(self);
        evt.setInventory(TagPatternInventory.valueOf(self));
        bus.publish(evt);
        chain.next();
    }

    private void handle(APIAttachTagToResourcesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("attach-tag-pattern-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                attachTag(msg, chain);
            }
        });
    }

    private void attachTag(APIAttachTagToResourcesMsg msg, SyncTaskChain chain) {
        APIAttachTagToResourcesEvent evt = new APIAttachTagToResourcesEvent(msg.getId());
        AttachTagToResourcesStruct struct = new AttachTagToResourcesStruct(msg.getTagUuid(), msg.getResourceUuids(), msg.getTokens());
        attachTag(struct, new ReturnValueCompletion<List<AttachTagResult>>(chain) {
            @Override
            public void success(List<AttachTagResult> results) {
                evt.setResults(results);
                bus.publish(evt);
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                chain.next();
            }
        });
    }

    private void attachTag(AttachTagToResourcesStruct struct, ReturnValueCompletion<List<AttachTagResult>> completion) {
        List<AttachTagResult> results = new ArrayList<>();

        Map<String, String> resourceTypes = getResourceType(struct.resourceUuids);
        new SQLBatch(){
            @Override
            protected void scripts() {
                int count = 0;
                String tag = buildTagByTokens(self.getValue(), struct.tokens);
                TagPatternInventory pattern = TagPatternInventory.valueOf(self);
                int tagLimit = Tag2GlobalConfig.ATTACHED_TAG_LIMIT.value(Integer.class);

                for (String resourceUuid : struct.resourceUuids) {
                    UserTagVO existing = q(UserTagVO.class).eq(UserTagVO_.tagPatternUuid, self.getUuid()).eq(UserTagVO_.resourceUuid, resourceUuid).find();
                    if (existing != null) {
                        logger.debug(String.format("tagPattern[uuid:%s] has been attached to resource[uuid:%s], skip",
                                self.getUuid(), resourceUuid));
                        results.add(new AttachTagResult(UserTagInventory.valueOf(existing)));
                        continue;
                    }

                    long attachedCount = q(UserTagVO.class).notNull(UserTagVO_.tagPatternUuid).eq(UserTagVO_.resourceUuid, resourceUuid).count();
                    if (attachedCount >= tagLimit) {
                        results.add(new AttachTagResult(err(TAG_QUOTA_EXCEEDED,
                                "resource[uuid:%s] has been attached %d tags, cannot attach any more",
                                resourceUuid, attachedCount)
                                .withOpaque("tag.count.limit", tagLimit)));
                        continue;
                    }

                    UserTagVO vo = new UserTagVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setTag(tag);
                    vo.setTagPatternUuid(struct.tagUuid);
                    vo.setType(TagType.User);
                    vo.setResourceUuid(resourceUuid);
                    vo.setResourceType(resourceTypes.get(resourceUuid));
                    persist(vo);

                    if (++count % 500 == 0) {
                        flush();
                    }

                    UserTagInventory inv = UserTagInventory.valueOf(vo);
                    inv.setTagPattern(pattern);
                    results.add(new AttachTagResult(inv));
                }
            }
        }.execute();

        completion.success(results);
    }

    @Transactional(readOnly = true)
    protected Map<String, String> getResourceType(List<String> resourceUuids) {
        Map<String, String> resourceType = new HashMap<>();

        for (List<String> sub : Lists.partition(resourceUuids, 100)) {
            List<Tuple> types = Q.New(ResourceVO.class).select(ResourceVO_.uuid, ResourceVO_.resourceType).in(ResourceVO_.uuid, sub).listTuple();
            types.forEach(t -> resourceType.put((String) t.get(0), (String) t.get(1)));
        }

        return resourceType;
    }

    private String buildTagByTokens(String format, Map<String, String> tokens) {
        if (tokens != null && !tokens.isEmpty()) {
            return s(format).formatByMap(tokens);
        } else {
            return format;
        }
    }

    private void handle(APIDetachTagFromResourcesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("detach-tag-pattern-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                detachTag(msg, chain);
            }
        });
    }

    private void detachTag(APIDetachTagFromResourcesMsg msg, SyncTaskChain chain) {
        APIDetachTagFromResourcesEvent evt = new APIDetachTagFromResourcesEvent(msg.getId());
        SQL.New(UserTagVO.class)
                .in(UserTagVO_.resourceUuid, msg.getResourceUuids())
                .eq(UserTagVO_.tagPatternUuid, msg.getTagPatternUuid())
                .delete();

        bus.publish(evt);
        chain.next();
    }

    private void handle(AttachTagToResourcesMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("attach-tag-pattern-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                attachTag(msg, chain);
            }
        });
    }

    private void attachTag(AttachTagToResourcesMsg msg, SyncTaskChain chain) {
        AttachTagToResourcesReply reply = new AttachTagToResourcesReply();
        AttachTagToResourcesStruct struct = new AttachTagToResourcesStruct(msg.getTagUuid(), msg.getResourceUuids(), msg.getTokens());
        attachTag(struct, new ReturnValueCompletion<List<AttachTagResult>>(chain) {
            @Override
            public void success(List<AttachTagResult> results) {
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                chain.next();
            }
        });
    }
}
