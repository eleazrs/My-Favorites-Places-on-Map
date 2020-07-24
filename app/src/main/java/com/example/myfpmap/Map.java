package com.example.myfpmap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Map extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<Place> placesLatLng;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Marker marker;
    private LatLng myPosition;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        placesLatLng = (ArrayList<Place>) getIntent().getSerializableExtra("placesLatLng");
        setResult(1);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.i("Location", location.toString());
                if (marker == null ||
                        location.getLatitude() != marker.getPosition().latitude ||
                        location.getLongitude() != marker.getPosition().longitude) {
                    if (marker != null) marker.remove();
                    myPosition = new LatLng(location.getLatitude(), location.getLongitude());
                    marker = mMap.addMarker(new MarkerOptions().position(myPosition).title("My position"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 15));
                }
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.w("WARN", "Location is disabled by user");
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        else
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        for (Place place : placesLatLng) {
            addMarkerIntoMap(place);
        }

        mMap.setOnMapLongClickListener(latLng -> {
            Toast.makeText(Map.this, "Favorite Place Added!", Toast.LENGTH_SHORT).show();
            Place place = Place.builder()
                    .latitude(latLng.latitude)
                    .longitude(latLng.longitude)
                    .description("").build();
            addMarkerIntoMap(place, latLng);
            placesLatLng.add(place);
            setResult(0, new Intent().putExtra("placesLatLng", (Serializable) placesLatLng));
        });

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                Context context = getApplicationContext(); //or getActivity(), YourActivity.this, etc.

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

        int selectedPlaceFromPreviousScreen = getIntent().getIntExtra("placeSelectedFromList", -1);
        if (selectedPlaceFromPreviousScreen > -1) {
            Place selectedPlace = placesLatLng.get(selectedPlaceFromPreviousScreen);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(selectedPlace.getLatitude(), selectedPlace.getLongitude()), 15));
        }
    }

    private void addMarkerIntoMap(Place place) {
        LatLng latLng = new LatLng(place.getLatitude(), place.getLongitude());
        mMap.addMarker(new MarkerOptions().position(latLng).title("Fav details:").snippet(place.getDescription()));
    }

    private void addMarkerIntoMap(Place place, LatLng latLng) {
        String titlePosition = getDescriptionFromPosition(latLng);
        place.setDescription(titlePosition);
        mMap.addMarker(new MarkerOptions().position(latLng).title("Fav details:").snippet(titlePosition));
    }

    private String getDescriptionFromPosition(LatLng latLng) {
        Geocoder geocoder = new Geocoder(Map.this, Locale.getDefault());
        try {
            Address address = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1).get(0);
            String resultAddress = String.format(Locale.getDefault(), "%s, %s", address.getThoroughfare(), address.getSubThoroughfare())
                    + String.format(Locale.getDefault(), "\n%s, %s ", address.getLocality(), address.getPostalCode())
                    + String.format(Locale.getDefault(), "%s", address.getCountryName());
            if (resultAddress.contains("null"))
                resultAddress = String.format(Locale.getDefault(), "Lat: %.05f\nLon: %.05f", latLng.latitude, latLng.longitude);

            return resultAddress;
        } catch (IOException e) {
            Log.e("Error, Whaaaat?", e.getMessage());
            return String.format(Locale.getDefault(), "Lat: %.04f, Lon: %.04f", latLng.latitude, latLng.longitude);
        }
    }

}