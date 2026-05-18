package com.oddlabs.tt.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WordsEncoding {
    private static final List<String> WORDS = new ArrayList<>();
    private static final Map<String, Integer> WORD_TO_INDEX = new HashMap<>();
    private static final int BITS_PER_WORD = 10;
    private static final BigInteger MASK = BigInteger.valueOf(0x3FF);

    static {
        try (var stream = WordsEncoding.class.getResourceAsStream("dictionary"); var reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                String word = line.toUpperCase();
                WORDS.add(word);
                WORD_TO_INDEX.put(word, i);
                i++;
            }
        } catch (Exception e) {
            System.out.println("Exception during initializing WordsEncoding: " + e);
        }
    }

    public static BigInteger decode(String code) {
        BigInteger result = BigInteger.ZERO;
        String[] words = code.split(" ");
        for (String word : words) {
            String upper = word.toUpperCase();
            if (upper.isEmpty()) continue;
            Integer index = WORD_TO_INDEX.get(upper);
            if (index == null) {
                throw new IllegalArgumentException("Invalid word: " + word);
            }
            result = result.shiftLeft(BITS_PER_WORD);
            result = result.or(BigInteger.valueOf(index));
        }
        return result;
    }

    public static String encode(BigInteger code) {
        StringBuilder result = new StringBuilder();
        while (!code.equals(BigInteger.ZERO)) {
            int index = code.and(MASK).intValue();
            code = code.shiftRight(BITS_PER_WORD);
            if (code.equals(BigInteger.ZERO) && index == 0) {
                break;
            }
            if (!result.isEmpty()) {
                result.insert(0, ' ');
            }
            result.insert(0, WORDS.get(index));
        }
        return result.toString();
    }
}
