package com.sunx.utils;

import java.security.MessageDigest;

/**
 * Created by sunx on 2017/5/9.
 */
public class MD5 {
    /**
     * 根据给定的字符串进行加密
     * @param plainText
     * @return
     * @throws Exception
     */
    public static String convert(String plainText) throws Exception{
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(plainText.getBytes());
        byte b[] = md.digest();
        int i;
        StringBuffer buf = new StringBuffer("");
        for (int offset = 0; offset < b.length; offset++) {
            i = b[offset];
            if (i < 0) {
                i += 256;
            }
            if (i < 16) {
                buf.append("0");
            }
            buf.append(Integer.toHexString(i));
        }
        String str = buf.toString();
        return str;
    }
}
