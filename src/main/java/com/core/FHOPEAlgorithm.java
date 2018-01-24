package com.core;

import java.util.Random;


public class FHOPEAlgorithm {
    public static final int CMAX = 0;
    public static final int CMIN = 1;

    /**
     * keyGenerate用于产生算法密钥
     *
     * @param keyNum
     *            是密钥的个数，每个密钥有三个子密钥组:[(k1,p1,r1),(k2,p2,r2),...]
     * @return 返回一二维数组，用于保存密钥
     */

    public double[][] keyGenerate(int keyNum) {
        double[][] keys = new double[keyNum][2];
        Random rnd = new Random();
        keys[0][0] = rnd.nextDouble() * 10000;
        keys[1][0] = -1 * keys[0][0];
        for (int i = 0; i < keyNum; i++) {
            if (i == 1 || i == 0) {
                keys[i][1] = rnd.nextDouble() * 10000;
            } else {
                if (i == keyNum - 1) {
                    keys[i][0] = rnd.nextDouble() * 10000;
                    keys[i][1] = rnd.nextDouble() * 10000 - 5000;
                }
                keys[i][0] = rnd.nextDouble() * 10000 - 5000;
                keys[i][1] = rnd.nextDouble() * 10000 - 5000;
            }
        }
        return keys;
    }


    /**
     * noiseGenerate用于产生随机噪声:[(r1,p1),(r2,p2),(r3,p3),...]
     *
     * @param noiseNum
     *            随机噪声的个数
     * @param sens
     *            数据的精度，默认1E-8
     * @param keys
     *            密钥，为了实现保序，要对部分随机噪声的范围进行限定，这个过程要密钥
     * @return 返回个二维数组，存储了随机噪声
     */

    public double[][] noiseGenerate(int noiseNum, double sens, double[][] keys) {
        double[][] noises = new double[noiseNum][2];
        Random rnd = new Random();
        double condition_Of_P1 = -1 * keys[1][0] * keys[0][1] * sens;
        double condition_Of_R1 = keys[0][0] * keys[0][1] * sens;
        double condition_Of_Pn = keys[0][0] * keys[0][1] * sens;
        double condition_Of_Rn = keys[noiseNum - 1][0] * keys[0][1] * sens;
        // P1
        noises[0][0] = (rnd.nextDouble() + 1) * condition_Of_P1;
        // R1
        noises[0][1] = (rnd.nextDouble() - 1) * condition_Of_R1;
        // Pn
        noises[noiseNum - 1][0] = (rnd.nextDouble() - 1) * condition_Of_Pn;
        // Rn
        noises[noiseNum - 1][1] = rnd.nextDouble() * condition_Of_Rn;
        for (int i = 1; i < noiseNum - 1; i++) {
            // Pi
            noises[i][0] = rnd.nextDouble() * 1E+7 - 0.5 * 1E+7;
            // Ri
            noises[i][1] = rnd.nextDouble() * 1E+7 - 0.5 * 1E+7;
        }
        return noises;
    }


    /**
     * 加密函数
     *
     * @param value
     *            明文,double类型
     * @param keys
     *            密钥
     * @param noises
     *            随机噪声
     * @param encNum
     *            密文分片的个数,也决定了密钥的个数和随机噪声的个数.
     * @return 加密后的密文是一个一维数组
     */
    public double[] encryptFunc(double value, double[][] keys, double[][] noises, int encNum) {
        double[] ciper = new double[encNum];
        double[] k = new double[encNum];
        double[] s = new double[encNum];
        double[] p = new double[encNum];
        double[] r = new double[encNum];
        for (int i = 0; i < encNum; i++) {
            k[i] = keys[i][0];
            s[i] = keys[i][1];
            p[i] = noises[i][0];
            r[i] = noises[i][1];
        }
        // C1:
        ciper[0] = k[0] * (s[0] * value + (p[0] / k[1]) - (r[encNum - 1] / k[encNum - 1])) + r[0] - p[encNum - 1];
        ciper[encNum - 1] = k[encNum - 1]
                * (s[encNum - 1] * value + (p[encNum - 1] / k[1]) - (r[encNum - 2] / k[encNum - 2])) + r[encNum - 1]
                - p[encNum - 2];
        for (int i = 1; i < encNum - 1; i++) {
            ciper[i] = k[i] * (s[i] * value + (p[i] / k[i + 1]) - (r[i - 1] / k[i - 1])) + r[i] - p[i - 1];
        }
        return ciper;
    }


    /**
     * 解密函数
     *
     * @param ciper
     *            要解密的密文，其形式为一维数组
     * @param keys
     *            解密的密文
     * @param encNum
     *            密文分片的个数
     * @return 返回double型的明文
     */

    public double decryptFunc(double[] ciper, double[][] keys, int encNum) {
        double[] k = new double[encNum];
        double[] s = new double[encNum];
        for (int i = 0; i < encNum; i++) {
            k[i] = keys[i][0];
            s[i] = keys[i][1];
        }
        double sum_S = 0;
        for (int i = 0; i < encNum; i++) {
            sum_S += s[i];
        }

        double[] fun_dec = new double[encNum];
        for (int i = 0; i < encNum; i++) {
            fun_dec[i] = 1.0 / (k[i] * sum_S);
        }

        double result_dec = 0;
        for (int i = 0; i < encNum; i++) {
            result_dec += (ciper[i] * fun_dec[i]);
        }
        return result_dec;
    }

    /**
     * 额外的随机因子，弥补原有算法中的漏洞，提高安全
     *
     * @return 理论上返回一个无穷实数空间中的随机数
     */

    public double extraNoiseGenerate() {
        Random rnd = new Random();
        return rnd.nextDouble() * 1E+10 - 0.5 * 1E+10;
    }

}
