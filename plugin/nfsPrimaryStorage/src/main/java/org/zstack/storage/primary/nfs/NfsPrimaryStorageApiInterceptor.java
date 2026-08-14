package org.zstack.storage.primary.nfs;

import java.util.Arrays;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.APIUpdatePrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.Tuple;
import java.util.List;


/**
 */
public class NfsPrimaryStorageApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddNfsPrimaryStorageMsg) {
            validate((APIAddNfsPrimaryStorageMsg) msg);
        }else if(msg instanceof APIUpdatePrimaryStorageMsg){
            validate((APIUpdatePrimaryStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddNfsPrimaryStorageMsg msg) {
        new NfsApiParamChecker().checkUrl(msg.getZoneUuid(), msg.getSystemTags(), msg.getUrl());
    }

    private void validate(APIUpdatePrimaryStorageMsg msg){
        Tuple t = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type, PrimaryStorageVO_.url, PrimaryStorageVO_.zoneUuid)
                .eq(PrimaryStorageVO_.uuid, msg.getUuid())
                .findTuple();

        String type = t.get(0, String.class);
        String url = t.get(1, String.class);
        String zoneUuid = t.get(2, String.class);
        if(!type.equals(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)){
            return;
        }

        if (msg.getUrl() != null && !msg.getUrl().equals(url)){
            new SQLBatch(){
                @Override
                protected void scripts() {
                    NfsApiParamChecker checker = new NfsApiParamChecker();
                    checker.checkUrl(zoneUuid, msg.getSystemTags(), msg.getUrl());
                    checker.checkRunningVmForUpdateUrl(msg.getUuid());
                }
            }.execute();
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIAddNfsPrimaryStorageMsg.class,
            APIUpdatePrimaryStorageMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
