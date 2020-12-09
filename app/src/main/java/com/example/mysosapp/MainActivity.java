package com.example.mysosapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.geojson.Point.fromLngLat;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

// classes to calculate a route

// classes needed to launch navigation UI


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {
    MapboxMap mapboxMap;
    MapView mapView;
    LocationComponent locationcomponent;
    PermissionsManager permissionsManager;
    DirectionsRoute currentRoute;
    NavigationMapRoute navigationMapRoute;
    private DatePicker button;
    LocationComponent locationComponent;




    public void startNavigationBtnClick(View v) {
        boolean simulateRoute = true;
        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(simulateRoute)
                .build();

        NavigationLauncher.startNavigation(MainActivity.this, options);


    }

    @Override
    public void onExplanationNeeded(List<String> permissionToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {

            Toast.makeText(getApplicationContext(), "permission not granted", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        com.mapbox.geojson.Point destinationPoint = fromLngLat(point.getLongitude(),point.getLatitude());
        com.mapbox.geojson.Point originPoint = fromLngLat(locationcomponent.getLastKnownLocation().getLongitude(), locationcomponent.getLastKnownLocation().getLatitude());
       

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null)
        {
            source.setGeoJson(Feature.fromGeometry((Geometry) destinationPoint));
        }
        
        getRoute(originPoint, destinationPoint);
        button.setEnabled(true);
        button.setBackgroundResource(R.color.mapbox_blue);
        return true;
    }

    private void getRoute(com.mapbox.geojson.Point originPoint, com.mapbox.geojson.Point destinationPoint) {
    }


    private void getRoute(Point originPoint, Point destinationPoint) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                //.origin(originPoint)
                //.destination(destinationPoint)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response


                       /* Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                        } else if (response.body() != null && response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        } */
                        currentRoute = response.body().routes().get(0);


                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);

                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        
                    }

                });


    }
    // variables for calculating and drawing a route

    private static final String TAG = "DirectionsActivity";
    // variables needed to initialize navigation


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "sk.eyJ1IjoidmlzaGFsMzExIiwiYSI6ImNrNXZncmQ4czAxcTYzZ21rN2NpbmRodTMifQ.6f9wiOgkHwXF2w4WqZNPWw");
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

    }
    
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        this.mapboxMap.setMinZoomPreference(15);
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                addDestinationIconLayer(style);
                mapboxMap.addOnMapClickListener(MainActivity.this);
            }
        });


    }

    private void addDestinationIconLayer(Style style)
    {
        style.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(),R.drawable.mapbox_marker_icon_default));
        GeoJsonSource getJasonSource = new GeoJsonSource("destination-source-id");
        style.addSource(getJasonSource);

        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-id", "destination-source-id");
        destinationSymbolLayer.withProperties(iconImage("destination-icon-id"),iconAllowOverlap(true),
                iconIgnorePlacement(true));

        style.addLayer(destinationSymbolLayer);


    }

    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationcomponent = mapboxMap.getLocationComponent();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }

            locationcomponent.activateLocationComponent(this, loadedMapStyle);
            locationcomponent.setLocationComponentEnabled(true);
            locationcomponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


        @Override
        public void onRequestPermissionsResult ( int requestCode, @NonNull String[] permission, @NonNull int[] grantResults){
            permissionsManager.onRequestPermissionsResult(requestCode, permission, grantResults);
        }

        @Override
        protected void onStart () {
            super.onStart();
            mapView.onStart();
        }

        @Override
        protected void onResume () {
            super.onResume();
            mapView.onResume();
        }


        @Override
        protected void onPause () {
            super.onPause();
            mapView.onPause();
        }

        @Override
        protected void onStop () {
            super.onStop();
            mapView.onStop();
        }

        @Override
        public void onSaveInstanceState (Bundle outState, PersistableBundle outPersistableState){
            super.onSaveInstanceState(outState, outPersistableState);
            mapView.onSaveInstanceState(outState);
        }

        @Override
        protected void onDestroy () {
            super.onDestroy();
            mapView.onDestroy();
        }

        @Override
        public void onLowMemory () {
            super.onLowMemory();
            mapView.onLowMemory();
        }

    }

