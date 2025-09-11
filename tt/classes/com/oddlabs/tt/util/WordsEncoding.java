package com.oddlabs.tt.util;

import com.oddlabs.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public final class WordsEncoding {
    private static ArrayList list;
    private static HashMap inverted;
    private static final int MAX_VALUE = 1024;

    static {
        try {
            URL url = Utils.tryMakeURL("/dictionary");
            list = new ArrayList();
            inverted = new HashMap();
            BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                line = line.toUpperCase();
                list.add(line);
                inverted.put(line, i);
                i++;
            }
        } catch (Exception e) {
            System.out.println("Exception during initializing WorksEncoding: " + e);
        }
    }

    public static BigInteger decode(String code) throws Exception {
        BigInteger result = BigInteger.ZERO;
        BigInteger mask = BigInteger.valueOf(0x3FF);
        String[] words = code.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toUpperCase();
            if (word != "") {
                if (!inverted.containsKey(word)) {
                    throw new Exception("Invalid word " + word);
                }
                int index = (int) inverted.get(word);
                result = result.shiftLeft(10);
                result = result.or(BigInteger.valueOf(index));
            }
        }
        return result;
    }

    public static String encode(BigInteger code) {
        String result = "";
        BigInteger mask = BigInteger.valueOf(0x3FF);
        while (code != BigInteger.ZERO) {
            int index = code.and(mask).intValue();
            code = code.shiftRight(10);
            if (code == BigInteger.ZERO && index == 0) {
                break;
            }
            if (result != "") {
                result = " " + result;
            }
            result = (String) list.get(index) + result;
        }
        return result;
    }
}
