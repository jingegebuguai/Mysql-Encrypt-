package com.core;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

public class RNDAlgorithm {

    // 对二进制进行Base64编码
    /**
     * DETAlgorithm是进行确定加密的
     *
     * @param content
     *            需要加密的内容
     * @param key
     *            加密密钥，需要通过KeyManager来产生
     * @return
     */

    public String encrypt(String content, Key key, IvParameterSpec ivspec) {
        try {

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// ����������

            // 将明文字符串类型的数据转换成字节类型，以utf-8进行编码

            /*
             * 注意这条语句，如果我们先对明文进行DET加密，生成一个以Base64编码的字符串，这里如果使用utf-8编码，会不会
             * 报错，现在还未知。
             */

            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, key, ivspec);// 初始化
            byte[] result = cipher.doFinal(byteContent);
            return Base64.encode(result); // 加密
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    /**
     *
     *
     * @param content
     *            待解密内容，数据库中的数据都是用Base64编码后的字符串
     * @param key
     *            解密密钥
     * @return 返回结果，以二进制表示
     */

    public byte[] decrypt(String content, Key key, IvParameterSpec ivspec) {
        try {
            byte[] byteContent = Base64.decode(content);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//创建密码器
            cipher.init(Cipher.DECRYPT_MODE, key, ivspec);// 初始化
            byte[] result = cipher.doFinal(byteContent);
            return result; // 加密
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
