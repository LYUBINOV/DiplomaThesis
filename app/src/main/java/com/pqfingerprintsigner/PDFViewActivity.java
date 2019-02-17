package com.pqfingerprintsigner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class PDFViewActivity extends AppCompatActivity
{
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfview);

        pdfView = (PDFView) findViewById(R.id.pdf);
        pdfView.fromFile(new File(getIntent().getStringExtra("filePath"))).load();
    }
}
