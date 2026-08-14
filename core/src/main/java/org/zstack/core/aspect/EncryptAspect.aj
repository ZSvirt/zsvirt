package org.zstack.core.aspect;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.encrypt.EncryptRSA;

public aspect EncryptAspect {
    private static final CLogger logger = Utils.getLogger(EncryptAspect.class);

    @Autowired
    private EncryptRSA rsa;

    void around(String param) : args(param) && execution(@org.zstack.header.core.encrypt.ENCRYPT * *(..)){
        if(param.length() > 0){
            try{
                param = rsa.encrypt1(param);
            }catch(Exception e){
                logger.debug(String.format("encrypt aspectj is error..."));
                logger.debug(e.getMessage());
                e.printStackTrace();
            }
            proceed(param);
        }
    }


}