package com.example.examenmapsapp;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.FileDownloadTask;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class RouteDrawer {

    private static final String TAG = "RouteDrawer";
    private Context context;

    // Constructor que recibe el contexto de la actividad
    public RouteDrawer(Context context) {
        this.context = context;
    }

    // Método para trazar la ruta desde la ubicación actual a un destino
    public void drawRoute(GoogleMap map, LatLng origin, LatLng destination) {
        // Mostrar un marcador en el destino
        map.addMarker(new MarkerOptions().position(destination).title("Destino"));

        // Usar la API Directions de Google para obtener la ruta
        String url = getDirectionsUrl(origin, destination);

        // Hacer la solicitud para obtener la ruta
        StringRequest routeRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    // Parsear la respuesta JSON
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray routes = jsonResponse.getJSONArray("routes");
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPoints = overviewPolyline.getString("points");

                    // Decodificar la ruta y dibujarla en el mapa
                    List<LatLng> points = decodePoly(encodedPoints);
                    map.addPolyline(new PolylineOptions()
                            .addAll(points)
                            .width(10)
                            .color(0xFF2196F3)
                            .geodesic(true));

                } catch (Exception e) {
                    Log.e(TAG, "Error al trazar la ruta", e);
                }
            }
        }, error -> Log.e(TAG, "Error en la solicitud de la ruta", error));

        // Agregar la solicitud a la cola usando el contexto de la actividad
        Volley.newRequestQueue(context).add(routeRequest);
    }

    // Método para generar la URL de la API Directions
    private String getDirectionsUrl(LatLng origin, LatLng destination) {
        // Aquí reemplaza con tu propia API Key de Google
        String apiKey = "AIzaSyBpn6VnM16eGMt5YpqeLYYHPfsyk3_zN7E";
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=driving&key=" + apiKey;
    }

    // Método para decodificar los puntos de la ruta de la API Directions
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            polyline.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }
        return polyline;
    }

    // Método para descargar y trazar la ruta desde Firebase Storage
    public void downloadAndDrawRoute(GoogleMap map, StorageReference storageReference) {
        // Creamos un archivo temporal donde almacenaremos el archivo descargado
        try {
            File localFile = File.createTempFile("route", "json");

            // Descargamos el archivo de Firebase Storage
            storageReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                // Archivo descargado con éxito
                Log.d(TAG, "Archivo descargado exitosamente");

                // Procesamos el archivo descargado (suponiendo que contiene la ruta en formato JSON)
                List<LatLng> points = decodePolyFromFile(localFile);

                // Dibujamos la ruta en el mapa
                map.addPolyline(new PolylineOptions()
                        .addAll(points)
                        .width(10)
                        .color(0xFF2196F3)
                        .geodesic(true));
            }).addOnFailureListener(e -> {
                // Manejo de errores en caso de fallo al descargar el archivo
                Log.e(TAG, "Error al descargar el archivo", e);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al crear el archivo temporal", e);
        }
    }

    // Método para decodificar los puntos de la ruta desde el archivo descargado (suponiendo que el archivo sea un JSON)
    private List<LatLng> decodePolyFromFile(File file) {
        List<LatLng> polyline = new ArrayList<>();
        try {
            // Suponiendo que el archivo contiene una cadena con los puntos codificados
            String jsonString = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            JSONObject jsonResponse = new JSONObject(jsonString);
            JSONArray routes = jsonResponse.getJSONArray("routes");
            JSONObject route = routes.getJSONObject(0);
            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
            String encodedPoints = overviewPolyline.getString("points");

            // Decodificar los puntos
            polyline = decodePoly(encodedPoints);
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar el archivo de ruta", e);
        }
        return polyline;
    }
}
