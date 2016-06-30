package org.m4m.samples;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MYtestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mytest);
        LinearLayout rl = (LinearLayout) findViewById(R.id.layout);
        Button temp = new Button(this);
        temp.setText("i am new");
        rl.addView(temp);
        temp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                VideoTranscoder vt = new VideoTranscoder();
                Log.e("LJR","onClick"+'1');
                vt.
/*
                vt.cropAndconcat(new VideoTranscoder.IVideoTranscoderListener() {
                    @Override
                    public void onEachProgress(float progress) {
                        Log.e("LJR", "onEachProgress" + progress);
                    }


                    @Override
                    public void onDone() {
                        Log.e("LJR", "onDone");
                    }

                    @Override
                    public void onError() {
                        Log.e("LJR", "onError");
                    }
                });
*/
            }
        });
        temp = new Button(this);


        temp.setText("i am neweeeeeeeeeeee");

        rl.addView(temp);


        temp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {





                VideoTranscoder vt = new VideoTranscoder();

/*                vt.crop("/storage/emulated/0/DCIM/Camera/20160516_165204.mp4",
                        1, 4, "/storage/emulated/0/DCIM/Camera/outA1.mp4",
                        new VideoTranscoder.IVideoTranscoderListener() {
                            @Override
                            public void onEachProgress(float progress) {
                                Log.e("LJR", "onEachProgress" + progress);
                            }


                            @Override
                            public void onDone() {
                                Log.e("LJR", "onDone");
                            }

                            @Override
                            public void onError() {
                                Log.e("LJR", "onError");
                            }
                        })*/;
            }
        });
        temp = new Button(this);
        temp.setText("i am newwwwwwww");
        rl.addView(temp);
        temp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                VideoTranscoder vt = new VideoTranscoder();
                vt.concat(new String[]{"/storage/emulated/0/DCIM/Camera/outA1.mp4", "/storage/emulated/0/DCIM/Camera/outA1.mp4","/storage/emulated/0/DCIM/Camera/outA1.mp4"}, "/storage/emulated/0/DCIM/Camera/outA2.mp4",
                        new VideoTranscoder.IVideoTranscoderListener() {
                            @Override
                            public void onProgress(float progress) {
                                Log.e("LJR", "onEachProgress" + progress);
                            }


                            @Override
                            public void onDone() {
                                Log.e("LJR", "onDone");
                            }

                            @Override
                            public void onError() {
                                Log.e("LJR", "onError");
                            }
                        });
            }
        });
    }
}
