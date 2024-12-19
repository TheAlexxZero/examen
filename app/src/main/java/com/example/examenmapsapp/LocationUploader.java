package com.example.examenmapsapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

import java.util.ArrayList;
import java.util.List;

public class LocationUploader {
    private FirebaseFirestore firestore;
    private CollectionReference locationsRef;
    private boolean isTracking = false;
    private List<LocationData> locations = new ArrayList<>();
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private Runnable locationUpdater;

    // Constructor
    public LocationUploader(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.locationsRef = firestore.collection("routes");  // Usamos "routes" para las rutas de cada usuario
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.handler = new Handler();
    }

    @SuppressLint("MissingPermission")
    public void startUploading() {
        isTracking = true;

        // Runnable para actualizar la ubicación cada cierto tiempo (30 segundos)
        locationUpdater = new Runnable() {
            @Override
            public void run() {
                if (isTracking) {
                    // Obtener la ubicación real
                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        long timestamp = System.currentTimeMillis();
                                        locations.add(new LocationData(location.getLatitude(), location.getLongitude(), timestamp));

                                        // Llamamos al método para subir las ubicaciones a Firestore
                                        uploadLocations();
                                    } else {
                                        Toast.makeText(context, "Ubicación no disponible", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                    // Reejecutar el Runnable cada 30 segundos
                    handler.postDelayed(this, 30000);  // 30000 ms = 30 segundos
                }
            }
        };

        // Comienza el seguimiento de ubicaciones
        handler.post(locationUpdater);
    }

    private void uploadLocations() {
        if (locations.size() > 0) {
            // Crear un documento único para cada ruta, con un ID de ruta (se puede usar UID del usuario)
            String routeId = "route_" + System.currentTimeMillis();  // Ejemplo de un ID único para cada ruta

            // Subir las ubicaciones al documento
            locationsRef.document(routeId).collection("locations")
                    .add(locations.get(locations.size() - 1))  // Subir solo la última ubicación
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(context, "Ubicación subida con éxito", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(exception -> {
                        Toast.makeText(context, "Error al subir la ubicación: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public void stopUploading() {
        isTracking = false;
        handler.removeCallbacks(locationUpdater);  // Detener el seguimiento de ubicaciones
    }
}

class LocationData {
    private double latitude;
    private double longitude;
    private long timestamp;

    public LocationData(double latitude, double longitude, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
