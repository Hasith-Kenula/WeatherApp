package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private TextView tvLocation, tvAddress, tvTime, tvWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        tvAddress = findViewById(R.id.tvAddress);
        tvTime = findViewById(R.id.tvTime);
        tvWeather = findViewById(R.id.tvWeather);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateUIWithLocation(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }

        updateTime();
    }

    private void updateUIWithLocation(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        tvLocation.setText(String.format(Locale.US, "Lat: %.4f, Lon: %.4f", latitude, longitude));
        getAddressFromLocation(latitude, longitude);
        fetchWeatherData(latitude, longitude);
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                tvAddress.setText(addressText);
            } else {
                tvAddress.setText("Address not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchWeatherData(double latitude, double longitude) {
        String url = String.format(Locale.US, "https://api.openweathermap.org/data/2.5/weather?lat=%.4f&lon=%.4f&appid=ddc1e34f051bc68200648faaa7d7710d&units=metric", latitude, longitude);
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject main = response.getJSONObject("main");
                            double temperature = main.getDouble("temp");
                            int humidity = main.getInt("humidity");

                            JSONObject weatherObject = response.getJSONArray("weather").getJSONObject(0);
                            String weatherDescription = weatherObject.getString("description");

                            String weatherInfo = String.format(Locale.US, "Temp: %.1f°C\nHumidity: %d%%\nDescription: %s", temperature, humidity, weatherDescription);
                            tvWeather.setText(weatherInfo);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvWeather.setText("Failed to get weather data");
                    }
                });

        queue.add(jsonObjectRequest);
    }

    private void updateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        String currentTime = sdf.format(new Date());
        tvTime.setText(currentTime);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}