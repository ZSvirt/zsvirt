package org.zstack.core.encrypt;


import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.Key;


/**
 * Created by mingjian.deng on 16/11/1.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class EncryptRSA {
	private static byte[] key1;
	private static Key key2;
	private static final String KEY_ALGORITHM = "AES";
	private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
	private static final CLogger logger = Utils.getLogger(EncryptRSA.class);
	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private static final String appendString = "crypt_key_for_v1::";
	@Autowired
	private GlobalConfigFacade gcf;


	public String updateKey(String key) throws Exception{
		try {
			EncryptGlobalConfig.ENCRYPT_ALGORITHM.updateValue(key);
			String keyString = EncryptGlobalConfig.ENCRYPT_ALGORITHM.value();

			byte[] srcBytes = encodeUTF8(keyString);
			key1 = Base64.decodeBase64(srcBytes);
			key2 = toKey(key1);

			return "success";
		}catch (Exception e){
			logger.debug("change key failed");
			logger.debug(e.getMessage());
			return "failed";
		}
	}

	public void initKey() throws Exception{
		try {
			if (key1 == null && key2 == null){
				String keyString = EncryptGlobalConfig.ENCRYPT_ALGORITHM.value();

				byte[] srcBytes = encodeUTF8(keyString);
				key1 = Base64.decodeBase64(srcBytes);
				key2 = toKey(key1);
			}
		}catch (Exception e){
			logger.debug(e.getMessage());
		}
	}


	private static Key toKey(byte[] key){
		return new SecretKeySpec(key, KEY_ALGORITHM);
	}


	public  byte[] encrypt(byte[] data,Key key) throws Exception{
		return encrypt(data, key,DEFAULT_CIPHER_ALGORITHM);
	}


	public  byte[] encrypt(byte[] data,byte[] key) throws Exception{
		return encrypt(data, key,DEFAULT_CIPHER_ALGORITHM);
	}


	public  byte[] encrypt(byte[] data,byte[] key,String cipherAlgorithm) throws Exception{
		Key k = toKey(key);
		return encrypt(data, k, cipherAlgorithm);
	}

	public  byte[] encrypt(byte[] data,Key key,String cipherAlgorithm) throws Exception{
		//init
		Cipher cipher = Cipher.getInstance(cipherAlgorithm);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	public  byte[] decrypt(byte[] data,byte[] key) throws Exception{
		return decrypt(data, key,DEFAULT_CIPHER_ALGORITHM);
	}

	public  byte[] decrypt(byte[] data,Key key) throws Exception{
		return decrypt(data, key,DEFAULT_CIPHER_ALGORITHM);
	}

	public  byte[] decrypt(byte[] data,byte[] key,String cipherAlgorithm) throws Exception{
		Key k = toKey(key);
		return decrypt(data, k, cipherAlgorithm);
	}

	public  byte[] decrypt(byte[] data,Key key,String cipherAlgorithm) throws Exception{
		Cipher cipher = Cipher.getInstance(cipherAlgorithm);
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	private String  showByteArray(byte[] data){
		if(null == data){
			return null;
		}
		StringBuilder sb = new StringBuilder("{");
		for(byte b:data){
			sb.append(b).append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("}");
		return sb.toString();
	}

	private static String decodeUTF8(byte[] bytes) {
		return new String(bytes, UTF8_CHARSET);
	}

	private static byte[] encodeUTF8(String string) {
		return string.getBytes(UTF8_CHARSET);
	}

	public static String generateKeyString(String randomFactor) {
		byte[] md5Data = encodeUTF8(StringDSL.getMd5Sum(randomFactor));
		byte[] base64Data = Base64.encodeBase64(md5Data);
		return decodeUTF8(base64Data);
	}

	public String encrypt1(String password) throws Exception{
		initKey();
		try {
			password = appendString+password;
			byte[] encryptData = encrypt(password.getBytes(),key2);
			byte[] base64EncryptData = Base64.encodeBase64(encryptData);
			return decodeUTF8(base64EncryptData);
		}catch (Exception e){
			logger.debug(e.getMessage());
			return password;
		}
	}

	public String encrypt(String password,String keyString){
		try {
			password = appendString+password;

			byte[] srcBytes = encodeUTF8(keyString);
			byte[] newKey1 = Base64.decodeBase64(srcBytes);
			Key newKey2 = toKey(newKey1);
			byte[] encryptData = encrypt(password.getBytes(),newKey2);
			byte[] base64EncryptData = Base64.encodeBase64(encryptData);
			return decodeUTF8(base64EncryptData);
		}catch (Exception e){
			logger.debug(e.getMessage());
			return password;
		}
	}

	public Object decrypt1(String password) throws Exception{
		initKey();
		try {
			byte[] srcBytes = encodeUTF8(password);
			byte[] desBytes = decrypt(Base64.decodeBase64(srcBytes), key2);
			String tempdecodeUTF8 = decodeUTF8(desBytes);
			if (tempdecodeUTF8.substring(0, appendString.length()).equals(appendString)){
				return tempdecodeUTF8.substring(appendString.length(),tempdecodeUTF8.length());
			}
			return password;
		}catch (Exception e){
			logger.debug(e.getMessage());
			return password;
		}

	}

	public String decrypt(String password,String keyString) {
		try {
			byte[] keySrcBytes = encodeUTF8(keyString);
			byte[] newKey1 = Base64.decodeBase64(keySrcBytes);
			Key newKey2 = toKey(newKey1);

			byte[] srcBytes = encodeUTF8(password);
			byte[] desBytes = decrypt(Base64.decodeBase64(srcBytes), newKey2);
			String tempdecodeUTF8 = decodeUTF8(desBytes);
			if (tempdecodeUTF8.substring(0, appendString.length()).equals(appendString)){
				return tempdecodeUTF8.substring(appendString.length(),tempdecodeUTF8.length());
			}
			return password;
		}catch (Exception e){
			logger.debug(e.getMessage());
			return password;
		}
	}

	public static Key getKey2() {
		return key2;
	}
}

