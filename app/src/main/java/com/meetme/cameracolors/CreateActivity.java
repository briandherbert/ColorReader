package com.meetme.cameracolors;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class CreateActivity extends Activity {
    public static final String TAG = CreateActivity.class.getSimpleName();
    ColorsView colorsView;
    EditText txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        colorsView = (ColorsView) findViewById(R.id.colorsview);
        txtMessage = (EditText) findViewById(R.id.txt_message);

        Button submit = (Button) findViewById(R.id.btn_submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = txtMessage.getText().toString();

                Log.v(TAG, "setting msg " + msg);
                colorsView.setMessage(msg);
            }
        });
    }

}
