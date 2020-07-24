package com.example.myfpmap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private List<Place> placesLatLng = new ArrayList<>();
    private ArrayAdapter<String> placeArrayAdapter;
    private ListView listView;
    private int placeSelectedFromList = -1;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getSharedPreferences("com.example.myfpmap", Context.MODE_PRIVATE);
        String placesSerialized = sharedPreferences.getString("placesLatLng", "");

        if (placesSerialized.equals("") || !placesSerialized.contains("description")) {
            LatLng madridLatLng = new LatLng(40.4723138, -3.6846261);
            placesLatLng.add(Place.builder()
                    .description("Madrid Lovely City!") //#0x2764D Red heart
                    .latitude(madridLatLng.latitude)
                    .longitude(madridLatLng.longitude)
                    .build());
            try {
                placesSerialized = new ObjectMapper().writeValueAsString(placesLatLng);
            } catch (JsonProcessingException e) {
                Log.e("Error processing madridLatLng", e.toString());
            }
            sharedPreferences.edit().putString("placesLatLng", placesSerialized).apply();
        } else {
            try {
                placesLatLng = new ObjectMapper().readValue(placesSerialized, new TypeReference<List<Place>>() {
                });
            } catch (JsonProcessingException e) {
                Log.e("ERROR!", "Processing list from sharedPreferences");
            }
        }

        fillListView();

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            placeSelectedFromList = i;
            showMap(view);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == 0) {
            placesLatLng = (ArrayList<Place>) Optional.ofNullable(data.getSerializableExtra("placesLatLng")).orElse(new ArrayList<>());
            String placesSerialized = "";
            try {
                placesSerialized = new ObjectMapper().writeValueAsString(placesLatLng);
            } catch (JsonProcessingException e) {
                Log.e("Error processing madridLatLng", e.toString());
            }
            sharedPreferences.edit().putString("placesLatLng", placesSerialized).apply();
            fillListView();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void fillListView() {
        Log.d("Places: ", this.placesLatLng.toString());
        List<String> places = placesLatLng.stream().map(Place::getDescription).collect(Collectors.toList());
        placeArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, places);

        listView = findViewById(R.id.listView);
        listView.setAdapter(placeArrayAdapter);
    }

    public void showMap(View view) {
        Intent mapActivity = new Intent(getApplicationContext(), Map.class);

        mapActivity.putExtra("placeSelectedFromList", placeSelectedFromList);
        mapActivity.putExtra("placesLatLng", (Serializable) placesLatLng);
        startActivityForResult(mapActivity, 0);
    }
}