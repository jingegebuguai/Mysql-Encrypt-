package com.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

/**
 * <h3>密钥管理器</h3><br>
 * 这个类用于生成密钥，并将密钥保存在文件中，以便后续使用。
 *
 */

public class KeyManager {

    /**
     * 这个函数用于获取BlowFish算法需要的密钥<br>
     * <li>1.首先检测密钥文件是否存在，如果不存在则生成一个新密钥，并使用对象序列化的方式保存在key.dat文件中；<br>
     * <li>2.如果密钥文件key.dat文件已经存在，则直接读取文件中的密钥即可.<br>
     *
     * @return key 密钥
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws ClassNotFoundException
     */

    public static Key blowfishKey() throws GeneralSecurityException, IOException, ClassNotFoundException {

        File f = new File("key.dat");
        // 如果密钥文件不存在说明还没有密钥，需要生成一个密钥
        if (!f.exists()) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("Blowfish");
            keyGenerator.init(128);
            Key key = keyGenerator.generateKey();
            FileOutputStream fileOut = new FileOutputStream("key.dat");
            // 以对象序列化的方式存储在key.dat中
            ObjectOutputStream objOutput = new ObjectOutputStream(fileOut);
            objOutput.writeObject(key);
            objOutput.close();
            return key;
        } else {
            // 如果文件存在，则表示已经有密钥，我们要读取这个密钥
            FileInputStream fileIn = new FileInputStream("key.dat");
            // 获取key.dat中的密钥，并将序列化数据重装为对象key。
            ObjectInputStream objInput = new ObjectInputStream(fileIn);
            Key key = (Key) objInput.readObject();
            objInput.close();
            return key;
        }

    }

    public static Key generateDETKey(String password, String columnName, String onionType)
            throws NoSuchAlgorithmException {
        byte[] colNameBytes = columnName.getBytes();
        // 新生成的16位bytes数组，初始为masterKey的bytes，之后用于存储计算后的key
        byte[] keyBytes = "1234567812345678".getBytes();
        // 计算生成多少组16位的byte数组
        int row = (int) Math.ceil((double) colNameBytes.length / 16);
        byte[][] bytesArray = new byte[row][16];
        // 填充生成byte二维数组
        for (int i = 0, k = 0; i < row; i++) {
            for (int j = 0; j < 16; j++) {
                // 若colName长度不足，则从头开始填充

                if (k == colNameBytes.length) {
                    k = 0;
                }
                bytesArray[i][j] = colNameBytes[k++];
            }
        }
        // ������
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < 16; j++) {
                keyBytes[j] = (byte) (keyBytes[j] ^ bytesArray[i][j]);
            }
        }
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        return key;

    }

    public static double[] generateOpeKey(double sens) {
        double[] opeKey = new double[3];
        opeKey[0] = Math.random() * 1000;
        opeKey[1] = Math.random() * 1000;
        // opeKey的第三个参数代表了sens
        opeKey[2] = sens;
        return opeKey;

    }

    public static double[][] generateHomKey() {
        double[][] homKey = new double[5][3];
        for (int i = 0; i < 5 - 1; i++) {
            // 如果是第一个k密钥，让其大于零
            if (i == 0) {
                // 如果是第一个k密钥，让其大于零
                homKey[i][0] = Math.random() * 20;
            } else {
                homKey[i][0] = Math.random() * 20 - 10;
            }

            // 对于si:s1+...s(n-2) != 0,s(n-1) !=0，我们采取一个简单的解决办法，让s始终为正数
            homKey[i][1] = Math.random() * 10;


            // 对于t的约束条件只有一个：kn+sn+tn !=
            // 0,而t1...t(n-1)没有要求,所以我们暂定t的范围是：(200~500)之间的随机数

            homKey[i][2] = Math.random();
        }
        // 注意：kn+sn+tn != 0
        homKey[4][0] = Math.random() * 20 - 10;
        homKey[4][1] = Math.random() * 10;
        // 为了满足kn+sn+tn != 0的条件，我们让tn由另外两个数计算得到,在这里kn+sn+tn =
        // 1000，我们还可以让kn+sn+tn = randomNumber
        homKey[4][2] = 10.0 - homKey[4][0] - homKey[4][1];
        // System.out.println("已使用KeyManager中的generateHomKey()方法生成密钥数组！");
        return homKey;
    }
}
