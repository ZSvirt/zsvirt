package org.zstack.core.aspect;

import org.apache.commons.codec.binary.Base64;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.zstack.core.thread.SyncThreadSignature;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.encrypt.EncryptRSA;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;


public aspect DecryptAspect {

	@Autowired
	private EncryptRSA encryptRSA;
	private static final CLogger logger = Utils.getLogger(DecryptAspect.class);

	Object around(): execution(@org.zstack.header.core.encrypt.DECRYPT * *(..)){
		Object value = proceed();
		if (value != null){
			try{
				value = encryptRSA.decrypt1((String) value);
			}catch(Exception e){
				logger.debug(String.format("decrypt aspectj is error..., no need decrypt"));
				logger.debug(e.getMessage());
			}
		}
		return value;
	}

}