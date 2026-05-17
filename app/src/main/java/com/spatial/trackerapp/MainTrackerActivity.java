package com.spatial.trackerapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainTrackerActivity extends AppCompatActivity {

    private Button showMapBtn;
    private double currentLat, currentLon;
    private RequestQueue networkQueue;
    private final String endpointUrl = "http://10.0.2.2/map_project/createPosition.php";
    private LocationManager spatialLocationManager;

    private static final int LOCATION_AUTH_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Utilise R.layout.activity_main généré dans le package com.spatial.trackerapp
        setContentView(R.layout.activity_main);

        networkQueue = Volley.newRequestQueue(this);
        spatialLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        showMapBtn = findViewById(R.id.btn_open_map);
        showMapBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainTrackerActivity.this, SpatialMapActivity.class));
        });

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_AUTH_CODE);
        } else {
            initializeLocationTracking();
        }
    }

    private void initializeLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        spatialLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 45000, 100, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                currentLat = loc.getLatitude();
                currentLon = loc.getLongitude();

                String info = String.format(Locale.getDefault(), "Lat: %.5f, Lon: %.5f", currentLat, currentLon);
                syncPositionToServer(currentLat, currentLon);
                Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {}
            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_AUTH_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationTracking();
            } else {
                Toast.makeText(this, "Accès localisation requis", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void syncPositionToServer(final double lat, final double lon) {
        StringRequest postRequest = new StringRequest(Request.Method.POST, endpointUrl,
                response -> {},
                error -> {}) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> data = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                data.put("latitude", String.valueOf(lat));
                data.put("longitude", String.valueOf(lon));
                data.put("date", dateFormat.format(new Date()));
                
                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                data.put("imei", deviceId);

                return data;
            }
        };
        networkQueue.add(postRequest);
    }
}
