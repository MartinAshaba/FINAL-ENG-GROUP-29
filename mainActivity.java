package com.example.aquasense;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    EditText editTextIP;
    Button buttonConnect, buttonOn, buttonOff, buttonAuto;
    TextView textViewStatus;
    ProgressBar progressBar;

    String espIp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextIP = findViewById(R.id.editTextIP);
        buttonConnect = findViewById(R.id.buttonConnect);
        buttonOn = findViewById(R.id.buttonOn);
        buttonOff = findViewById(R.id.buttonOff);
        buttonAuto = findViewById(R.id.buttonAuto);
        textViewStatus = findViewById(R.id.textViewStatus);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE);

        buttonConnect.setOnClickListener(v -> {
            String inputIp = editTextIP.getText().toString().trim();

            if (!inputIp.isEmpty()) {
                // Prepend "http://" if user didn't include it
                if (!inputIp.startsWith("http://") && !inputIp.startsWith("https://")) {
                    inputIp = "http://" + inputIp;
                }

                espIp = inputIp;
                textViewStatus.setText("Status: Connected to " + espIp);
            } else {
                textViewStatus.setText("Status: Please enter IP address");
            }
        });

        buttonOn.setOnClickListener(v -> sendCommand("/on"));
        buttonOff.setOnClickListener(v -> sendCommand("/off"));
        buttonAuto.setOnClickListener(v -> sendCommand("/auto"));
    }

    private void sendCommand(String commandPath) {
        if (espIp.isEmpty()) {
            textViewStatus.setText("Status: Not connected. Enter IP.");
            return;
        }

        String fullUrl = espIp + commandPath;
        new HttpGetTask().execute(fullUrl);
    }

    private class HttpGetTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            textViewStatus.setText("Status: Sending command...");
        }

        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(View.GONE);
            textViewStatus.setText("Status: " + result);
        }
    }
}
