package com.example.trackingapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FirstActivity extends Activity {

    int truckID, truckCapacity, truckType;
    String latitude, longitude;
    static SharedPreferences sharedPreferences;
    GpsTracker gpsTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("PREFERENCE", MODE_PRIVATE);
        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);
        sharedPreferences.getBoolean("isFull", false);
        sharedPreferences.getInt("truck_ID", 0);
        sharedPreferences.getInt("capacity", 0);
        sharedPreferences.getInt("truckType", 0);
        sharedPreferences.getInt("ID", 0);
        sharedPreferences.getString("type", "");
        sharedPreferences.getInt("position", 0);
        sharedPreferences.getString("latitude", "");
        sharedPreferences.getString("longitude", "");
        sharedPreferences.getString("baseLatitude", "");
        sharedPreferences.getString("baseLongitude", "");
        sharedPreferences.getBoolean("hasNext", false);
        sharedPreferences.getBoolean("isCleared", false);
        sharedPreferences.getBoolean("isSaved", false);
        sharedPreferences.getBoolean("isRerouted", false);
        sharedPreferences.getBoolean("isStartedAgain", false);

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (isFirstRun) {
            setContentView(R.layout.settings_activity);

            final EditText truckID_edit_txt = findViewById(R.id.truck_id);
            final EditText truckCapacity_edit_txt = findViewById(R.id.truck_capacity);
            final EditText truckType_edit_txt = findViewById(R.id.truck_type);


            Button save_btn = findViewById(R.id.save_btn);
            save_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gpsTracker = new GpsTracker(FirstActivity.this);

                    if (gpsTracker.canGetLocation()) {
                        latitude = String.valueOf(gpsTracker.getLatitude());
                        sharedPreferences.edit().putString("latitude", latitude).apply();
                        longitude = String.valueOf(gpsTracker.getLongitude());
                        sharedPreferences.edit().putString("longitude", longitude).apply();
                    } else {
                        gpsTracker.showSettingsAlert();
                    }

                    if (truckID_edit_txt.getText().toString().length() == 0 ||
                            truckCapacity_edit_txt.getText().toString().length() == 0 ||
                            truckType_edit_txt.getText().toString().length() == 0) {
                        Toast.makeText(FirstActivity.this, "Incorrect Retry", Toast.LENGTH_LONG).show();
                        truckID_edit_txt.setText("");
                        truckCapacity_edit_txt.setText("");
                        truckType_edit_txt.setText("");
                    } else {
                        truckID = Integer.parseInt(truckID_edit_txt.getText().toString());
                        truckCapacity = Integer.parseInt(truckCapacity_edit_txt.getText().toString());
                        truckType = Integer.parseInt(truckType_edit_txt.getText().toString());

                        sharedPreferences.edit().putBoolean("isFirstRun", false).apply();
                        sharedPreferences.edit().putInt("truck_ID", truckID).apply();
                        sharedPreferences.edit().putInt("capacity", truckCapacity).apply();
                        sharedPreferences.edit().putInt("truckType", truckType).apply();

                        AsyncT asyncT = new AsyncT();
                        asyncT.execute();
                    }

                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            });

        } else {
            Intent intent = new Intent(FirstActivity.this, SecondActivity.class);
            startActivity(intent);
        }
    }

    class AsyncT extends AsyncTask<Void, Void, Void> {
        int responseCode;

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL("http://Api-env.pjxxtmeicp.us-east-2.elasticbeanstalk.com/api/track/startup");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.connect();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("truck_ID", truckID);
                jsonObject.put("capacity", truckCapacity);
                jsonObject.put("current_lat", latitude);
                jsonObject.put("current_long", longitude);
                jsonObject.put("type", truckType);

                DataOutputStream write = new DataOutputStream(httpURLConnection.getOutputStream());
                write.writeBytes(jsonObject.toString());
                write.flush();
                write.close();

                try (BufferedReader read = new BufferedReader(
                        new InputStreamReader(httpURLConnection.getInputStream(), "utf-8"))) {
                    StringBuilder jsonResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = read.readLine()) != null) {
                        jsonResponse.append(responseLine.trim());
                    }

                    System.out.println("Start up Response: " + jsonResponse.toString());

                    String numberResponse;
                    numberResponse = jsonResponse.toString();
                    if (numberResponse.contains("\"Code\":1")) {
                        responseCode = 1;
                    } else {
                        responseCode = 0;
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (responseCode == 1) {
                Toast.makeText(getBaseContext(), "Done", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(FirstActivity.this, SecondActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_LONG).show();
            }
        }
    }
}