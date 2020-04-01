package com.example.trackingapplication;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SecondActivity extends AppCompatActivity {

    List<ItemSlideMenu> slideMenu;
    SlidingMenuAdapter adapter;
    ListView listViewSliding;
    DrawerLayout drawerLayout;
    ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_content);

        listViewSliding = findViewById(R.id.list_slide_menu);
        drawerLayout = findViewById(R.id.drawer_layout);

        slideMenu = new ArrayList<>();
        slideMenu.add(new ItemSlideMenu(R.drawable.ic_location, "Map"));
        slideMenu.add(new ItemSlideMenu(R.drawable.ic_settings, "Settings"));
        slideMenu.add(new ItemSlideMenu(R.drawable.ic_clear, "Clear"));

        adapter = new SlidingMenuAdapter(this, slideMenu);
        listViewSliding.setAdapter(adapter);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listViewSliding.setItemChecked(0, true);
        drawerLayout.closeDrawer(listViewSliding);
        replaceFragment(0);

        listViewSliding.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 2) {
                    AsyncT asyncT = new AsyncT();
                    asyncT.execute();
                } else {
                    listViewSliding.setItemChecked(position, true);
                    replaceFragment(position);
                }
                drawerLayout.closeDrawer(listViewSliding);
            }
        });

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_opened, R.string.drawer_closed) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    private void replaceFragment(int position) {
        Fragment fragment;

        switch (position) {
            case 1:
                fragment = new FragmentSettings();
                break;
            default:
                fragment = new FragmentMap();
                break;
        }

        if (fragment != null) {
            getFragmentManager().beginTransaction().replace(R.id.main_content, fragment).addToBackStack(null).commit();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    class AsyncT extends AsyncTask<Void, Void, Void> {
        int responseCode;

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL("http://Api-env.pjxxtmeicp.us-east-2.elasticbeanstalk.com/api/track/cleared");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.connect();

                JSONObject jsonObject = new JSONObject();

                if (FirstActivity.sharedPreferences.getString("type", "").equals("bin")) {
                    jsonObject.put("truck_ID", FirstActivity.sharedPreferences.getInt("truck_ID", 0));
                    jsonObject.put("binID", FirstActivity.sharedPreferences.getInt("ID", 0));
                    jsonObject.put("position", FirstActivity.sharedPreferences.getInt("position", 0) + 1);
                } else if (FirstActivity.sharedPreferences.getString("type", "").equals("Claim")) {
                    jsonObject.put("truck_ID", FirstActivity.sharedPreferences.getInt("truck_ID", 0));
                    jsonObject.put("claimID", FirstActivity.sharedPreferences.getInt("ID", 0));
                    jsonObject.put("position", FirstActivity.sharedPreferences.getInt("position", 0) + 1);
                } else {
                    System.out.println("No defined type");
                }

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

                    System.out.println("Clear Response: " + jsonResponse.toString());

                    String numberResponse;
                    numberResponse = jsonResponse.toString();
                    if (numberResponse.contains("\"Code\":1")) {
                        if (numberResponse.contains("Truck Not active")) {
                            responseCode = -2;
                        } else {
                            responseCode = 1;
                        }
                    } else if (numberResponse.contains("\"Code\":-1")) {
                        responseCode = -1;
                    } else {
                        responseCode = 0;
                    }

                    if (FirstActivity.sharedPreferences.getBoolean("isFull", false)) {
                        FirstActivity.sharedPreferences.edit().putBoolean("isCleared", true).apply();
                        FirstActivity.sharedPreferences.edit().putInt("capacity", 0).apply();
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
                Toast.makeText(getBaseContext(), "Has next position", Toast.LENGTH_LONG).show();
                FirstActivity.sharedPreferences.edit().putBoolean("hasNext", true).apply();
            } else if (responseCode == -1) {
                Toast.makeText(getBaseContext(), "No new positions", Toast.LENGTH_LONG).show();
                FirstActivity.sharedPreferences.edit().putBoolean("hasNext", false).apply();
            } else if (responseCode == -2) {
                Toast.makeText(getBaseContext(), "Empty", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
