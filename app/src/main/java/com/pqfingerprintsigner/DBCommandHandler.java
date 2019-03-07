package com.pqfingerprintsigner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBCommandHandler extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "PQCryptoSigner.db";
    public static final String CONTACTS_TABLE_NAME = "SPHINCS_KEYS";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_PUBLIC_PRIVATE_KEYS = "publicKey_privateKey";
    public static final String COLUMN_INITIALIZATION_VECTOR = "initializationVector";

    public DBCommandHandler(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table SPHINCS_KEYS (id integer primary key, publicKey_privateKey text, initializationVector text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists SPHINCS_KEYS");
        onCreate(db);
    }

    public boolean insertSphincsKeys(String publicKey_privateKey, String initializationVector) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(this.COLUMN_ID, 0);
        contentValues.put(this.COLUMN_PUBLIC_PRIVATE_KEYS, publicKey_privateKey);
        contentValues.put(this.COLUMN_INITIALIZATION_VECTOR, initializationVector);

        db.insert(this.CONTACTS_TABLE_NAME, null, contentValues);

        return true;
    }

    public Cursor getSphincsKeys(Integer id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from SPHINCS_KEYS where id=" + id + "", null);

        return res;
    }

    public boolean regenerateSphincsKeys(Integer id, String publicKey_privateKey, String initializationVector) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(this.COLUMN_PUBLIC_PRIVATE_KEYS, publicKey_privateKey);
        contentValues.put(this.COLUMN_INITIALIZATION_VECTOR, initializationVector);

        SQLiteDatabase db = this.getWritableDatabase();
        db.update(this.DATABASE_NAME, contentValues, "id = ? ", new String[]{Integer.toString(id)});

        return true;
    }
}
