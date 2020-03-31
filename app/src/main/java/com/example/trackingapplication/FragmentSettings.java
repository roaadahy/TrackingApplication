package com.example.trackingapplication;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FragmentSettings extends Fragment {
    int truckID, truckCapacity, truckType;
    String latitude, longitude;
    private static final int STATE_UPDATE_INTERVAL = 60000;
    private Handler handler = new Handler();
    private Runnable runnable;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.settings_activity, container, false);

        final EditText truckID_edit_txt = root.findViewById(R.id.truck_id);
        final EditText truckCapacity_edit_txt = root.findViewById(R.id.truck_capacity);
        final EditText truckType_edit_txt = root.findViewById(R.id.truck_type);

        Button save_btn = root.findViewById(R.id.save_btn);
        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (truckID_edit_txt.getText().toString().length() == 0 ||
                        truckCapacity_edit_txt.getText().toString().length() == 0 ||
                        truckType_edit_txt.getText().toString().length() == 0) {
                    Toast.makeText(getActivity(), "Incorrect Retry", Toast.LENGTH_LONG).show();
                    truckID_edit_txt.setText("");
                    truckCapacity_edit_txt.setText("");
                    truckType_edit_txt.setText("");
                } else {
                    FirstActivity.sharedPreferences.edit().putBoolean("isSaved", true).apply();

                    truckID = Integer.parseInt(truckID_edit_txt.getText().toString());
                    truckCapacity = Integer.parseInt(truckCapacity_edit_txt.getText().toString());
                    truckType = Integer.parseInt(truckType_edit_txt.getText().toString());

                    FirstActivity.sharedPreferences.edit().putInt("truck_ID", truckID).apply();
                    FirstActivity.sharedPreferences.edit().putInt("capacity", truckCapacity).apply();
                    FirstActivity.sharedPreferences.edit().putInt("truckType", truckType).apply();

                    GpsTracker gpsTracker = new GpsTracker(getActivity());
                    latitude = String.valueOf(gpsTracker.getLatitude());
                    FirstActivity.sharedPreferences.edit().putString("latitude", latitude).apply();
                    longitude = String.valueOf(gpsTracker.getLongitude());
                    FirstActivity.sharedPreferences.edit().putString("longitude", longitude).apply();

                    FragmentSettings.AsyncT asyncT = new FragmentSettings.AsyncT();
                    asyncT.execute();
                }

                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                AsyncT asyncT = new AsyncT();
                asyncT.execute();

                handler.postDelayed(runnable, STATE_UPDATE_INTERVAL);
            }
        }, STATE_UPDATE_INTERVAL);

        return root;
    }

    class AsyncT extends AsyncTask<Void, Void, Void> {
        int responseCode;

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url;

                if (FirstActivity.sharedPreferences.getBoolean("isFull", false)) {
                    if (FirstActivity.sharedPreferences.getBoolean("isCleared", false)) {
                        url = new URL("http://Api-env.pjxxtmeicp.us-east-2.elasticbeanstalk.com/api/track/startup");
                        FirstActivity.sharedPreferences.edit().putBoolean("isFull", false).apply();
                        FirstActivity.sharedPreferences.edit().putBoolean("isStartedAgain", true).apply();
                    } else {
                        url = new URL("http://Api-env.pjxxtmeicp.us-east-2.elasticbeanstalk.com/api/track/updateCurrentState");
                    }
                } else {
                    url = new URL("http://Api-env.pjxxtmeicp.us-east-2.elasticbeanstalk.com/api/track/updateCurrentState");
                }

                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.connect();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("truck_ID", FirstActivity.sharedPreferences.getInt("truck_ID", 0));
                jsonObject.put("current_lat", Double.valueOf(FirstActivity.sharedPreferences.getString("latitude", "")));
                jsonObject.put("current_long", Double.valueOf(FirstActivity.sharedPreferences.getString("longitude", "")));
                jsonObject.put("capacity", FirstActivity.sharedPreferences.getInt("capacity", 0));
                jsonObject.put("type", FirstActivity.sharedPreferences.getInt("truckType", 0));
                jsonObject.put("ReqType", FirstActivity.sharedPreferences.getString("type", ""));

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

                    System.out.println("Update Response: " + jsonResponse.toString());

                    String numberResponse;
                    numberResponse = jsonResponse.toString();
                    if (numberResponse.contains("\"Code\":1")) {
                        if (numberResponse.contains("Truck Not active")) {
                            responseCode = -2;
                        } else {
                            responseCode = 1;
                            FirstActivity.sharedPreferences.edit().putBoolean("isFull", false).apply();
                        }

                    } else if (numberResponse.contains("\"Code\":-1")) {
                        responseCode = -1;
                        FirstActivity.sharedPreferences.edit().putBoolean("isFull", true).apply();
                        JSONObject root = new JSONObject(jsonResponse.toString());
                        FirstActivity.sharedPreferences.edit().putString("baseLatitude", String.valueOf(root.getDouble("baseLat"))).apply();
                        FirstActivity.sharedPreferences.edit().putString("baseLongitude", String.valueOf(root.getDouble("baseLong"))).apply();






                    } else if (numberResponse.contains("\"Code\":2")) {
                        FirstActivity.sharedPreferences.edit().putInt("position",
                                ((FirstActivity.sharedPreferences.getInt("position", 0)) - 1 )).apply();
                        JSONObject root = new JSONObject(jsonResponse.toString());
                        FirstActivity.sharedPreferences.edit().putString("latitude", String.valueOf(root.getDouble("lat"))).apply();
                        FirstActivity.sharedPreferences.edit().putString("longitude", String.valueOf(root.getDouble("long"))).apply();
                        FirstActivity.sharedPreferences.edit().putInt("ID", root.getInt("ID")).apply();
                        FirstActivity.sharedPreferences.edit().putBoolean("isRerouted", true).apply();







                    } else {
                        responseCode = 0;
                        FirstActivity.sharedPreferences.edit().putBoolean("isFull", false).apply();
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
                Toast.makeText(getActivity(), "Go ahead", Toast.LENGTH_LONG).show();
            } else if (responseCode == -1) {
                Toast.makeText(getActivity(), "Full", Toast.LENGTH_LONG).show();
            } else if (responseCode == -2) {
                Toast.makeText(getActivity(), "Full Not Cleared", Toast.LENGTH_LONG).show();
            } else if (responseCode == 2) {
                Toast.makeText(getActivity(), "Decrement Position", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
            }

            if (FirstActivity.sharedPreferences.getBoolean("isSaved", false)) {
                Fragment fragment = new FragmentMap();
                getFragmentManager().beginTransaction().replace(R.id.main_content, fragment).addToBackStack(null).commit();
                FirstActivity.sharedPreferences.edit().putBoolean("isSaved", false).apply();
            }
        }
    }
}