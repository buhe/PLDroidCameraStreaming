package com.pili.pldroid.streaming.camera.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.streaming.StreamingProfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String url = "your app server address.";

    private static final String[] m = {"Water", "Normal"};

    private TextView view;
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
    private String water;

    private static boolean isSupportHWEncode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            water = m[arg2];
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    private String requestStreamJson() {
        try {
            HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setConnectTimeout(5000);
            httpConn.setReadTimeout(10000);
            int responseCode = httpConn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int length = httpConn.getContentLength();
            if (length <= 0) {
                return null;
            }
            InputStream is = httpConn.getInputStream();
            byte[] data = new byte[length];
            int read = is.read(data);
            is.close();
            if (read <= 0) {
                return null;
            }
            return new String(data, 0, read);
        } catch (Exception e) {
            showToast("Network error!");
        }
        return null;
    }

    void showToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startStreamingActivity(final Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String resByHttp = null;

                if (!Config.DEBUG_MODE) {
//                    resByHttp = requestStreamJson();
                    resByHttp = "";
                    Log.i(TAG, "resByHttp:" + resByHttp);
                    if (resByHttp == null) {
                        showToast("Stream Json Got Fail!");
                        return;
                    }
                    intent.putExtra(Config.EXTRA_KEY_STREAM_JSON, resByHttp);
                    intent.putExtra("Water", water);
                }

                startActivity(intent);
            }
        }).start();
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view = (TextView) findViewById(R.id.waterText);
        spinner = (Spinner) findViewById(R.id.SpinnerWater);
        //将可选内容与ArrayAdapter连接起来
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m);

        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //将adapter 添加到spinner中
        spinner.setAdapter(adapter);

        //添加事件Spinner事件监听
        spinner.setOnItemSelectedListener(new SpinnerSelectedListener());

        //设置默认值
        spinner.setVisibility(View.VISIBLE);

        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/qiniuStreaming" );
            File logDirectory = new File( appDirectory + "/log" );
            File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec( "logcat -c");
                process = Runtime.getRuntime().exec( "logcat -f " + logFile + " *:S TEST:D TEST:D");
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }

        TextView mVersionInfoTextView = (TextView) findViewById(R.id.version_info);
        mVersionInfoTextView.setText("v1.6.2");

        Button mHWCodecCameraStreamingBtn = (Button) findViewById(R.id.hw_codec_camera_streaming_btn);
        if (!isSupportHWEncode()) {
            mHWCodecCameraStreamingBtn.setVisibility(View.INVISIBLE);
        }
        mHWCodecCameraStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HWCodecCameraStreamingActivity.class);
                startStreamingActivity(intent);
            }
        });

        Button mSWCodecCameraStreamingBtn = (Button) findViewById(R.id.sw_codec_camera_streaming_btn);
        mSWCodecCameraStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SWCodecCameraStreamingActivity.class);
                startStreamingActivity(intent);
            }
        });

        Button mAudioStreamingBtn = (Button) findViewById(R.id.start_pure_audio_streaming_btn);
        mAudioStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AudioStreamingActivity.class);
                startStreamingActivity(intent);
            }
        });
    }
}
