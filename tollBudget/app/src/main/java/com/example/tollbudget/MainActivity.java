package com.example.tollbudget;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    
    // UI Components
    private EditText etDestination;
    private EditText etBudget;
    private Button btnFindRoutes;
    private TextView tvCurrentLocation;
    private RecyclerView rvRoutes;
    private GoogleMap mMap;
    
    // Location and Route components
    private FusedLocationProviderClient fusedLocationClient;
    private RouteAdapter routeAdapter;
    private List<Route> availableRoutes;
    private LatLng currentLocation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupMap();
        setupLocationServices();
        setupRouteRecyclerView();
        setupClickListeners();
    }
    
    private void initializeViews() {
        etDestination = findViewById(R.id.et_destination);
        etBudget = findViewById(R.id.et_budget);
        btnFindRoutes = findViewById(R.id.btn_find_routes);
        tvCurrentLocation = findViewById(R.id.tv_current_location);
        rvRoutes = findViewById(R.id.rv_routes);
    }
    
    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    
    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();
    }
    
    private void setupRouteRecyclerView() {
        availableRoutes = new ArrayList<>();
        routeAdapter = new RouteAdapter(availableRoutes, this::onRouteSelected);
        rvRoutes.setLayoutManager(new LinearLayoutManager(this));
        rvRoutes.setAdapter(routeAdapter);
    }
    
    private void setupClickListeners() {
        btnFindRoutes.setOnClickListener(v -> findBudgetRoutes());
    }
    
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        tvCurrentLocation.setText(String.format("Current: %.4f, %.4f", 
                            location.getLatitude(), location.getLongitude()));
                        
                        if (mMap != null) {
                            mMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Current Location"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                        }
                    }
                });
    }
    
    private void findBudgetRoutes() {
        String destination = etDestination.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        
        if (destination.isEmpty() || budgetStr.isEmpty()) {
            Toast.makeText(this, "Please enter destination and budget", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentLocation == null) {
            Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }
        
        double budget = Double.parseDouble(budgetStr);
        
        // Show loading
        btnFindRoutes.setText("Finding Routes...");
        btnFindRoutes.setEnabled(false);
        
        // Simulate API call - replace with actual implementation
        RouteService.findRoutes(currentLocation, destination, budget, new RouteCallback() {
            @Override
            public void onRoutesFound(List<Route> routes) {
                runOnUiThread(() -> {
                    availableRoutes.clear();
                    availableRoutes.addAll(routes);
                    routeAdapter.notifyDataSetChanged();
                    
                    btnFindRoutes.setText("Find Routes");
                    btnFindRoutes.setEnabled(true);
                    
                    if (routes.isEmpty()) {
                        Toast.makeText(MainActivity.this, 
                            "No routes found within budget", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    btnFindRoutes.setText("Find Routes");
                    btnFindRoutes.setEnabled(true);
                });
            }
        });
    }
    
    private void onRouteSelected(Route route) {
        if (mMap != null) {
            mMap.clear();
            
            // Add current location marker
            if (currentLocation != null) {
                mMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Start"));
            }
            
            // Add destination marker
            mMap.addMarker(new MarkerOptions()
                .position(route.getDestination())
                .title("Destination"));
            
            // Draw route polyline
            PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(route.getPolylinePoints())
                .width(8)
                .color(route.getTollCost() > 0 ? 0xFF FF6B35 : 0xFF 4CAF50); // Orange for toll, green for free
            
            mMap.addPolyline(polylineOptions);
            
            // Move camera to show entire route
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10));
        }
        
        // Start navigation (placeholder)
        Toast.makeText(this, "Starting navigation for route: $" + 
            String.format("%.2f", route.getTollCost()), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        
        // Add current location if available
        if (currentLocation != null) {
            mMap.addMarker(new MarkerOptions()
                .position(currentLocation)
                .title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}