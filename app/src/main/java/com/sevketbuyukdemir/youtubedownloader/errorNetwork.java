package com.sevketbuyukdemir.youtubedownloader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class errorNetwork extends AppCompatActivity {
    Button againTryNetwork;
    TextView errorMessageTextView;
    Context context = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error_network);
        errorMessageTextView = findViewById(R.id.error_message_textView);
        againTryNetwork = findViewById(R.id.again_network);
        againTryNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Splash.class);
                startActivity(intent);
                finish();
            }
        });
    }
}

