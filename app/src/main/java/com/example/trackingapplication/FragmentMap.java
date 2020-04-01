package com.example.trackingapplication;

import android.app.Fragment;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FragmentMap extends Fragment {

    MapView mMapView;
    private GoogleMap googleMap;
    private MarkerOptions currentLocation, nextLocation;
    double latitude, longitude, nextLatitude, nextLongitude;
    LatLng location1, location2;
    Marker currentMarker;
    Polyline polyline;
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final int LOCATION_UPDATE_INTERVAL = 2000;
    String type;
    int ID, position;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.map_fragment, container, false);
        mMapView = rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        if (!FirstActivity.sharedPreferences.getBoolean("isFull", false)) {
            AsyncT asyncT = new AsyncT();
            asyncT.execute();
        } else {
            GpsTracker gpsTracker = new GpsTracker(getActivity());
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();

            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap mMap) {
                    googleMap = mMap;

                    location1 = new LatLng(latitude, longitude);
                    location2 = new LatLng(Double.parseDouble(FirstActivity.sharedPreferences.getString("baseLatitude", "")),
                            Double.parseDouble(FirstActivity.sharedPreferences.getString("baseLongitude", "")));

                    currentLocation = new MarkerOptions().position(location1).title("Location 1")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_truck_indigo));
                    currentMarker = googleMap.addMarker(currentLocation);

                    nextLocation = new MarkerOptions().position(location2).title("Location 2")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_indigo));
                    googleMap.addMarker(nextLocation);

                    // For zooming automatically to the location of the marker
                    CameraPosition cameraPosition1 = new CameraPosition.Builder().target(location1).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition1));

                    CameraPosition cameraPosition2 = new CameraPosition.Builder().target(location2).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition2));

                    String url = getDirectionsUrl(location1, location2);
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                }
            });
        }

        mMapView.onResume();// needed to get the map to display immediately
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Perform any camera updates here
        return rootView;
    }

    private void startLocationRunnable() {
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                updateLocation();
                handler.postDelayed(runnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdate() {
        handler.removeCallbacks(runnable);
    }

    private void updateLocation() {
        GpsTracker gpsTracker = new GpsTracker(getActivity());
        LatLng updatedLocation = new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude());
        currentMarker.setPosition(updatedLocation);
        if (latitude != gpsTracker.getLatitude() || longitude != gpsTracker.getLongitude()) {
            if (polyline != null) {
                polyline.remove();
            }
            String url = getDirectionsUrl(updatedLocation, location2);
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.execute(url);
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
        }

        if (FirstActivity.sharedPreferences.getBoolean("isRerouted", false)) {
            googleMap.clear();
            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap mMap) {
                    googleMap = mMap;

                    GpsTracker gpsTracker = new GpsTracker(getActivity());
                    LatLng location = new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude());
                    currentLocation = new MarkerOptions().position(location).title("Location 1")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_truck_indigo));
                    currentMarker = googleMap.addMarker(currentLocation);

                    location2 = new LatLng(Double.parseDouble(FirstActivity.sharedPreferences.getString("latitude", "")),
                            Double.parseDouble(FirstActivity.sharedPreferences.getString("longitude", "")));

                    nextLocation = new MarkerOptions().position(location2).title("Location 2")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_indigo));
                    googleMap.addMarker(nextLocation);

                    CameraPosition cameraPosition1 = new CameraPosition.Builder().target(location1).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition1));

                    CameraPosition cameraPosition2 = new CameraPosition.Builder().target(location2).zoom(12).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition2));

                    String url = getDirectionsUrl(location, location2);
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                    latitude = gpsTracker.getLatitude();
                    longitude = gpsTracker.getLongitude();
                    FirstActivity.sharedPreferences.edit().putBoolean("isRerouted", false).apply();
                }
            });
        }

        if (FirstActivity.sharedPreferences.getBoolean("hasNext", false)) {
            if (!FirstActivity.sharedPreferences.getBoolean("isFull", false)) {
                googleMap.clear();
                AsyncT asyncT = new AsyncT();
                asyncT.execute();
                FirstActivity.sharedPreferences.edit().putBoolean("hasNext", false).apply();
            }
        }

        if (FirstActivity.sharedPreferences.getBoolean("isStartedAgain", false)) {
            googleMap.clear();
            AsyncT asyncT = new AsyncT();
            asyncT.execute();
            FirstActivity.sharedPreferences.edit().putBoolean("isStartedAgain", false).apply();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        startLocationRunnable();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        stopLocationUpdate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    class AsyncT extends AsyncTask<Void, Void, Void> {
        int responseCode;

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL("http://Api-env.pjxxtmeicp.us-east-2.elasticbeanstalk.com/api/track/nextLocation");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.connect();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("truck_ID", FirstActivity.sharedPreferences.getInt("truck_ID", 0));

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

                    System.out.println("Next Response: " + jsonResponse.toString());

                    String numberResponse;
                    numberResponse = jsonResponse.toString();
                    if (numberResponse.contains("\"Code\":1")) {
                        if (numberResponse.contains("Truck Not active")) {
                            responseCode = -2;
                        } else {
                            responseCode = 1;
                            JSONObject root = new JSONObject(jsonResponse.toString());
                            nextLatitude = root.getDouble("lat");
                            nextLongitude = root.getDouble("long");
                            ID = root.getInt("ID");
                            type = root.getString("type");
                            if (type.equals("bin")) {
                                position = root.getInt("position");
                                FirstActivity.sharedPreferences.edit().putInt("position", position).apply();
                            }
                            FirstActivity.sharedPreferences.edit().putInt("ID", ID).apply();
                            FirstActivity.sharedPreferences.edit().putString("type", type).apply();
                            FirstActivity.sharedPreferences.edit().putBoolean("isCleared", false).apply();
                        }
                    } else if (numberResponse.contains("\"Code\":-1")) {
                        responseCode = -1;
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

            GpsTracker gpsTracker = new GpsTracker(getActivity());
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();

            if (responseCode == 1) {
                mMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap mMap) {
                        googleMap = mMap;

                        // For dropping a marker at a point on the Map
                        location1 = new LatLng(latitude, longitude);
                        location2 = new LatLng(nextLatitude, nextLongitude);

                        currentLocation = new MarkerOptions().position(location1).title("Location 1")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_truck_indigo));
                        currentMarker = googleMap.addMarker(currentLocation);

                        nextLocation = new MarkerOptions().position(location2).title("Location 2")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_indigo));
                        googleMap.addMarker(nextLocation);

                        // For zooming automatically to the location of the marker
                        CameraPosition cameraPosition1 = new CameraPosition.Builder().target(location1).zoom(12).build();
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition1));

                        CameraPosition cameraPosition2 = new CameraPosition.Builder().target(location2).zoom(12).build();
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition2));

                        String url = getDirectionsUrl(location1, location2);
                        DownloadTask downloadTask = new DownloadTask();
                        downloadTask.execute(url);
                    }
                });
            } else if (responseCode == -1) {
                System.out.println("wrong truck id");
            } else if (responseCode == -2) {
                System.out.println("Truck Not Active");
            } else {
                System.out.println("Error");
            }
        }
    }


    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DataParser parser = new DataParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = new ArrayList();
            PolylineOptions lineOptions = new PolylineOptions();
            for (int i = 0; i < result.size(); i++) {
                List<HashMap<String, String>> path = result.get(i);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.GRAY);
                lineOptions.geodesic(true);
            }
            if (points.size() != 0)
                polyline = googleMap.addPolyline(lineOptions);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String mode = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

}