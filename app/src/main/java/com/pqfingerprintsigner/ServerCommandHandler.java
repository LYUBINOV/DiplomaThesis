package com.pqfingerprintsigner;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.pqc.crypto.MessageSigner;
import org.bouncycastle.pqc.crypto.sphincs.SPHINCS256KeyGenerationParameters;
import org.bouncycastle.pqc.crypto.sphincs.SPHINCS256KeyPairGenerator;
import org.bouncycastle.pqc.crypto.sphincs.SPHINCS256Signer;
import org.bouncycastle.pqc.crypto.sphincs.SPHINCSPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.sphincs.SPHINCSPublicKeyParameters;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created by lubor on 3. 2. 2019.
 */

public class ServerCommandHandler {
    private DBCommandHandler dbCommandHandler;
    private Cursor dbCursor;

    public void getDatabaseCursor(Context context) {
        this.dbCommandHandler = new DBCommandHandler(context);
        this.dbCursor = this.dbCommandHandler.getSphincsKeys(0); //Unique ID for each key
    }

    public void checkGeneratedKeysInDatabase(Context context, File targetFile) {
        if(this.dbCursor.getCount() == 0) {
            //KeyGen
            Map<String, byte[]> sphincsKeysDatas = this.generateSphincsKeys();
            Toast.makeText(context, "SPHINCS keys were generated!", Toast.LENGTH_LONG).show();

            try {
                //KeyEnc
                Cipher cipher = FingerprintCommandHandler.getFingerprintCryptoObject().getCipher();

                String concatPubPrivKeys = new String(sphincsKeysDatas.get("publicKey"))
                                         + " "
                                         + new String(sphincsKeysDatas.get("privateKey"));

                byte[] pub_privKeysEncrypted = cipher.doFinal(concatPubPrivKeys.getBytes());
                Toast.makeText(context, "SPHINCS keys were encrypted!", Toast.LENGTH_LONG).show();

                //KeyInsert
                this.dbCommandHandler.insertSphincsKeys(new String(pub_privKeysEncrypted), new String(cipher.getIV()));
                Toast.makeText(context, "SPHINCS keys were inserted to database!", Toast.LENGTH_LONG).show();

                //Sign and send
                if (signAndSend(sphincsKeysDatas.get("publicKey"), sphincsKeysDatas.get("privateKey"), this.fullyReadFileToBytes(targetFile))) {
                    Toast.makeText(context, "Document was signed!", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(context, "ERROR IN SIGNING DOCUMENT WORKFLOW!", Toast.LENGTH_LONG).show();
                }
            }
            catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }
        }
        else if(this.dbCursor.getCount() == 1) {
            dbCursor.moveToFirst();

            //KeyGet
            String pub_privKeysEnc = this.dbCursor.getString(this.dbCursor.getColumnIndex(DBCommandHandler.COLUMN_PUBLIC_PRIVATE_KEYS));
            String iv = this.dbCursor.getString(this.dbCursor.getColumnIndex(DBCommandHandler.COLUMN_INITIALIZATION_VECTOR));
            Toast.makeText(context, "SPHINCS keys were obtained!", Toast.LENGTH_LONG).show();

            if(!this.dbCursor.isClosed()) {
                this.dbCursor.close();
            }

            try {
                //DecKey
                Cipher cipher = FingerprintCommandHandler.getFingerprintCryptoObject().getCipher();
                SecretKey secretKey = FingerprintCommandHandler.getFingerprintKey();

                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv.getBytes()));

                byte[] decryptedPub_Priv = cipher.doFinal(pub_privKeysEnc.getBytes());
                String[] pub_privKeys = new String(decryptedPub_Priv).split(" ");
                String publicKey = pub_privKeys[0];
                String privateKey = pub_privKeys[0];
                Toast.makeText(context, "SPHINCS keys were decrypted!", Toast.LENGTH_LONG).show();

                //Sign and send
                if(signAndSend(publicKey.getBytes(), privateKey.getBytes(), this.fullyReadFileToBytes(targetFile))) {
                    Toast.makeText(context, "Document was signed!", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(context, "ERROR IN SIGNING DOCUMENT WORKFLOW!", Toast.LENGTH_LONG).show();
                }
            }
            catch (InvalidKeyException | InvalidAlgorithmParameterException |
                   BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, byte[]> generateSphincsKeys() {
        //SPHINCS TESTING - implementacia https://github.com/bcgit/bc-java/tree/master/core/src/main/java/org/bouncycastle/pqc/crypto/sphincs
        SPHINCS256KeyPairGenerator generator = new SPHINCS256KeyPairGenerator();
        generator.init(new SPHINCS256KeyGenerationParameters(new SecureRandom(), new SHA3Digest(256)));

        AsymmetricCipherKeyPair kp = generator.generateKeyPair();
        final SPHINCSPublicKeyParameters pub = (SPHINCSPublicKeyParameters)kp.getPublic();
        final SPHINCSPrivateKeyParameters priv = (SPHINCSPrivateKeyParameters)kp.getPrivate();

        return new HashMap<String, byte[]>(){
                        {
                            put("publicKey", pub.getKeyData());
                            put("privateKey", priv.getKeyData());
                        }
                    };
    }

    private boolean signAndSend(byte[] _publicKey, byte[] _privateKey, byte[] document) {
        try {
            //SPHINCSPublicKeyParameters publicKey = new SPHINCSPublicKeyParameters(_publicKey);
            SPHINCSPrivateKeyParameters privateKey = new SPHINCSPrivateKeyParameters(_privateKey);

            //Sign //https://github.com/bcgit/bc-java/tree/master/core/src/main/java/org/bouncycastle/pqc/crypto
            MessageSigner sphincsSigner = new SPHINCS256Signer(new SHA3Digest(256), new SHA3Digest(512)); //
            sphincsSigner.init(true, privateKey);
            byte[] signature = sphincsSigner.generateSignature(document);

            //verif
            //sphincsSigner.init(false, pub);
            //System.out.println(sphincsSigner.verifySignature("helloWorldq".getBytes(), signature));

            //TODO: dorobit server
            this.sendPost(new String(_publicKey), new String(signature), new String(document));

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private byte[] fullyReadFileToBytes(File f) {
        int size = (int) f.length();

        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            int read = fis.read(bytes, 0, size);

            if (read < size) {
                int remain = size - read;

                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);

                    remain -= read;
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            try {
                fis.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bytes;
    }

    private void sendPost(final String publicKey, final String signature, final String document) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://[ip]:[port]");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("publicKey", publicKey);
                    jsonParam.put("signature", signature);
                    jsonParam.put("document", document);

                    Log.i("JSON", jsonParam.toString());

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}
