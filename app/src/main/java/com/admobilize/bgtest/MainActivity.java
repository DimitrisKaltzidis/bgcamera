package com.admobilize.bgtest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.admobilize.bgtest.receivers.ServiceScheduler;
import com.admobilize.bgtest.service.BuiltinCameraService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        Intent newIntent = new Intent(this, BuiltinCameraService.class);
        startService(newIntent);
        ServiceScheduler.startScheduleService(this, 5*1000);

        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("Camera on background test, please close this activity and check logs via ADB");
    }


}
