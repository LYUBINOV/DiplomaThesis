package com.pqfingerprintsigner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lubor on 3. 2. 2019.
 */

public class MainActivity extends AppCompatActivity {
    private Map<String, String> pdfFiles;
    private ListView listPdfsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listFiles();
        listPdfsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String pathToChosenItem = pdfFiles.get((String) parent.getItemAtPosition(position).toString());

                Intent intent = new Intent(MainActivity.this, PDFViewActivity.class);

                intent.putExtra("filePath", pathToChosenItem);
                startActivity(intent);
            }
        });
    }

    protected void listFiles() {
        pdfFiles = new HashMap<>();
        searchPdfs(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));

        ArrayAdapter adapter = new ArrayAdapter<>(this, R.layout.listview_format, new ArrayList<>(pdfFiles.keySet()));

        listPdfsView = (ListView) findViewById(R.id.mobile_list);
        listPdfsView.setAdapter(adapter);
    }

    public void searchPdfs(File dir) {
        String pdfPattern = ".pdf";
        File fileList[] = dir.listFiles();

        if (fileList != null) {
            for (int i = 0; i < fileList.length; ++i) {
                if (fileList[i].isDirectory()) {
                    searchPdfs(fileList[i]);
                }
                else if (fileList[i].getName().endsWith(pdfPattern)) {
                    pdfFiles.put(fileList[i].getName(), fileList[i].getAbsolutePath());
                }
            }
        }
    }
}