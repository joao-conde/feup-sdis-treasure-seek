package sdis.treasureseek;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import sdis.controller.Controller;
import sdis.model.Treasure;
import sdis.util.ParseMessageException;
import sdis.util.Utils;

public class TreasureMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Controller controller;

    private final int REQUEST_LOCATION = 1;
    private final int NOTIFICATIONS_PORT = 4012;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    LocationCallback locationCallback;

    private ArrayList<Marker> treasureMarkers;
    private ArrayList<Marker> foundTreasuresMarkers;

    private ServerSocket serverSocket;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treasure_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        controller = Controller.getInstance();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallBackClass();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        TreasureMapActivity.this.setTitle(getString(R.string.treasure_map));

        try {
             serverSocket = new ServerSocket(NOTIFICATIONS_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }


        new Thread(new NotificationListener(this)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class NotificationListener implements Runnable {

        Context context;

        public NotificationListener(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {

                while(true) {

                    Socket notificationSocket = serverSocket.accept();
                    processNotification(notificationSocket,context);

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private void processNotification(Socket notificationSocket, Context context) throws IOException {

        Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(notificationSocket.getInputStream())));
        final String newTreasure = scanner.nextLine();
        Utils.showNotification(this,"New Treasure","A New Treasure has been planted: " + newTreasure);

        try {
            controller.requestAllTreasures();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseMessageException e) {
            e.printStackTrace();
        }

        Handler mainHandler = new Handler(context.getMainLooper());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {

                drawTreasures(newTreasure);

            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();

        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback );
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        LatLng feupLocation = new LatLng(41.178539, -8.596096);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }

        else {
            mMap.setMyLocationEnabled(true);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        }


        mMap.moveCamera(CameraUpdateFactory.newLatLng(feupLocation));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        drawTreasures(null);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setTitle((String)controller.getLoggedUser().getValue("name"));
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.action_ranking) {

            Intent intent = new Intent(TreasureMapActivity.this, RankingActivity.class);
            startActivity(intent);

        }

        return super.onOptionsItemSelected(item);

    }

    private void drawTreasures(String newTreasure) {

        treasureMarkers = new ArrayList<>();
        foundTreasuresMarkers = new ArrayList<>();
        mMap.clear();


        for (Treasure treasure : controller.getAllTreasures()) {

            float colorTreasure = BitmapDescriptorFactory.HUE_RED;
            String treasureDesc = (String) treasure.getValue("description");

            if(newTreasure != null && newTreasure.equals(treasureDesc))
                colorTreasure = BitmapDescriptorFactory.HUE_BLUE;


            LatLng pos = new LatLng((double) treasure.getValue("latitude"), (double) treasure.getValue("longitude"));
            Marker marker = mMap.addMarker(new MarkerOptions().position(pos).title(treasureDesc).icon(BitmapDescriptorFactory.defaultMarker(colorTreasure)));

            treasureMarkers.add(marker);

            mMap.setOnMarkerClickListener(new MarkerListener());

        }

        for(Treasure treasure : (ArrayList<Treasure>)controller.getLoggedUser().getValue("foundTreasures")) {

            LatLng pos = new LatLng((double) treasure.getValue("latitude"), (double) treasure.getValue("longitude"));
            Marker marker = mMap.addMarker(new MarkerOptions().position(pos).title((String) treasure.getValue("description")).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            foundTreasuresMarkers.add(marker);

            mMap.setOnMarkerClickListener(new MarkerListener());

        }

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_LOCATION) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                mMap.setMyLocationEnabled(true);
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());


            }

        }

    }


    class LocationCallBackClass extends LocationCallback {

        @SuppressLint("MissingPermission")
        @Override
        public void onLocationResult(LocationResult locationResult) {

            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                mMap.setMyLocationEnabled(true);
                //Location location = locationList.get(locationList.size() - 1);

                //move map camera
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()), 17));

            }

        }
    }

    class MarkerListener implements GoogleMap.OnMarkerClickListener {


        @Override
        public boolean onMarkerClick(Marker marker) {

            int treasureIndex = treasureMarkers.indexOf(marker);
            int foundIndex = foundTreasuresMarkers.indexOf(marker);

            int index = treasureIndex != -1 ? treasureIndex : foundIndex;

            Intent intent = new Intent(TreasureMapActivity.this, TreasureActivity.class);
            intent.putExtra("treasureIndex", index);
            intent.putExtra("found", treasureIndex == -1);


            startActivity(intent);


            return false;

        }

    }


    @Override
    protected void onRestart() {
        super.onRestart();
        drawTreasures(null);
    }
}
