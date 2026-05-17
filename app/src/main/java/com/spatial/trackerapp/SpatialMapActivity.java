package com.spatial.trackerapp;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class SpatialMapActivity extends AppCompatActivity {

    private MapView spatialMapView;
    private RequestQueue apiRequestQueue;
    private final String fetchUrl = "http://10.0.2.2/map_project/getPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuration d'OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("spatial_prefs", MODE_PRIVATE));
        setContentView(R.layout.activity_spatial_map);

        spatialMapView = findViewById(R.id.osm_map_view);
        spatialMapView.setTileSource(TileSourceFactory.MAPNIK);
        spatialMapView.setBuiltInZoomControls(true);
        spatialMapView.setMultiTouchControls(true);

        spatialMapView.getController().setZoom(16.0);
        spatialMapView.getController().setCenter(new GeoPoint(33.5731, -7.5898)); // Casablanca

        apiRequestQueue = Volley.newRequestQueue(this);
        retrieveAndDisplayMarkers();
    }

    private void retrieveAndDisplayMarkers() {
        JsonObjectRequest fetchRequest = new JsonObjectRequest(Request.Method.POST, fetchUrl, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray dataArray = response.getJSONArray("positions");
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                double lat = item.getDouble("latitude");
                                double lng = item.getDouble("longitude");

                                Marker posMarker = new Marker(spatialMapView);
                                posMarker.setPosition(new GeoPoint(lat, lng));
                                posMarker.setTitle("Point #" + (i + 1));

                                try {
                                    // Utilisation de ContextCompat pour récupérer le drawable de manière sécurisée
                                    Drawable iconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_location_marker);
                                    if (iconDrawable instanceof BitmapDrawable) {
                                        Bitmap b = ((BitmapDrawable) iconDrawable).getBitmap();
                                        Bitmap rescaled = Bitmap.createScaledBitmap(b, 70, 70, false);
                                        posMarker.setIcon(new BitmapDrawable(getResources(), rescaled));
                                    } else if (iconDrawable != null) {
                                        posMarker.setIcon(iconDrawable);
                                    }
                                } catch (Exception e) {
                                    // Fallback au marqueur par défaut
                                }

                                posMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                spatialMapView.getOverlays().add(posMarker);
                            }
                            spatialMapView.invalidate();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
        );

        apiRequestQueue.add(fetchRequest);
    }

    @Override
    public void onResume() {
        super.onResume();
        spatialMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        spatialMapView.onPause();
    }
}
