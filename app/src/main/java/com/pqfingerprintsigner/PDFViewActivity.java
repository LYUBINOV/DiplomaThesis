package com.pqfingerprintsigner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.github.barteksc.pdfviewer.PDFView;
import java.io.File;

public class PDFViewActivity extends AppCompatActivity
{
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfview);

        pdfView = findViewById(R.id.pdf);
        pdfView.fromFile(new File(getIntent().getStringExtra("filePath"))).load();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sign_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signer:
                Toast.makeText(getApplicationContext(), "Sign button clicked!", Toast.LENGTH_LONG).show();
                //TODO: tu zavolat nieco ako startAuth()

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
