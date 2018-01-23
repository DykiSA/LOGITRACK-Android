package com.servertechno.logitrack;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    private static final int APP_LOCATION_PERMISSION = 100; // request code identifier
    private final String[] locationPermissions = new String[]{
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    }; // used to request permission

    // driving status
    private String statusReady;
    private String statusOTW;
    private String statusTransit;
    private String statusDenied; // location access is denied

    private GoogleMap mMap; // main map
    private GoogleApiClient googleApiClient; // to use google play services

    private Location myLocation; // current position
    private List<LatLng> destinations; // destinations
    private List<LatLng> reachedDestination; // reached destination
    private List<Route> mRoutes; // all routes
    private List<Integer> colorsIndex; // colors for recommended routes

    private List<LatLng> ruinedAreas; // places where the road is ruined

    private String TAG = "MainActivity"; // Log

    private List<Polyline> polylines; // route lines
    private List<Marker> markers; // marked places

    private RoutingType routingType;

    private ProgressDialog progressDialog; // UI loading dialog

    /*******************************************************************
    /** App Lifecycle
    /********************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.initializeVariable();

        Button actionButton = (Button) findViewById(R.id.action_button);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onActionButtonClicked();
            }
        });

        buildGoogleApi(); // request location update
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (googleApiClient.isConnected()) googleApiClient.disconnect();
    }

    /**
     * Setup default variable
     */
    private void initializeVariable() {
        statusReady = getResources().getString(R.string.status_ready);
        statusOTW = getResources().getString(R.string.status_otw);
        statusTransit = getResources().getString(R.string.status_transit);
        statusDenied = getResources().getString(R.string.status_denied);

        destinations = new ArrayList<>();
        reachedDestination = new ArrayList<>();
        mRoutes = new ArrayList<>();
        ruinedAreas = new ArrayList<>();
        polylines = new ArrayList<>();
        markers = new ArrayList<>();

        // set destinations
        destinations.add(new LatLng(-7.3087487, 112.7322525));
        destinations.add(new LatLng(-7.261911, 112.748436));
        destinations.add(new LatLng(-7.288768, 112.7437159));

        colorsIndex = new ArrayList<>();
        colorsIndex.add(getResources().getColor(R.color.my_green));
        colorsIndex.add(getResources().getColor(R.color.my_yellow));
        colorsIndex.add(getResources().getColor(R.color.my_red));
    }

    /*******************************************************************
     /** Action Button
     /********************************************************************/

    /**
     * When button is clicked
     */
    private void onActionButtonClicked() {
        TextView statusLabel = (TextView) findViewById(R.id.my_status);
        String statusText = statusLabel.getText().toString().toLowerCase();

        Log.d(TAG, statusText); // print

        // Ready -> OTW
        if (Objects.equals(statusText, statusReady.toLowerCase())) {
            // calculate first trip
            Log.d(TAG, "calculate first trip");
            requestFirstTrip();
        }
        // OTW -> Marking
        else if (Objects.equals(statusText, statusOTW.toLowerCase())) {
            ruinedAreas.add(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()));
        }
        // Transit -> Resume
        else if (Objects.equals(statusText, statusTransit.toLowerCase())) {

        }
        // Access Denied -> Ready
        else if (Objects.equals(statusText, statusDenied.toLowerCase())) {
            requestLocationPermissions();
        }
    }

    public void setStatusAndButton(StatusEnum statusEnum) {
        Button button = (Button) findViewById(R.id.action_button);
        TextView statusLabel = (TextView) findViewById(R.id.my_status);

        switch (statusEnum) {
            case LOCATION_ACCESS_DENIED:
                statusLabel.setText(statusDenied);
                statusLabel.setTextColor(getResources().getColor(R.color.colorAccent));
                button.setBackground(getResources().getDrawable(R.drawable.my_blue_button));
                button.setText(R.string.btn_allow);
                break;
            case READY:
                statusLabel.setText(statusReady);
                statusLabel.setTextColor(getResources().getColor(R.color.my_blue));
                button.setBackground(getResources().getDrawable(R.drawable.my_green_button));
                button.setText(R.string.btn_start);
                break;
            case ON_THE_WAY:
                statusLabel.setText(statusOTW);
                statusLabel.setTextColor(getResources().getColor(R.color.my_green));
                button.setBackground(getResources().getDrawable(R.drawable.my_red_button));
                button.setText(R.string.btn_mark);
                break;
            case TRANSIT:
                statusLabel.setText(statusTransit);
                statusLabel.setTextColor(getResources().getColor(R.color.my_yellow));
                button.setBackground(getResources().getDrawable(R.drawable.my_blue_button));
                button.setText(R.string.btn_continue);
                break;
        }
    }

    /**
     * Display loading dialog
     * @param message message to be displayed
     */
    private void showLoadingDialog(String message) {
        hideLoadingDialog();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    /**
     * Hide loading dialog
     */
    private void hideLoadingDialog() {
        if (progressDialog == null) return;
        if (!progressDialog.isShowing()) return;
        progressDialog.hide();
    }

    /*******************************************************************
    /** Google Map
    /********************************************************************/

    /**
     * When Map is ready
     * @param googleMap google map object
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    /**
     * Enable my location in google map
     */
    public void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestLocationPermissions();
            setStatusAndButton(StatusEnum.LOCATION_ACCESS_DENIED);
            return;
        }

        try {
            LocationManager lm = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            myLocation = lm != null ? lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) : null;
            Log.d(TAG, "Last known: "+myLocation.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMap.setMyLocationEnabled(true);
    }

    /*******************************************************************
    /** Location Access Permission
    /********************************************************************/

    public void requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Akses Lokasi Ditolak");
            alert.setMessage("Aplikasi ini tidak dapat mengakses lokasi anda. Izinkan aplikasi untuk mengakses lokasi anda?");
            alert.setCancelable(false);
            alert.setPositiveButton("Izinkan", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(MapsActivity.this, locationPermissions, APP_LOCATION_PERMISSION);
                }
            });
            alert.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setStatusAndButton(StatusEnum.LOCATION_ACCESS_DENIED);
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, locationPermissions, APP_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case APP_LOCATION_PERMISSION:
                boolean allowed = false;
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED)
                            allowed = true;
                    }
                }

                if (allowed) {
                    setStatusAndButton(StatusEnum.READY);
                    enableMyLocation(); // re-enable my location

                    if (googleApiClient.isConnected())
                        onConnected(null);
                } else {
                    setStatusAndButton(StatusEnum.READY);
                }
                break;
        }
    }

    /*******************************************************************
    /** RealTime Location Update
    /********************************************************************/

    public void buildGoogleApi() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("GoogleApi", "onConnect");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = createLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this
        );
    }

    @Override
    public void onConnectionSuspended(int i) {
        // code...
    }

    private LocationRequest createLocationRequest() {
        int INTERVAL = 10000;
        int FAST_INTERVAL = 5000;
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FAST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "myLocation"+ location.toString());
        myLocation = location;

        Location destLocation = new Location("destination");
        for (LatLng destination : destinations) {
            destLocation.setLatitude(destination.latitude);
            destLocation.setLongitude(destination.longitude);

            if (destLocation.distanceTo(myLocation) <= 10) { // a destinations is reached
                if (destinations.equals(reachedDestination)) { // all destination is already reached
                    // set status as ready
                    setStatusAndButton(StatusEnum.READY);
                    // finish trip
                    clearRecentTrip();
                } else { // still need to continue to next trip
                    // set status as transit
                    setStatusAndButton(StatusEnum.TRANSIT);
                    reachedDestination.add(destination);
                }
            }
        }

    }

    /*******************************************************************
    /** Routing Process
    /********************************************************************/

    private void requestFirstTrip() {
        routingType = RoutingType.FIRST_TRIP;
        showLoadingDialog("Memindai rute ...");

        for (LatLng destination : destinations) {
            LatLng start = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .waypoints(start, destination)
                .key(getResources().getString(R.string.google_place_key))
                .build();
            routing.execute();
        }
    }

    private void requestNextTrip() {
        routingType = RoutingType.NEXT_TRIP;
        showLoadingDialog("Memindai rute ...");

        LatLng start = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        LatLng destination = getNearestDestination();
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .waypoints(start, destination)
                .key(getResources().getString(R.string.google_place_key))
                .build();
        routing.execute();
    }

    private LatLng getNearestDestination() {
        List<LatLng> dests = destinations;
        Collections.sort(dests, new Comparator<LatLng>() {
            @Override
            public int compare(LatLng o1, LatLng o2) {
                Location destLocation1 = new Location("destination");
                Location destLocation2 = new Location("destination");

                destLocation1.setLatitude(o1.latitude);
                destLocation1.setLongitude(o1.longitude);
                destLocation2.setLatitude(o2.latitude);
                destLocation2.setLongitude(o2.longitude);

                Float distance1 = destLocation1.distanceTo(myLocation);
                Float distance2 = destLocation1.distanceTo(myLocation);
                return distance1.compareTo(distance2);
            }
        });

        return dests.get(0);
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        // LOG
        Log.d(TAG, "routing failure");
        e.printStackTrace();

        // show alert
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Gagal mendapatkan rute");
        alert.setMessage(e.getMessage() + ". Silahkan coba lagi");
        alert.setCancelable(false);
        alert.show();

        // throw state back
        if (routingType == RoutingType.FIRST_TRIP) {
            setStatusAndButton(StatusEnum.READY); // back to ready status
        } else {
            setStatusAndButton(StatusEnum.TRANSIT); // back to transit
        }
    }
