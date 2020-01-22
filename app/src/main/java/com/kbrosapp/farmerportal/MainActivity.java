package com.kbrosapp.farmerportal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MessageListener {

    public static SharedPreferences sharedPreferences;

    TextView textView;
    TextView textViewLatestMessage;
    String URL = "";
    String IP_ADDRESS = "192.168.1.1";
    EditText editTextIpaddressOfServer;
    Button buttonAddServerIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        textView = findViewById(R.id.textView);
        textViewLatestMessage = findViewById(R.id.textViewLatestMessage);
        buttonAddServerIp = findViewById(R.id.buttonAddServerIp);

        sharedPreferences = this.getSharedPreferences("com.kbrosapp.farmerportal", Context.MODE_PRIVATE);
        IP_ADDRESS = sharedPreferences.getString("ipaddress", "192.168.1.1");
        URL = "http://" + IP_ADDRESS + "/farmfresh/androidToServerCode.php";

        editTextIpaddressOfServer = findViewById(R.id.editTextIpaddressOfServer);
        editTextIpaddressOfServer.setText(IP_ADDRESS);

        buttonAddServerIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipaddress = editTextIpaddressOfServer.getText().toString();
                URL = "http://" + ipaddress + "/farmfresh/androidToServerCode.php";
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("ipaddress", ipaddress).apply();
                Toast.makeText(MainActivity.this, "SERVER IP CHANGED", Toast.LENGTH_SHORT).show();
            }
        });

        //Register sms listener
        MessageReceiver.bindListener(this);




        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //BELOW CODE IS TO SEND DATA FROM SERVER TO ANDROID
        Thread myThread=new Thread(new MyServerThread());
        myThread.start();
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //BELOW CODE IS TO SEND DATA FROM SERVER TO ANDROID
    class MyServerThread implements Runnable{

        Socket s;
        ServerSocket ss;
        InputStreamReader isr;
        BufferedReader bufferedReader;
        Handler h =new Handler();
        String message;

        @Override
        public void run() {
            try {
                ss=new ServerSocket(7881);
                while(true){
                    s=ss.accept();
                    isr=new InputStreamReader(s.getInputStream());
                    bufferedReader=new BufferedReader(isr);
                    message=bufferedReader.readLine();

                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

     /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void messageReceived(final String phone, String msgBody, String time, final String message) {
        //Toast.makeText(this, "From : " + phone + "\nMessage : " + message, Toast.LENGTH_SHORT).show();
        textView.setText("From : " + phone + "\nMessage : " + message);

        sendPhoneMessageToServer(phone, message);

    }

    public void sendPhoneMessageToServer(final String phone, final String message) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject jsonObject = new JSONObject(response);

                    Boolean error = jsonObject.getBoolean("error");
                    String phoneNumber = jsonObject.getString("phone");
                    String message = jsonObject.getString("message");

                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    String latestMessage = "Lastest Message From The Server\nPhone Number : " + phoneNumber + "\nMessage : " + message;
                    textViewLatestMessage.setText(latestMessage);

                    //SEND SMS BACK TO THAT NUMBER
                    SmsManager sms = SmsManager.getDefault();
                    sms.sendTextMessage(phoneNumber, null, message, null, null);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "An Network Error Occured!", Toast.LENGTH_SHORT).show();
                Log.e("VOLLEEEYY!!!!", error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("phone", phone);
                params.put("message", message);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

}
