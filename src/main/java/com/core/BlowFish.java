package com.core;

import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;


public class BlowFish {


    /**
     * 密钥产生函数，用于生成一个加解密密钥
     *
     * @return
     * @throws Exception
     */


    public static Key keyGenerator() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("Blowfish");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }


    /**
     * 加密函数，这个函数将明文字符串加密成为密文字符串
     *
     * @param key
     *            加密密钥
     * @param text
     *            明文字符串
     * @return 密文字符串
     * @throws Exception
     * @see com.core.Base64
     */

    public static String encrypt(Key key, String text) throws Exception {
        Cipher cipher = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // 使用Base64编码将二进制串转换成字符串
        return Base64.encode(cipher.doFinal(text.getBytes()));
    }


    /**
     * 解密
     *
     * @param key
     *            加密密钥
     * @param encrypedData
     *            密文
     * @return 明文
     * @throws Exception
     */

    public static String decrypt(Key key, String encrypedData) throws Exception {
        Cipher cipher = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.decode(encrypedData)));
    }
}