package com.core;

//import org.apache.commons.codec.binary.Base64;
public class Base64 {


    /**
     * 编码，将二进制串转换成字符串
     *
     * @param binary
     *            二进制串
     * @return 字符串
     * @see org.apache.commons.codec.binary.Base64
     */

    public static String encode(byte[] binary) {

        return org.apache.commons.codec.binary.Base64.encodeBase64String(binary);
    }

    /**
     * 解码，将成字符串转换二进制串
     *
     * @param str
     *            字符串
     * @return 二进制串
     * @see org.apache.commons.codec.binary.Base64
     */

    public static byte[] decode(String str) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(str);
    }

}