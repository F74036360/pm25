package com.example.joan.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public int moved = 0;
    //用戶所在地的pm2.5值
    public String user_pm25;
    public static Context ctx;
    public Button Guardian;
    public Button TaiwanInfo;
    public Button Listen;
    public TextToSpeech talk_obj;
    public TextView textview;
    public int MY_DATA_CHECK_CODE = 2;
    public static Location loc;
    public static GoogleMap mMap;
    private static final String gps_lat = "gps_lat";
    private static final String gps_lon = "gps_lon";
    private static final String pm25 = "s_d0";
    LocationManager locationManager;
    String provider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
        statusCheck();
        ctx = getApplicationContext();
        Guardian = (Button) findViewById(R.id.Guardian);
        TaiwanInfo = (Button) findViewById(R.id.Taiwan);
        Listen = (Button) findViewById(R.id.Listen);

        Guardian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moved = 0;
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, InfoActivity.class);
                startActivity(intent);
                MainActivity.this.finish();
            }
        });

        TaiwanInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moved = 0;
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MainActivity.class);
                startActivity(intent);
                MainActivity.this.finish();
            }
        });

        Listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user_pm25 != null) {
                    Intent checkIntent = new Intent();
                    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                    startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
                }
            }
        });

        locationManager = (LocationManager) getSystemService(
                Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        if (provider != null && !provider.equals("")) {
            if (!provider.contains("gps")) { // if gps is disabled
                final Intent poke = new Intent();
                poke.setClassName("com.android.settings",
                        "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                sendBroadcast(poke);
            }
            // Get the location from the given provider
            Location location = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 500, 0, this);
            if (location != null) onLocationChanged(location);
            else location = locationManager.getLastKnownLocation(provider);
           /* if (location != null)
                onLocationChanged(location);
            else
                Toast.makeText(getBaseContext(), "Location can't be retrieved",
                        Toast.LENGTH_SHORT).show();*/

        } else {
            Toast.makeText(getBaseContext(), "No Provider Found",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(
                Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                "Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false).setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,
                                        final int id) {
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,
                                        final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* getMenuInflater().inflate(R.menu.activity_main, menu); */
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Getting reference to TextView tv_longitude
        //TextView tvLongitude = (TextView) findViewById(R.id.tv_longitude);
        // Getting reference to TextView tv_latitude
        //T/extView tvLatitude = (TextView) findViewById(R.id.tv_latitude);
        // Setting Current Longitude
        loc = location;
        //  tvLongitude.setText("Longitude:" + location.getLongitude());
        // Setting Current Latitude
        //  tvLatitude.setText("Latitude:" + location.getLatitude());
        String url = "https://pm25.lass-net.org/data/last-all-airbox.json";
        new JsonTask().execute(url);


    }


    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) // 如果TTS Engine有成功找到的話
            {
                talk_obj = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            Locale locale = Locale.TAIWAN;
                            if (talk_obj.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                                int result = talk_obj.setLanguage(locale);
                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    Log.e("TTS", "This Language is not supported");
                                } else {
                                    TaiwanInfo.setEnabled(true);
                                    Guardian.setEnabled(true);
                                    Listen.setEnabled(true);
                                    speakOut();
                                }
                            } else {
                                Log.e("TTS", "Initilization Failed!");
                            }
                        } else {
                            Log.d("lang", "onInit:error");
                        }
                    }


                });
                Log.d("onActivityResult", "onInit");
            } else // 如果 TTS 沒有安裝的話 , 要求安裝
            {
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    private void speakOut() {
        if (Double.parseDouble(user_pm25) >= 50) {
            String tex = "今日的懸浮微粒指數為 " + user_pm25 + " ，請盡量減少外出";
            talk_obj.speak(tex, TextToSpeech.QUEUE_FLUSH, null);//TextToSpeech.QUEUE_ADD 為目前的念完才念
        }

    }

    @Override
    public void onDestroy() {
        if (talk_obj != null) {
            talk_obj.stop();
            talk_obj.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(new LatLng(loc.getLatitude(), loc.getLongitude()))
                        .zoom(15)
                        .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private class JsonTask extends AsyncTask<String, String, String> {
        ArrayList<LatLng> alllatlng = new ArrayList<>();
        ArrayList<String> allpm25 = new ArrayList<>();

        protected String doInBackground(String... params) {
            Log.e("inot back", "");

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            Double pm25_ori = 0.0;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                    Log.e("into back", "yaaaa");
                }
                String result = buffer.toString();
                JSONObject jObj = new JSONObject(result);
                JSONArray arr = jObj.getJSONArray("feeds");
                alllatlng.clear();
                allpm25.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.getJSONObject(i);

                    Double lass_lat = Double.parseDouble(c.getString(gps_lat));
                    Double lass_lon = Double.parseDouble(c.getString(gps_lon));
                    alllatlng.add(new LatLng(lass_lat, lass_lon));
                    String lass_pm25 = c.getString(pm25);
                    allpm25.add(lass_pm25);
                    //抓到用戶所在地的pm2.5
                    if (getDistance(lass_lat, lass_lon, loc.getLatitude(), loc.getLongitude()) == 1) {
                        if (Double.parseDouble(lass_pm25) > pm25_ori) {
                            pm25_ori = Double.parseDouble(lass_pm25);
                            Log.e("pm25", lass_pm25);
                            user_pm25 = lass_pm25;
                        }

                    }
                }

                return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;

        }

        private double rad(double d) {
            return d * Math.PI / 180.0;
        }

        private int getDistance(Double lass_lat, Double lass_lon, double latitude, double longitude) {
            double EARTH_RADIUS = 6378137;
            double radLat1 = rad(lass_lat);
            double radLat2 = rad(latitude);
            double a = radLat1 - radLat2;
            double b = rad(lass_lon) - rad(longitude);
            double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                    + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
            s = s * EARTH_RADIUS;
            s = Math.round(s * 10000) / 10000;
            if (s <= 10000) return 1;
            else return 0;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (moved == 0) moveMap(alllatlng, allpm25);
            Log.e("user pm2.5", "" + Double.parseDouble(user_pm25));

        }
    }

    public void moveMap(ArrayList<LatLng> all_poi_latlng, ArrayList<String> pm25) {
        // 建立地圖攝影機的位置物件
        moved = 1;
        Log.e("into movemap", "GG");
        mMap.clear();
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //Initialize Google Play Services
        for (int i = 0; i < all_poi_latlng.size(); i++) {
            Double int_pm25 = Double.parseDouble(pm25.get(i));
            if (int_pm25 <= 11) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(75))
                        .title(pm25.get(i)));
            } else if (int_pm25 > 11 && int_pm25 <= 23) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(100))
                        .title(pm25.get(i)));
            } else if (int_pm25 > 23 && int_pm25 <= 35) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(120))
                        .title(pm25.get(i)));
            } else if (int_pm25 > 35 && int_pm25 <= 41) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                        .title(pm25.get(i)));
            } else if (int_pm25 > 42 && int_pm25 <= 53) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        .title(pm25.get(i)));
            } else if (int_pm25 > 53 && int_pm25 <= 58) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                        .title(pm25.get(i)));
            } else if (int_pm25 > 58 && int_pm25 <= 70) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .title(pm25.get(i)));
            } else if (int_pm25 > 70) {
                mMap.addMarker(new MarkerOptions()
                        .position(all_poi_latlng.get(i)).icon(BitmapDescriptorFactory.defaultMarker(255))
                        .title(pm25.get(i)));
            }


        }
        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(new LatLng(loc.getLatitude(), loc.getLongitude()))
                        .zoom(7)
                        .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        textview = (TextView) findViewById(R.id.textView);
        textview.setText("pm2.5: " + user_pm25 + "  ");


        if (user_pm25 != null && Double.parseDouble(user_pm25) >= 50) {
            Toast.makeText(ctx, "今日的懸浮微粒指數為 " + user_pm25 + "  ", Toast.LENGTH_LONG).show();
        }
    }


}
