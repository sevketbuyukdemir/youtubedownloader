package com.sevketbuyukdemir.youtubedownloader;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {
    Context context = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        Thread thread;
        thread = new Thread(){
            @Override
            public void run() {
                try{
                    synchronized (this) {
                        wait(2000);//wait time on enter
                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                } finally {
                    if(networkIsOn()){
                        //if internet connection is exist go to main activity
                        Intent intent = new Intent(context, MainActivity.class);
                        startActivity(intent);
                        finish();//delete connect from splash screen
                    }else{
                        //if internet connection do not exist go to error page
                        Intent intent = new Intent(context, errorNetwork.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        };
        thread.start();
    }

    boolean networkIsOn(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isAvailable()
                && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
