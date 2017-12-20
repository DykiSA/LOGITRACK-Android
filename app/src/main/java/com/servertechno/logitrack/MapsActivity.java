package com.servertechno.logitrack;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;

    private Location sourcePlace;
    private Location destinationPlace;

    private String TAG = "MainActivity";

    private List<Polyline> polylines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        polylines = new ArrayList<>();

        this.configurePlaceAutocomplete();

        ImageView setRiding = (ImageView) findViewById(R.id.set_riding);
        setRiding .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRiding();
            }
        });
    }

    private void setRiding() {
        Log.d(TAG, "set riding ...");
        if (sourcePlace == null || destinationPlace == null)
            return;

        Log.d(TAG, "request riding ...");
        LatLng start = new LatLng(sourcePlace.getLatitude(), sourcePlace.getLongitude());
        LatLng end = new LatLng(destinationPlace.getLatitude(), destinationPlace.getLongitude());

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .waypoints(start, end)
                .key(getResources().getString(R.string.google_place_key))
                .build();
        routing.execute();
    }

    private void configurePlaceAutocomplete() {
        // get autocomplete fragment
        PlaceAutocompleteFragment autocompleteSource = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_start);
        PlaceAutocompleteFragment autocompleteDestination = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_destination);

        autocompleteSource.setHint("Start from");
        autocompleteDestination.setHint("Destination");

        // filter result by country
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setCountry("ID")
                .build();
        autocompleteSource.setFilter(typeFilter);
        autocompleteDestination.setFilter(typeFilter);

        sourcePlace = new Location("src");
        destinationPlace = new Location("dest");

        // set selection listener
        autocompleteSource.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.toString());

                LatLng latLng = place.getLatLng();
                sourcePlace.setLatitude(latLng.latitude);
                sourcePlace.setLongitude(latLng.longitude);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        autocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.toString());

                LatLng latLng = place.getLatLng();
                destinationPlace.setLatitude(latLng.latitude);
                destinationPlace.setLongitude(latLng.longitude);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        Log.d(TAG, "route failure");
        e.printStackTrace();
    }

    @Override
    public void onRoutingStart() {
        Log.d(TAG, "route starting ...");
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> routes, int shortestRouteIndex) {
        LatLng start = new LatLng(sourcePlace.getLatitude(), sourcePlace.getLongitude());
        LatLng end = new LatLng(destinationPlace.getLatitude(), destinationPlace.getLongitude());


        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);


        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < routes.size(); i++) {

            //In case of more than 5 alternative routes
            // int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            // polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(routes.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ routes.get(i).getDistanceValue()+": duration - "+ routes.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }

        // Start marker
        MarkerOptions options = new MarkerOptions();
        options.position(start);
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mMap.addMarker(options);

        // End marker
        options = new MarkerOptions();
        options.position(end);
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mMap.addMarker(options);
    }

    @Override
    public void onRoutingCancelled() {
        Log.d(TAG, "route cancelled");
    }
}
