package com.example.examenmapsapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private boolean isTracking = false;
    private LocationUploader locationUploader;
    private RouteDrawer routeDrawer;  // Declara RouteDrawer
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        // Inicializa Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Inicializa los componentes
        locationUploader = new LocationUploader(this);
        routeDrawer = new RouteDrawer();  // Asegúrate de que no haya errores al instanciar RouteDrawer

        // Inicializa el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configura el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botón para iniciar/detener el registro de la ruta
        Button btnToggleTracking = findViewById(R.id.btn_toggle_tracking);
        btnToggleTracking.setOnClickListener(v -> {
            if (!isTracking) {
                startLocationTracking();
                btnToggleTracking.setText("Detener Registro");
            } else {
                stopLocationTracking();
                btnToggleTracking.setText("Iniciar Registro");
            }
        });

        // Botón para mostrar historial de rutas
        Button btnShowHistory = findViewById(R.id.btn_show_history);
        btnShowHistory.setOnClickListener(v -> {
            drawRouteOnMap();
        });

        // Botón para cambiar el estilo del mapa
        Button btnChangeMapStyle = findViewById(R.id.btn_change_map_style);
        btnChangeMapStyle.setOnClickListener(v -> {
            changeMapStyle();
        });

        // Botón para colocar el marcador en la ubicación actual
        Button btnAddMarker = findViewById(R.id.btn_add_marker);
        btnAddMarker.setOnClickListener(v -> {
            getCurrentLocation();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        // Obtener ubicación actual y agregar marcador
        getCurrentLocation();
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Obtener la ubicación actual
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            // Llamar al método para agregar el marcador en la ubicación actual
                            addMarkerAtCurrentLocation(currentLocation);

                            // Mover la cámara al marcador
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        }
                    }
                });
    }

    // Método para agregar el marcador en la ubicación actual
    private void addMarkerAtCurrentLocation(LatLng location) {
        mMap.addMarker(new MarkerOptions().position(location).title("Mi Ubicación Actual"));
    }

    private void drawRouteOnMap() {
        // Ahora pasamos storageReference al método de RouteDrawer
        routeDrawer.downloadAndDrawRoute(mMap, storageReference);
    }

    private void startLocationTracking() {
        isTracking = true;
        locationUploader.startUploading();
    }

    private void stopLocationTracking() {
        isTracking = false;
        locationUploader.stopUploading();
    }

    // Método para cambiar el estilo del mapa
    private void changeMapStyle() {
        try {
            // Cargar el estilo desde el archivo JSON
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success) {
                Toast.makeText(this, "Estilo del mapa no cargado correctamente.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar el estilo del mapa.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
