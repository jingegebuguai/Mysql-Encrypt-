package com.core;

/**
 *
 * 针对数据库中列名的隐藏<br>
 * 目前只是一个框架，其中的加解密的函数中，只是采用将字符串颠倒的方式“模拟”加密，后续可以在加解密函数中增加加密算法<br>
 *
 *
 */
public class NameHide {


    /**
     * 这个函数的作用是将明文的列名加密为密文下的列名，这目前使用的是简单的Base64算法<br>
     * 以telephone为例，经过这个函数的处理之后，得到的是dGVsZXBob25l，我们还需要使用后面几个getXXXTableName来改写为<br>
     * dGVsZXBob25l_DET、dGVsZXBob25l_OPE等形式、并以这种形式存放在数据库中。<br>
     *
     * @param name
     *            这个参数是明文下的列名
     * @return 返回加密后的密文列名
     * @throws Exception
     */

    public static String getSecretName(String name) throws Exception {
        return EncryptAlgorithm(name);
    }


    /**
     * 这个函数的作用是将密文下的列名解密成明文的列名<br>
     * 在数据库中的列名是以"secretTableName_type",其中的type是指：DET、OPE、JOIN、SEARCH或者HOM<br>
     * 首先将字符串以"_"分割为secretTableName和type两部分，前者经过Base64的解码后输出，后者以后会用。<br>
     * 以后的版本中必须加以修改，因为要避免“emp_id_DET”的情况
     *
     * @param secretNameWithType
     *            带有加密类型的密文列名
     * @return 明文的列名
     * @throws Exception
     */

    public static String getPlainName(String secretNameWithType) throws Exception {
        // 例如列名为：di_DET，将其分割为di和DET，前者是密文的列名，后者是当前列的加密方式。
        String[] temp = secretNameWithType.split("_");
        // 我们对密文的列名进行解密
        return new String(DecryptAlgorithm(temp[0]));
    }

    /**
     * 经过String getSecretTableName(String tableName)函数的处理之后，还要用这些函数<br>
     * 以telephone为例，经过这个函数的处理之后，得到的是dGVsZXBob25l，我们还需要使用后面几个getXXXTableName来改写为<br>
     * dGVsZXBob25l_DET、dGVsZXBob25l_OPE等形式、并以这种形式存放在数据库中。<br>
     *
     * @param secretName
     * @return
     */

    public static String getDETName(String secretName) {

        return secretName + "_DET";

    }

    public static String getOPEName(String secretName) {
        return secretName + "_OPE";

    }

    public static String getJOINName(String secretName) {
        return secretName + "_JOIN";

    }

    public static String getHOMName(String secretName) {
        return secretName + "_HOM";

    }

    public static String getSEARCHName(String secretName) {
        return secretName + "_SEARCH";
    }

    private static String EncryptAlgorithm(String str) {
        StringBuffer strBuffer = new StringBuffer(str).reverse();
        return strBuffer.toString();
    }

    private static String DecryptAlgorithm(String str) {
        StringBuffer strBuffer = new StringBuffer(str).reverse();
        return strBuffer.toString();
    }

}
