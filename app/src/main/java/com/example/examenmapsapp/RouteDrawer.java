package com.example.examenmapsapp;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteDrawer {

    // Modificado para aceptar StorageReference como parámetro
    public void downloadAndDrawRoute(GoogleMap map, StorageReference storageReference) {
        storageReference.child("user_locations.json").getBytes(1024 * 1024)
                .addOnSuccessListener(bytes -> {
                    try {
                        String jsonData = new String(bytes);
                        JSONArray jsonArray = new JSONArray(jsonData);
                        List<LatLng> points = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject locationObject = jsonArray.getJSONObject(i);
                            double lat = locationObject.getDouble("latitude");
                            double lng = locationObject.getDouble("longitude");
                            points.add(new LatLng(lat, lng));
                        }

                        // Agrega la polilínea al mapa
                        map.addPolyline(new PolylineOptions().addAll(points));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}
