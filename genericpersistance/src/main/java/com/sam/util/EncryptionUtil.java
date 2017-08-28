package com.sam.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.*;

/**
 * Created by root on 27/8/17.
 */
public class EncryptionUtil {
    private static final Logger LOG = LoggerFactory.getLogger(EncryptionUtil.class);
    private static final Marker FATAL = MarkerFactory.getMarker("FATAL");

    private static final Cipher gratterCipherEncrypter = getCipher("AES");
    private static final Cipher gratterCipherDecrypter = getCipher("AES");
    private static final EncryptionUtil encUtil = new EncryptionUtil();
    private static final String AES_CONFIG_KEY = "aes.key";
    private static final String AES_CONFIG_KEY_RESOURCE = "aes.key.resource";
    private static final String AES_RESOURCE_KEY = "aes.resource.key";

    public static final String ALGORITHM_AES = "AES";
    private static final byte[] keyValue = new byte[] { 'T', 'h', 'i', 's', 'I', 's', 'A', 'S', 'e', 'c', 'r', 'e', 't', 'K', 'e', 'y' };

    private static final byte bufferByte = (byte) 0x99;

    static {
        initCipher(gratterCipherEncrypter, "JCEKS", "gratter", Cipher.ENCRYPT_MODE);
        initCipher(gratterCipherDecrypter, "JCEKS", "gratter", Cipher.DECRYPT_MODE);
    }

    /**
     * ************************* private methods*************************
     *
     * @param type Type of the key store
     * @return {@link KeyStore}
     */
    private static KeyStore getKeyStore(String type) {
        try {
            return KeyStore.getInstance(type);
        } catch (KeyStoreException e) {
            LOG.error("Error while getting keystore", e);
            return null;
        }
    }

    private static Cipher getCipher(String alogorithm) {
        try {
            return Cipher.getInstance(alogorithm);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("NoSuchAlgorithmException while getting cipher. Ignoring Error. Algorithm: " + alogorithm, e);
        } catch (NoSuchPaddingException e) {
            LOG.error("NoSuchPaddingException while getting cipher. Ignoring Error Algorithm: " + alogorithm, e);
        }

        return null;
    }

    private static void initCipher(Cipher cipher, String keyStoreType, String alias, int mode) {
        try {

            final String keyFile = DomainUtil.getConfig(AES_CONFIG_KEY);
            final Boolean isResource = Boolean.valueOf(DomainUtil.getConfig(AES_CONFIG_KEY_RESOURCE, "true"));
            final InputStream is = isResource ? EncryptionUtil.class.getResourceAsStream(keyFile) : new FileInputStream(keyFile);
            final char[] password = (isResource ? DomainUtil.getConfig(AES_RESOURCE_KEY) : System.getProperty("keyFileKey")).toCharArray();
            final KeyStore keyStore = getKeyStore(keyStoreType);
            keyStore.load(is, password);
            cipher.init(mode, new SecretKeySpec(keyStore.getKey(alias, password).getEncoded(), "AES"));
        } catch (Exception e) {
            LOG.error(FATAL, "Key for decryption failed to load. Forcing Exit !!!", e);
            System.exit(45);
        }
    }

    /*
    * The below two methods are used to authenticate icici DMA and branch employees, via iciciTablet to APS service
    *
    * Making the public method specific to be used only for ICICI APS Purpose.
    * */
    public static String encryptICICIAPSPassword(String userId, String password) throws Exception {
        String encryptedPassword;
        SecretKey secretKey = getEncryptionKey(userId);
        byte[] buffer;
        byte[] cipherText;

        Cipher cipher = Cipher.getInstance("DESede");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        buffer = password.getBytes();
        cipherText = cipher.doFinal(buffer);

        BASE64Encoder base64 = new BASE64Encoder();
        encryptedPassword = base64.encode(cipherText);

        return encryptedPassword;
    }

    private static SecretKey getEncryptionKey(String userId) throws Exception {
        byte[] keyBytes = new byte[24];
        byte[] tmpBytes = userId.getBytes();
        for (int i = 0; i < 24; i++) {
            if (i < tmpBytes.length) {
                keyBytes[i] = tmpBytes[i];
            } else {
                keyBytes[i] = bufferByte;
            }
        }

        DESedeKeySpec desKeySpec = new DESedeKeySpec(keyBytes);
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("DESede");
        return keyFac.generateSecret(desKeySpec);
    }

    /**
     * **************************Forcing singleton**************************
     */
    private EncryptionUtil() {

    }

    public static EncryptionUtil getInstance() {
        return encUtil;
    }

    public byte[] encrypt(byte[] decryptedBytes) throws Exception {
        return gratterCipherEncrypter.doFinal(decryptedBytes);
    }

    public byte[] decrypt(byte[] encryptedBytes) throws Exception {
        return gratterCipherDecrypter.doFinal(encryptedBytes);
    }
    public String encrypt(String data) throws Exception{
        return new BASE64Encoder().encode(encrypt(data.getBytes("UTF-8")));
    }

    public String decrypt(String data) throws Exception{
        return new String(decrypt(new BASE64Decoder().decodeBuffer(data)));
    }

    public String urlSafeEncrypt(String data) throws Exception{
        return URLEncoder.encode(encrypt(data), "UTF-8");
    }

    public static String decode(final String encryptedValue, final String algorithm){
        String decryptedValue = null;
        try{
            final Key key = generateKey(algorithm);

            final Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, key);
            final byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedValue);
            final byte[] decValue = cipher.doFinal(decordedValue);

            decryptedValue = new String(decValue);
        }catch(Exception e){
            LOG.error("Exception while decrypting key", e);
        }

        return decryptedValue;

    }


    private static Key generateKey(String algorithm) {
        return new SecretKeySpec(keyValue, algorithm);
    }

    public static String calculateHash(String type, String str) {
        byte[] hashseq = str.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance(type);
            algorithm.reset();
            algorithm.update(hashseq);
            byte messageDigest[] = algorithm.digest();


            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append("0");
                hexString.append(hex);
            }

        } catch (NoSuchAlgorithmException nsae) {
            LOG.error("NoSuchAlgorithmException in calculation hash. Type: " + type, nsae);
        }

        return hexString.toString();


    }
}