//
    @Override
    public void onRoutingStart() {
        Log.d(TAG, "routing started ...");
    }
//

    /**
     * Request routing success
     * @param routes routes you get
     * @param shortestRouteIndex nearby best route
     */
    @Override
    public void onRoutingSuccess(ArrayList<Route> routes, int shortestRouteIndex) {
        switch (routingType) {
            case FIRST_TRIP:
                mRoutes.add(routes.get(0));
                if (mRoutes.size() == 3) {
                    drawFirstTripRoute();
                }
                break;
            case NEXT_TRIP:
                mRoutes = routes;
                break;
        }

        Log.d("shortestRouteIndex", String.valueOf(shortestRouteIndex));

//        LatLng start = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
//        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
//        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
//
//        mMap.moveCamera(center);
//        mMap.animateCamera(zoom);
//
    }

    @Override
    public void onRoutingCancelled() {
//        Log.d(TAG, "route cancelled");
    }


    private void drawFirstTripRoute() {
        progressDialog.setMessage("Menganalisa rute");
        // sorting routes based on nearest destination
        Collections.sort(mRoutes, new Comparator<Route>() {
            @Override
            public int compare(Route o1, Route o2) {
                Integer distance1 = o1.getDistanceValue();
                Integer distance2 = o2.getDistanceValue();
                return distance1.compareTo(distance2);
            }
        });

        progressDialog.setMessage("Menggambar jalur");
        for (int i = 0; i < mRoutes.size(); i++) {
            Route route = mRoutes.get(i);
            int polyColor = i < 3 ? colorsIndex.get(i) : colorsIndex.get(colorsIndex.size() - 1);

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(10);
            polyOptions.color(polyColor);
            polyOptions.addAll(route.getPoints());

            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
        }

        for (LatLng destination : destinations) {
            MarkerOptions options = new MarkerOptions();
            options.position(destination);
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            Marker marker = mMap.addMarker(options);
            markers.add(marker);
        }

        // set status as otw
        setStatusAndButton(StatusEnum.ON_THE_WAY);

        hideLoadingDialog();
    }

    public void clearRecentTrip() {
        markers.clear();
        polylines.clear();
        reachedDestination.clear();
        mRoutes.clear();
    }

//
//    public void drawDistanceLabel(LatLng position, String label) {
//        MarkerOptions options = new MarkerOptions();
//        options.position(position);
//        options.title(label);
//        options.flat(true);
//        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
//        Marker marker = mMap.addMarker(options);
//        markers.add(marker);
//    }
//
//    private void deleteAllMarks() {
//        markers.clear();
//        polylines.clear();
//    }
}

enum StatusEnum {
    LOCATION_ACCESS_DENIED,
    READY,
    TRANSIT,
    ON_THE_WAY
}

enum RoutingType {
    FIRST_TRIP,
    NEXT_TRIP
}
