package com.pqfingerprintsigner;

import android.database.Cursor;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created by lubor on 3. 2. 2019.
 */

public class FilesActivity extends AppCompatActivity {
    private DBCommandHandler dbCommandHandler;
    private Cipher cipher;
    private SecretKey key;
    private KeyStore keyStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbCommandHandler = new DBCommandHandler(this);
        checkGeneratedKeysInDatabase();
    }

    protected void checkGeneratedKeysInDatabase() {
        //Init cipher
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" +
                                        KeyProperties.BLOCK_MODE_CBC + "/" +
                                        KeyProperties.ENCRYPTION_PADDING_PKCS7);

            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            key = (SecretKey) keyStore.getKey(MainActivity.getKeyName(), null);
        } catch (NoSuchAlgorithmException   | NoSuchPaddingException |
                 KeyStoreException          | IOException |
                 CertificateException       | UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        Cursor cursor = dbCommandHandler.getSphincsKeys(0);

        if(cursor.getCount() == 0) {
            //TODO: import bouncyCastle.jar, generate SPHINCS keys and put them encrypted to DB
            //TODO: cipher is inited, need to know how to encrypt/decrypt sphincs keys THIS WAS LAST EDIT
            Toast.makeText(getApplicationContext(), "SPHINCS keys were generated.", Toast.LENGTH_LONG).show();

            //just for test
            try {
                Cipher cipher = FingerprintCommandHandler.getFingerprintCryptoObject().getCipher();
                SecretKey sk = FingerprintCommandHandler.getFingerprintKey();
                byte[] ivBytes = cipher.getIV();
                byte[] pub_privKeysEnc = cipher.doFinal("PUBKEY_PRIVKEY".getBytes());
//                String privKeyEnc = new String(FingerprintCommandHandler.getFingerprintCryptoObject().getCipher().doFinal("TOHO_JE_PRIVATE_KEY".getBytes()));

                String ivEnc = new String(cipher.getIV());

//                System.out.println("ENCRYPTED:\n" + pubKeyEnc + "\n" + privKeyEnc + "\n" + ivEnc);
                System.out.println("ENCRYPTED:\n" + new String(pub_privKeysEnc) + "\n" + ivEnc);

                try {
                    cipher.init(Cipher.DECRYPT_MODE, sk, new IvParameterSpec(ivBytes));

                    byte[] bytes = cipher.doFinal(pub_privKeysEnc);
                    System.out.println("DECRYPTED:\n" + new String(bytes));
                } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }

//                dbCommandHandler.insertSphincsKeys("TOHO_JE_PUBLIC_KEY","TOHO_JE_PRIVATE_KEY","TOHO_JE_IV");
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }
        }
//        else if(cursor.getCount() == 1) {
//            Toast.makeText(getApplicationContext(), "SPHINCS keys unlocked.", Toast.LENGTH_LONG).show();
//
//            cursor.moveToFirst();
//
//            String pubKeyEnc = cursor.getString(cursor.getColumnIndex(DBCommandHandler.COLUMN_PUBLIC_KEY));
//            String privKeyEnc = cursor.getString(cursor.getColumnIndex(DBCommandHandler.COLUMN_PRIVATE_KEY));
//            String ivEnc = cursor.getString(cursor.getColumnIndex(DBCommandHandler.COLUMN_INITIALIZATION_VECTOR));
//
//            if(!cursor.isClosed()) {
//                cursor.close();
//            }
//
//            System.out.println("ENCRYPTED:\n" + pubKeyEnc + "\n" + privKeyEnc + "\n" + ivEnc);
//
//            try {
//                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivEnc.getBytes()));
//
//                String pubKeyDec = Base64.encodeToString(cipher.doFinal(pubKeyEnc.getBytes()), Base64.NO_WRAP);
//                String privKeyDec = Base64.encodeToString(cipher.doFinal(privKeyEnc.getBytes()), Base64.NO_WRAP);
//                String ivDec = Base64.encodeToString(cipher.doFinal(ivEnc.getBytes()), Base64.NO_WRAP);
//
//                System.out.println("DECRYPTED:\n" + pubKeyDec + "\n" + privKeyDec + "\n" + ivDec);
//            } catch (InvalidKeyException | InvalidAlgorithmParameterException |
//                     BadPaddingException | IllegalBlockSizeException e) {
//                e.printStackTrace();
//            }
//        }
    }
}
