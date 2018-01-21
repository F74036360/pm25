package com.example.joan.myapplication;

import android.*;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

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

public class InfoActivity extends FragmentActivity implements LocationListener  {
    public int moved = 0;

    public Button Guardian;
    public Button TaiwanInfo;
    public Button Listen;
    public String user_pm25;
    public TextView textview;
    public TextView warningTextview;
    public TextToSpeech talk_obj;
    public int MY_DATA_CHECK_CODE = 2;
    private static final String gps_lat = "gps_lat";
    private static final String gps_lon = "gps_lon";
    private static final String pm25 = "s_d0";
    public static Location loc;
    LocationManager locationManager;
    String provider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Guardian = (Button) findViewById(R.id.Guardian);
        TaiwanInfo = (Button) findViewById(R.id.Taiwan);
        Listen = (Button) findViewById(R.id.Listen);

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
        statusCheck();

        Guardian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(InfoActivity.this, InfoActivity.class);
                startActivity(intent);
                InfoActivity.this.finish();
            }
        });

        TaiwanInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moved = 0;
                Intent intent = new Intent();
                intent.setClass(InfoActivity.this, MainActivity.class);
                startActivity(intent);
                InfoActivity.this.finish();
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
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 500, 0, this);
            if (location != null) onLocationChanged(location);
            else location = locationManager.getLastKnownLocation(provider);

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

    private class JsonTask extends AsyncTask<String, String, String> {
        ArrayList<LatLng> alllatlng = new ArrayList<>();
        ArrayList<String> allpm25 = new ArrayList<>();

        protected String doInBackground(String... params) {
            Log.e("into back", "");

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
                    Log.e("into back", "getpm25info");
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
    }

    public void setInfoText() {
        textview = (TextView) findViewById(R.id.textView);
        warningTextview = (TextView) findViewById(R.id.warningmsg);
        if (user_pm25 != null) {
            double pm = Double.parseDouble(user_pm25);
            if (pm <= 50 ) {
                textview.setText("pm2.5: " + user_pm25 + "  (良好)");
                warningTextview.setText("正常戶外活動。\n\n自我防護措施\n1. 規律作息、多喝水、適當運動\n2.多吃深色蔬果");
            } else if (pm > 50 && pm <= 100 ) {
                textview.setText("pm2.5: " + user_pm25 + "  (普通)");
                warningTextview.setText("正常戶外活動\n\n自我防護措施\n1. 規律作息、多喝水、適當運動\n2.多吃深色蔬果\n3.洗手洗臉清洗鼻腔\n4.室外戴口罩、室內使用空氣清淨機");
            } else if (pm > 100 && pm <= 150 ) {
                textview.setText("pm2.5: " + user_pm25 + "  (對敏感族群不健康)");
                warningTextview.setText("有心臟、呼吸道及心血管疾病的成人與孩童感受到癥狀時，應考慮減少體力消耗，特別是減少戶外活動。\n\n自我防護措施\n1. 規律作息、多喝水、適當運動\n2.多吃深色蔬果\n3.洗手洗臉清洗鼻腔\n4.室外戴口罩、室內使用空氣清淨機");
            } else if (pm > 150 && pm <= 200 ) {
                textview.setText("pm2.5: " + user_pm25 + "  (對所有族群不健康)");
                warningTextview.setText("任何人如果有不適，如眼痛，咳嗽或喉嚨痛等，應該考慮減少戶外活動。\n有心臟、呼吸道及心血管疾病的成人與孩童，應減少體力消耗，特別是減少戶外活動。\n老年人應減少體力消耗。\n具有氣喘的人可能需增加使用吸入劑的頻率。\n\n自我防護措施\n1. 規律作息、多喝水、適當運動\n2.多吃深色蔬果\n3.洗手洗臉清洗鼻腔\n4.室外戴口罩、室內使用空氣清淨機");
            } else if (pm > 201 && pm <= 300 ) {
                textview.setText("pm2.5: " + user_pm25 + "  (非常不健康)");
                warningTextview.setText("任何人如果有不適，如眼痛，咳嗽或喉嚨痛等，應該考慮減少戶外活動。\n有心臟、呼吸道及心血管疾病的成人與孩童，應減少體力消耗，特別是減少戶外活動。\n老年人應減少體力消耗。\n具有氣喘的人可能需增加使用吸入劑的頻率。\n\n自我防護措施\n1. 規律作息、多喝水、適當運動\n2.多吃深色蔬果\n3.洗手洗臉清洗鼻腔\n4.室外戴口罩、室內使用空氣清淨機");
            } else if (pm > 301 && pm <= 500 ) {
                textview.setText("pm2.5: " + user_pm25 + "  (危害)");
                warningTextview.setText("任何人如果有不適，如眼痛，咳嗽或喉嚨痛等，應減少體力消耗，特別是減少戶外活動。\n有心臟、呼吸道及心血管的成人與孩童，以及老年人應避免體力消耗，特別是避免戶外活動。\n具有氣喘的人可能需增加使用吸入劑的頻率。\n\n自我防護措施\n1. 規律作息、多喝水、適當運動\n2.多吃深色蔬果\n3.洗手洗臉清洗鼻腔\n4.室外戴口罩、室內使用空氣清淨機");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* getMenuInflater().inflate(R.menu.activity_main, menu); */
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;
        String url = "https://pm25.lass-net.org/data/last-all-airbox.json";
        new JsonTask().execute(url);
        setInfoText();
    }


    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

}
