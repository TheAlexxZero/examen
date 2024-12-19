package com.example.examenmapsapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationUploader locationUploader;
    private RouteDrawer routeDrawer;
    private FusedLocationProviderClient fusedLocationClient;
    private Button startButton, stopButton, historyButton, mapStyleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar botones
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        historyButton = findViewById(R.id.historyButton);
        mapStyleButton = findViewById(R.id.mapStyleButton);

        // Cargar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar la clase RouteDrawer con el contexto
        routeDrawer = new RouteDrawer(this);

        // Configurar botones
        startButton.setOnClickListener(v -> {
            if (locationUploader != null) {
                locationUploader.startUploading();
                Toast.makeText(MainActivity.this, "Seguimiento iniciado", Toast.LENGTH_SHORT).show();
            }
        });

        stopButton.setOnClickListener(v -> {
            if (locationUploader != null) {
                locationUploader.stopUploading();
                Toast.makeText(MainActivity.this, "Seguimiento detenido", Toast.LENGTH_SHORT).show();
            }
        });

        historyButton.setOnClickListener(v -> {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            routeDrawer.downloadAndDrawRoute(mMap, storageReference); // Asegúrate de tener este método implementado
            Toast.makeText(MainActivity.this, "Cargando historial de rutas...", Toast.LENGTH_SHORT).show();
        });

        mapStyleButton.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.setMapType(mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL
                        ? GoogleMap.MAP_TYPE_SATELLITE
                        : GoogleMap.MAP_TYPE_NORMAL);
                Toast.makeText(MainActivity.this, "Estilo de mapa cambiado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Habilitar el botón de ubicación en el mapa
        mMap.setMyLocationEnabled(true);

        // Obtener la ubicación actual y mover la cámara
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
            }
        });

        // Inicializar clases de seguimiento
        locationUploader = new LocationUploader(this);  // Aquí pasamos el contexto de la actividad
    }
}