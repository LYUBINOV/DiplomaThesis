package com.pqfingerprintsigner;

import android.os.Bundle;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

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

        if(dbCommandHandler.getSphincsKeys(0).getCount() == 0) {
            //TODO: import bouncyCastle.jar, generate SPHINCS keys and put them encrypted to DB
            //TODO: cipher is inited, need to know how to encrypt/decrypt sphincs keys THIS WAS LAST EDIT
            Toast.makeText(getApplicationContext(), "SPHINCS keys were generated.", Toast.LENGTH_LONG).show();
        }
        else if(dbCommandHandler.getSphincsKeys(0).getCount() == 1) {
            Toast.makeText(getApplicationContext(), "SPHINCS keys unlocked.", Toast.LENGTH_LONG).show();
        }
    }
}
