package com.pqfingerprintsigner;

import android.database.Cursor;
import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.io.IOException;
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
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);

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

            try {
                cipher.init(Cipher.ENCRYPT_MODE, key);

                String pubKeyEnc = new String(cipher.doFinal("PUB_KEY".getBytes()));
                String privKeyEnc = new String(cipher.doFinal("PRIV_KEY".getBytes()));
                String iv = new String(cipher.getIV());

                dbCommandHandler.insertSphincsKeys(pubKeyEnc, privKeyEnc, iv);
                System.out.println("ENCRYPTED:\n" + pubKeyEnc + "\n" + privKeyEnc + "\n" + iv);
            }
            catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }

//            Toast.makeText(getApplicationContext(), "SPHINCS keys were generated.", Toast.LENGTH_LONG).show();
        }
        else if(cursor.getCount() == 1) {
            cursor.moveToFirst();

            String pubKeyEnc = cursor.getString(cursor.getColumnIndex(dbCommandHandler.COLUMN_PUBLIC_KEY));
            String privKeyEnc = cursor.getString(cursor.getColumnIndex(dbCommandHandler.COLUMN_PRIVATE_KEY));
            String iv = cursor.getString(cursor.getColumnIndex(dbCommandHandler.COLUMN_INITIALIZATION_VECTOR));

            System.out.println("ENCRYPTED:\n" + pubKeyEnc + "\n" + privKeyEnc + "\n" + iv);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
            try {
                cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

                String pubKeyDec = new String(cipher.doFinal(pubKeyEnc.getBytes()));
                String privKeyDec = new String(cipher.doFinal(privKeyEnc.getBytes()));

                System.out.println("DECRYPTED:\n" + pubKeyDec + "\n" + privKeyDec + "\n" + iv);
            }
            catch (InvalidKeyException | InvalidAlgorithmParameterException |
                    BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }

//            Toast.makeText(getApplicationContext(), "SPHINCS keys unlocked.", Toast.LENGTH_LONG).show();
        }
    }
}
