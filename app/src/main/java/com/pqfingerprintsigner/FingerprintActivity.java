package com.pqfingerprintsigner;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class FingerprintActivity extends AppCompatActivity {
    private KeyStore keyStore;
    private static final String KEY_NAME = "SPHINCS"; //TODO: vymyslet rozumny nazov pre key, //popripade .getApplicationName()
    private SecretKey key;
    private Cipher cipher;

    private FingerprintCommandHandler fingerprintCommandHandler;

    private TextView errorText;

    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        this.errorText = (TextView) findViewById(R.id.errorText);
        this.filePath = getIntent().getStringExtra("filePath");

        checkPermissions();
    }

    protected void checkPermissions() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        if(!fingerprintManager.isHardwareDetected()){
            errorText.setText("Your Device does not have a Fingerprint Sensor!");

            //App killing if needed
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
        else {
            if (!Objects.equals(ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT), PackageManager.PERMISSION_GRANTED)) {
                errorText.setText("Fingerprint authentication permission not enabled!");
            }
            else {
                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    errorText.setText("Register at least one fingerprint in Settings!");
                }
                else {
                    if (!keyguardManager.isKeyguardSecure()) {
                        errorText.setText("Lock screen security not enabled in Settings!");
                    }
                    else {
                        try {
                            keyStore = KeyStore.getInstance("AndroidKeyStore");
                            keyStore.load(null);

                            //First time started app = the key is not exists in keystore
                            if(!keyStore.containsAlias(KEY_NAME)) {
                                generateKey();
                            }
                        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
                            e.printStackTrace();
                        }

                        if (cipherInit()) {
                            FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                            fingerprintCommandHandler = new FingerprintCommandHandler(this);
                            fingerprintCommandHandler.startAuth(fingerprintManager, cryptoObject, key, new File(this.filePath));
                        }
                        else {
                        	//App killing if needed - obj sa nepodarilo inicializovat
            				android.os.Process.killProcess(android.os.Process.myPid());
            				System.exit(1);
                        }
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void generateKey() {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get KeyGenerator instance!", e);
        }

        try {
            keyGenerator.init(
                    new KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                            .setUserAuthenticationRequired(false) //TODO: toto nejak ocheckovat, false vraj neodporucaju, ale inac nejde pouzivat na roznych aktivitach
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                            .setKeySize(256)
                            .build()
            );

            keyGenerator.generateKey();
        }
        catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher!", e);
        }

        try {
            key = (SecretKey) keyStore.getKey(KEY_NAME, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return true;
        }
        catch (KeyPermanentlyInvalidatedException e) {
            return false;
        }
        catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to init Cipher!", e);
        }
    }

    public static String getKeyName() {
        return KEY_NAME;
    }
}
