package com.oddlabs.registration;

import com.oddlabs.util.KeyManager;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public final class RegistrationKey {
    private static final int STRIPPED_LENGTH = 16;
    public static final int LENGTH = STRIPPED_LENGTH + 3;
    public static final String CHAR_TO_WORD = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final String LOWER_CASE_CHARS = "abcdefghjklmnpqrstuvwxyz";
    public static final String SEPARATOR = "-";
    public static final String ALLOWED_CHARS = CHAR_TO_WORD + SEPARATOR + LOWER_CASE_CHARS;

    public static PublicKey loadPublicKey() throws Exception {
        URL key_url = Utils.tryMakeURL("/" + RegServiceInterface.PUBLIC_KEY_FILE);
        byte[] encoded_key = Utils.tryLoadObject(key_url);
        return KeyManager.readPublicKey(encoded_key, RegServiceInterface.KEY_ALGORITHM);
    }

    public static boolean verify(PublicKey public_key, @NonNull SignedObject signed_object) throws GeneralSecurityException {
        return signed_object.verify(public_key, Signature.getInstance(RegServiceInterface.SIGN_ALGORITHM));
    }

    private static int computeShifting() {
        return (int) (Math.log(CHAR_TO_WORD.length()) / Math.log(2));
    }

    public static @NonNull BigInteger parseBits(@NonNull String str) throws RegistrationKeyFormatException {
        byte[] index = new byte[1]; // must be two bytes, to avoid negative values
        BigInteger result = BigInteger.ZERO;
        int shifting = computeShifting();
        int accum_shifting = 0;
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            index[0] = (byte) CHAR_TO_WORD.indexOf(c);
            if (index[0] == -1)
//				throw new RegistrationKeyFormatException(c + " is not a valid character");
                throw new RegistrationKeyFormatException(RegistrationKeyFormatException.TYPE_INVALID_CHAR, c);
            BigInteger index_big = new BigInteger(index);
            result = result.or(index_big.shiftLeft(accum_shifting));
            accum_shifting += shifting;
        }
        return result;
    }

    public static @NonNull String createString(@NonNull BigInteger val) throws NumberFormatException {
        String result = "";
        int shifting = computeShifting();
        BigInteger filter = new BigInteger(new byte[]{(byte) (CHAR_TO_WORD.length() - 1)});
        for (int accum_shifting = 0; accum_shifting < val.bitLength(); accum_shifting += shifting) {
            int char_index = val.shiftRight(accum_shifting).and(filter).intValue();
            result = CHAR_TO_WORD.charAt(char_index) + result;
        }
        return result;
    }

    private static byte[] splitToBytes(long l) {
        byte[] bytes = new byte[8]; // One superfluous byte to avoid signedness
        int accum_shifting = 64;
        int index = 0; // Start at first byte and fill the next 8
        while (accum_shifting >= 8) {
            accum_shifting -= 8;
            byte b = (byte) ((l >>> accum_shifting) & 0xFF);
            bytes[index++] = b;
        }
        return bytes;
    }

    public static @NonNull String normalize(String key_str) throws RegistrationKeyFormatException {
        return encode(decode(key_str));
    }

    public static long decode(String key_str) throws RegistrationKeyFormatException {
        key_str = key_str.toUpperCase();
        key_str = key_str.replaceAll("-", "");
        if (key_str.length() != STRIPPED_LENGTH)
//			throw new RegistrationKeyFormatException("The specified key must be " + STRIPPED_LENGTH + " characters, excluding the dashes.");
            throw new RegistrationKeyFormatException(RegistrationKeyFormatException.TYPE_INVALID_LENGTH, STRIPPED_LENGTH);
        BigInteger key_code_crc = parseBits(key_str);
        long key_code = key_code_crc.longValue();
        BigInteger crc = computeChecksum(key_code);
        BigInteger encoded_crc = key_code_crc.shiftRight(64);
        if (!crc.equals(encoded_crc))
//			throw new RegistrationKeyFormatException("The key does not appear to be valid.");
            throw new RegistrationKeyFormatException(RegistrationKeyFormatException.TYPE_INVALID_KEY);
        return key_code;
    }

    private static @NonNull BigInteger computeChecksum(long key) {
        Checksum crc = new CRC32();
        byte[] key_bytes = splitToBytes(key);
        crc.update(key_bytes, 0, key_bytes.length);
        long crc_val = crc.getValue() & 0xFFFF;
        return new BigInteger(splitToBytes(crc_val));
    }

    public static @NonNull String encode(long key_code) {
        key_code = key_code & 0x7fffffffffffffffL;
        BigInteger key_big = new BigInteger(splitToBytes(key_code));
        BigInteger crc = computeChecksum(key_code);
        key_big = key_big.or(crc.shiftLeft(64));
        int mask = CHAR_TO_WORD.length() - 1;
        int shifting = computeShifting();
        StringBuilder encoded_key = new StringBuilder();
        while (encoded_key.length() < STRIPPED_LENGTH) {
            int index = key_big.intValue() & mask;
            char c = CHAR_TO_WORD.charAt(index);
            encoded_key.append(c);
            key_big = key_big.shiftRight(shifting);
        }
        encoded_key.reverse();
        encoded_key.insert(12, SEPARATOR);
        encoded_key.insert(8, SEPARATOR);
        encoded_key.insert(4, SEPARATOR);
        return encoded_key.toString();
    }

    // test
    void main() {
        //System.out.println(Long.toHexString(decode(args[0])));
        //System.out.println(encode(Long.parseLong(args[0])));
        //System.out.println("checksum: " + computeChecksum(Long.parseLong(args[0])));
        int TEST_LENGTH = 1000000;
        Random r = new Random();
        for (int i = 0; i < TEST_LENGTH; i++) {
            long key = r.nextLong() & 0x7fffffffffffffffL;
            test(key);
        }
    }

    private static void test(long key) {
        String key_encoded = encode(key);
        long key_decoded = decode(key_encoded);
        if (key_decoded != key)
            throw new RuntimeException("Test failed, key_decoded = " + key_decoded + " != key = " + key);
//System.out.println("key = " + key + " | key_decoded = " + key_decoded + " | key_encoded = " + key_encoded);
    }
}
