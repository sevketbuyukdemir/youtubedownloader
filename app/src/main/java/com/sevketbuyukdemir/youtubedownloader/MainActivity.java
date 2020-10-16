package com.sevketbuyukdemir.youtubedownloader;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {
    private final String url_youtube = "https://www.youtube.com";
    private String currentDownloadURl;
    private String oldDownloadURL = "";
    private WebView webView;
    public Context context = this;
    CustomWebViewClient webViewClient;
    NetworkChangeReceiver receiverNetwork;
    public final String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE = 1;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;
    ProgressDialog progressDialog;
    private static final int progressDialogEnterId = 11;
    private static final int progressDialogDownloadId = 12;
    private String fileURL;
    private String musicName;
    private static final int BUFFER_SIZE = 4096;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiverNetwork = new NetworkChangeReceiver();
        registerReceiver(receiverNetwork, filter);
        final SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(!networkIsOn()){
                    Intent intent = new Intent(context, errorNetwork.class);
                    startActivity(intent);
                    finish();
                }
                webView.reload();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        showDialog(progressDialogEnterId);

        webViewClient = new CustomWebViewClient();
        webView = findViewById(R.id.webView);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(url_youtube);
        if(!(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ){
            requestReadStoragePermission();
        }
        if(!(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ){
            requestWriteStoragePermission();
        }
        if(!(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED)){
            requestInternet();
        }
        if(!(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED)){
            requestAccessNetworkState();
        }
    }

    private class CustomWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return url.startsWith(url_youtube);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if(!progressDialog.isShowing()){
                progressDialog.show();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if(progressDialog.isShowing()){
                progressDialog.dismiss();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, final boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            currentDownloadURl = url;

            if(currentDownloadURl.contains("/watch?v=")){
                if(oldDownloadURL.equals("") || !(oldDownloadURL.equals(currentDownloadURl))){
                    oldDownloadURL = url;
                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setCancelable(true);
                    builder.setTitle(getString(R.string.download_title));
                    builder.setPositiveButton(getString(R.string.download_mp3_button), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            showDialog(progressDialogDownloadId);

                            new Thread(new Runnable() {
                                public void run() {
                                    downloadUrlPrepare();
                                    downloadProcess(fileURL);
                                }
                                private void downloadUrlPrepare(){
                                    final String apiStateText = "www.convertmp3.io/fetch/?format=text&video=";
                                    String d = apiStateText + currentDownloadURl;
                                    try {
                                        String info = "";
                                        URL u;
                                        Scanner s;
                                        u = new URL("https://" + d);
                                        s = new Scanner(u.openStream());
                                        while (s.hasNext()) {
                                            info += s.nextLine();
                                        }
                                        d = info.substring(info.indexOf("https"));
                                        musicName = info.substring((info.indexOf("Title") + 7), info.indexOf("<br"));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    fileURL = d;
                                }
                                private void downloadProcess(String urlYouTube) {
                                    try {
                                        String saveFilePath;
                                        URL url = new URL(urlYouTube);
                                        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                                        int responseCode = httpConn.getResponseCode();
                                        if (responseCode == HttpURLConnection.HTTP_OK) {
                                            String fileName;
                                            int contentLength = httpConn.getContentLength();
                                            if (contentLength <= -1) {
                                                downloadProcess(urlYouTube);
                                            }
                                            fileName = musicName + ".mp3";
                                            InputStream inputStream = httpConn.getInputStream();
                                            saveFilePath = directory + "/" + fileName;
                                            File saveFile = new File(saveFilePath);
                                            saveFile.setReadable(true);
                                            FileOutputStream outputStream = new FileOutputStream(saveFile);
                                            int downloaded = 0;
                                            int bytesRead;
                                            byte[] buffer = new byte[BUFFER_SIZE];
                                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                                outputStream.write(buffer, 0, bytesRead);
                                                downloaded += bytesRead;
                                                progressDialog.setProgress((downloaded * 100) / contentLength);
                                            }
                                            outputStream.close();
                                            inputStream.close();
                                            dismissDialog(progressDialogDownloadId);
                                        }
                                        httpConn.disconnect();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        dismissDialog(progressDialogDownloadId);
                                    }
                                }
                            }).start();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.download_cancel_button), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id){
            case progressDialogEnterId:
                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(getString(R.string.enter_progress));
                progressDialog.show();
                return progressDialog;
            case progressDialogDownloadId:
                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(getString(R.string.download_title));
                progressDialog.setMax(100);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.show();
                return progressDialog;
                default:
                    return null;

        }
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
        }else{
            AlertDialog.Builder exit = new AlertDialog.Builder(context);
            exit.setTitle(getString(R.string.exit_alert_title));
            exit.setMessage(getString(R.string.exit_alert_message));
            exit.setCancelable(true);
            exit.setPositiveButton(getString(R.string.exit_alert_do_not_exit_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            exit.setNegativeButton(getString(R.string.exit_alert_exit_button), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.super.onBackPressed();
                }
            });
            exit.show();
        }
    }

    boolean networkIsOn(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isAvailable()
                && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiverNetwork);
        super.onDestroy();
    }

    private void requestReadStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    private void requestWriteStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void requestAccessNetworkState(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_NETWORK_STATE)){
            ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.ACCESS_NETWORK_STATE}, MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE);
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_NETWORK_STATE}, MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE);
        }
    }

    private void requestInternet(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)){
            ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }
    }
}
