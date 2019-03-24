package com.pqfingerprintsigner;

import android.app.Activity;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import java.io.File;

import javax.crypto.SecretKey;

/**
 * Created by lubor on 3. 2. 2019.
 */

public class FingerprintCommandHandler extends FingerprintManager.AuthenticationCallback
{
    private Context context;
    private static FingerprintManager.CryptoObject fingerprintCryptoObject;
    private static SecretKey fingerprintKey;
    private File targetFile;

    private ServerCommandHandler serverCommandHandler;

    public FingerprintCommandHandler(Context mContext) {
        context = mContext;
    }

    public void startAuth(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject, SecretKey key, File targetFile) {
        CancellationSignal cancellationSignal = new CancellationSignal();

        // if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
        //     return;
        // }

        fingerprintCryptoObject = cryptoObject;
        fingerprintKey = key;

        this.targetFile = targetFile;

        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        this.update("Fingerprint Authentication error\n" + errString, false);
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        this.update("Fingerprint Authentication help\n" + helpString, false);
    }

    @Override
    public void onAuthenticationFailed() {
        this.update("Fingerprint Authentication failed.", false);
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        this.update("Fingerprint Authentication succeeded.", true);

        //WF: wf po prilozeni prsta
        signDocument();
    }

    public void update(String e, Boolean success){
        TextView textView = (TextView) ((Activity)context).findViewById(R.id.errorText);

        textView.setText(e);

        if(success){
            textView.setTextColor(ContextCompat.getColor(context,R.color.colorPrimaryDark));
        }
    }

    public static FingerprintManager.CryptoObject getFingerprintCryptoObject() {
        return fingerprintCryptoObject;
    }

    public static SecretKey getFingerprintKey() {
        return fingerprintKey;
    }

    private void signDocument() {
        ServerCommandHandler serverCommandHandler = new ServerCommandHandler();

        serverCommandHandler.getDatabaseCursor(context); //Init
        serverCommandHandler.checkGeneratedKeysInDatabase(this.targetFile);
    }
}
