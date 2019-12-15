package com.example.xiaochongchen.androidspeedtest;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import android.net.*;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

public class MainActivity extends AppCompatActivity {

    String wifiIP;
    ProgressBar pb = null;
    TextView output_textView = null;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize GUI
        output_textView = (TextView) findViewById(R.id.textView);
        Button start_button = (Button) findViewById(R.id.start_button);
        pb = (ProgressBar) findViewById(R.id.progressBar);

        // Obtain the wifi gateway IP
        wifiIP = GetGatewayIP();

        output_textView.setText(wifiIP);

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pb.setMax(100);
                pb.setProgress(0);
                Thread downloadthread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Download();
                    }
                });
                downloadthread.start();
            }
        });
    }

    public String GetGatewayIP()
    {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wm.setWifiEnabled(true);
        DhcpInfo dhcpInfo = wm.getDhcpInfo();

        // Get the ip address of the wifi
        int ip = dhcpInfo.serverAddress;

        String ipStr =
                String.format("%d.%d.%d.%d",
                        (ip & 0xff),
                        (ip >> 8 & 0xff),
                        (ip >> 16 & 0xff),
                        (ip >> 24 & 0xff));

        return ipStr;
    }

    // returns false if not reachable or timed out
    private boolean SendPingRequest(String ipAddress){
        System.out.println("executeCommand");
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 " + ipAddress);
            int mExitValue = mIpAddrProcess.waitFor();
            System.out.println(" mExitValue "+mExitValue);
            if(mExitValue==0){
                return true;
            }else{
                return false;
            }
        }
        catch (InterruptedException ignore)
        {
            ignore.printStackTrace();
            System.out.println(" Exception:"+ignore);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println(" Exception:"+e);
        }
        return false;
    }


    public void Download()
    {

        File file = new File(Environment.DIRECTORY_DOWNLOADS + "/test.apk");
        String url = "https://dl.google.com/dl/android/studio/install/3.5.3.0/android-studio-ide-191.6010548-mac.dmg";

        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        long start = 0, end = 0;
        int count = 0;
        try {
            URL sUrl = new URL(url);
            connection = (HttpURLConnection) sUrl.openConnection();
            connection.connect();

            // download the file
            input = connection.getInputStream();
            //output = new FileOutputStream(file);

            start = System.currentTimeMillis();
            byte data[] = new byte[1024];
            int bytes = 0;
            while (input.read(data, 0, 1024) != -1) {
                // allow canceling with back button
                if(bytes % 512 == 0)
                {
                    count++;
                    pb.incrementSecondaryProgressBy(1);
                }
                if(pb.getProgress() >= 99 || count > 99) {
                    end = System.currentTimeMillis();
                    break;
                }
                bytes++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long time = end - start;
        final int speed = (int)(512 * 1000 / time);

        // Check to see Ping response time
        start = System.currentTimeMillis();
        SendPingRequest(wifiIP);
        end = System.currentTimeMillis();
        final int pingTime = (int) (end - start);

        // Send another ping request to check the time delay between two pings
        SendPingRequest(wifiIP);
        long end2 = System.currentTimeMillis();
        final int pingDelay = (int) (end2 - start);

        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // change UI elements here
                    output_textView.setText("Speed: " + String.valueOf(speed) + "Mbps\r\n"
                            + "Ping Response: " + String.valueOf(pingTime) + "ms\r\n"
                            + "Delay Between Pings: " + String.valueOf(pingDelay) + "ms\r\n");
                }
            });
        }catch (Exception e){

        }

    }

}
