package com.example.landpooling.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import cn.pedant.SweetAlert.SweetAlertDialog;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;


import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.landpooling.R;

import com.google.gson.JsonObject;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.layers.HillshadeLayer;
import com.mapbox.mapboxsdk.style.sources.RasterDemSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.backgroundOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.hillshadeHighlightColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.hillshadeShadowColor;


public class LocationComponentActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener, MapboxMap.OnMarkerClickListener {

    private static final String TAG = "OffManActivity";
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    // JSON encoding/decoding
    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    // UI elements
    private MapView mapView;
    private MapboxMap map;
    private ProgressBar progressBar;
    private Button downloadButton;
    private Button listButton;

    private boolean isEndNotified;
    private int regionSelected;

    // Offline objects
    private OfflineManager offlineManager;
    private OfflineRegion offlineRegion;

    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILISECONDS =1000L;
    private long DEFAULT_MAX_WAIT_TIME= DEFAULT_INTERVAL_IN_MILISECONDS*5;
    private LocationComponentActivityLocationCallback callback= new LocationComponentActivityLocationCallback(this);

    private MapboxMap mapboxMap;

    private static final String LAYER_ID = "hillshade-layer";
    private static final String SOURCE_ID = "hillshade-source";
    private static final String SOURCE_URL = "mapbox://mapbox.terrain-rgb";
    private static final String HILLSHADE_HIGHLIGHT_COLOR = "#008924";
    private Button one;

    private Button three;
    private Button two;
    public TextView poicount;
    public int count=0;
    public int satellite=0;
    public int initialcount=0;
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private CarmenFeature home;
    private CarmenFeature work;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";
    private String status = "Unmarked";
    private int totalpoi=0;
    private File apkStorage=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.branded);

        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1Ijoic2tzaGl0aXoxIiwiYSI6ImNqcmJ2czBjODBhMTgzeWxwM2t1djJuaXUifQ.wlFktg-soH3B_pqVyJj2Ig");
        setContentView(R.layout.fragment_home);
        mapView = findViewById(R.id.mapView);
        poicount=findViewById(R.id.textView);
        converter();
        createFile();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        map=mapboxMap;
        LocationComponentActivity.this.mapboxMap = mapboxMap;


        mapboxMap.setStyle(Style.OUTDOORS, new Style.OnStyleLoaded() {

//        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/mapbox/cjerxnqt3cgvp2rmyuxbeqme7"),
//                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        initSearchFab();


// Add the symbol layer icon to map for future use
                        style.addImage(symbolIconId, BitmapFactory.decodeResource(
                                LocationComponentActivity.this.getResources(), R.drawable.blue_marker));
// Create an empty GeoJSON source using the empty feature collection
                        setUpSource(style);
// Set up a new symbol layer for displaying the searched location's feature coordinates
                        setupLayer(style);
                        RasterDemSource rasterDemSource = new RasterDemSource(SOURCE_ID, SOURCE_URL);
                        style.addSource(rasterDemSource);
// Create and style a hillshade layer to add to the map
                        HillshadeLayer hillshadeLayer = new HillshadeLayer(LAYER_ID, SOURCE_ID).withProperties(
                                hillshadeHighlightColor(Color.parseColor(HILLSHADE_HIGHLIGHT_COLOR)),
                                hillshadeShadowColor(Color.BLACK)
                        );
// Add the hillshade layer to the map
                        style.addLayerBelow(hillshadeLayer, "aerialway");
                        findViewById(R.id.locationbutton).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Style style = mapboxMap.getStyle();
                                enableLocationComponent(style);
                            }


                        });
                        findViewById(R.id.fab_layer_toggle).setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View view) {
                                showAlertDialog();
                                            }
                                        });
                        findViewById(R.id.toggle).setOnClickListener(new View.OnClickListener() {

                                                                         @Override
                                                                         public void onClick(View view) {
                                                                             satellite=satellite+1;
                                                                             if(satellite%2==0){
                                                                                 mapboxMap.setStyle(Style.OUTDOORS, new Style.OnStyleLoaded() {
                                                                                     @Override
                                                                                     public void onStyleLoaded(@NonNull Style style) {

                                                                                     }
                                                                                 });

                                                                             }else{
                                                                                 mapboxMap.setStyle(Style.SATELLITE_STREETS, new Style.OnStyleLoaded() {
                                                                                     @Override
                                                                                     public void onStyleLoaded(@NonNull Style style) {

                                                                                     }
                                                                                 });

                                                                             }

                                                                         }
                                                                     });

                        progressBar = findViewById(R.id.progress_bar);
// Set up the offlineManager
                        offlineManager = OfflineManager.getInstance(LocationComponentActivity.this);
// Bottom navigation bar button clicks are handled here.
// Download offline button
                        downloadButton = findViewById(R.id.download_button);
                        downloadButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                downloadRegionDialog();
                            }
                        });
// List offline regions
                        listButton =  findViewById(R.id.list_button);
                        listButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                downloadedRegionList();
                            }
                        });



                    }

                });
    }
    public void converter() {
        findViewById(R.id.converter).setOnClickListener(new View.OnClickListener() {

            private String m_Text = "";

            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LocationComponentActivity.this);
                builder.setTitle("Meter Square to Ropani");
                Context context = v.getContext();
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText input = new EditText(LocationComponentActivity.this);
                input.setHint("Enter Area in Meter Square");
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
//                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                layout.addView(input);
                final TextView output= new TextView(LocationComponentActivity.this);
                output.setText("0");
                final Button newbutton= new Button(LocationComponentActivity.this);
                newbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        m_Text=input.getText().toString();

                        if(input.getText()!=null && !TextUtils.isEmpty(m_Text)){
                            Double f1 = Double.parseDouble(m_Text);
                            Double ropani=f1*0.0019656406022723;
                            output.setText(ropani.toString()+" Ropani");
                        }
                        else{
                            Toast.makeText(LocationComponentActivity.this, "Enter Area First", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                newbutton.setText("Clickme");
                layout.addView(newbutton);
                layout.addView(output);
                builder.setView(layout);
// Set up the buttons
//                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        m_Text = input.getText().toString();
//                    }
//                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();

            }
        });
    }
    private void showAlertDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LocationComponentActivity.this);
        alertDialog.setTitle("Select Layers");
                String[] items = {"Point block2","Point block1","Seratar Block 1","Seratar Block 2","Seratar Block 3","POI Block 3"};
                boolean[] checkedItems = {false, false, false, false, false,false};

                alertDialog.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                        switch (which) {
                            case 0:
                                if(isChecked)
                                    Toast.makeText(LocationComponentActivity.this, "Clicked on Design Guthhim", Toast.LENGTH_LONG).show();
                                    String file="poiblock2.geojson";
                                    jsonbackground(file,"point");
                                break;
                            case 1:
                                if(isChecked)
                                    Toast.makeText(LocationComponentActivity.this, "Clicked on Point Guthhim", Toast.LENGTH_LONG).show();
                                String filenew="poiblock1.geojson";
                                jsonbackground(filenew,"point");

                                break;
                            case 2:
                                if(isChecked)
                                    Toast.makeText(LocationComponentActivity.this, "Clicked on Data Structures", Toast.LENGTH_LONG).show();
                                    String serafile="block1seratar.geojson";
                                    jsonbackground(serafile,"line");
                                break;
                            case 3:
                                if(isChecked)
                                    Toast.makeText(LocationComponentActivity.this, "Clicked on HTML", Toast.LENGTH_LONG).show();
                                String sera2file="block2seratar.geojson";
                                jsonbackground(sera2file,"line");
                                break;
                            case 4:
                                if(isChecked)
                                    Toast.makeText(LocationComponentActivity.this, "Clicked on CSS", Toast.LENGTH_LONG).show();
                                String sera3file="block3seratar.geojson";
                                jsonbackground(sera3file,"line");
                                break;
                            case 5:
                                if(isChecked)
                                    Toast.makeText(LocationComponentActivity.this, "Clicked on 3", Toast.LENGTH_LONG).show();
                                String sera4file="poiblock3.geojson";
                                jsonbackground(sera4file,"point");
                                break;
                        }
                    }
                });
                AlertDialog alert = alertDialog.create();
                alert.setCanceledOnTouchOutside(false);

                alert.show();
    }

    public void createFile() {
        if (new CheckForSDCard().isSDCardPresent()) {

            apkStorage = new File(Environment.getExternalStorageDirectory() + "/" + "Chaklabandi");
        } else
            Toast.makeText(LocationComponentActivity.this, "Oops!! There is no SD Card.", Toast.LENGTH_SHORT).show();

        //If File is not present create directory
        if (!apkStorage.exists()) {
            apkStorage.mkdir();
            Log.e(TAG, "Directory Created.");
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        new SweetAlertDialog(LocationComponentActivity.this, SweetAlertDialog.WARNING_TYPE)
                                            .setTitleText(marker.getTitle())
                                            .setContentText(marker.getSnippet())
                                            .setConfirmText("Mark!")
                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                @Override
                                                public void onClick(SweetAlertDialog sDialog) {
                                                    IconFactory iconFactory = IconFactory.getInstance(LocationComponentActivity.this);
                                                    Icon icon = iconFactory.fromResource(R.drawable.blue_pin_marker);
                                                    String sentence=marker.getTitle();
                                                    String search ="Done";
                                                    if ( sentence.toLowerCase().indexOf(search.toLowerCase()) != -1 ) {

                                                        Toast.makeText(LocationComponentActivity.this, "You Already Marked this point", Toast.LENGTH_LONG).show();
                                                    } else {

                                                        marker.setTitle(marker.getTitle()+" Done");
                                                        marker.setIcon(icon);
                                                        iconOffset(new Float[] {0f, -8f});
                                                        count=count+1;
                                                        poicount.setText("Completed: "+Integer.toString(count)+" /"+Integer.toString(totalpoi));
                                                        Toast.makeText(LocationComponentActivity.this, "Point marked as Done"+Integer.toString(count), Toast.LENGTH_LONG).show();
                                                    }

                                                    sDialog.dismissWithAnimation();

                                                }
                                            })
                                            .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                                                @Override
                                                public void onClick(SweetAlertDialog sDialog) {
                                                    sDialog.dismissWithAnimation();
                                                }
                                            })
                                            .show();

        return false;
    }

    public class CheckForSDCard {
        //Check If SD Card is present or not method
        public boolean isSDCardPresent() {
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                return true;
            }
            return false;
        }
    }

    public void jsonbackground(String fileename, String filetype){
        mapboxMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                String data = getAssetJsonData(getApplicationContext(),fileename);
                FeatureCollection featureCollection = FeatureCollection.fromJson(data);
                if(filetype=="line"){
                    if (style.getSourceAs("line-source")!=null){
                        Toast.makeText(LocationComponentActivity.this, "line source is not null", Toast.LENGTH_LONG).show();
                        style.removeLayer("line-layer");
                        style.removeSource("line-source");
                    }
                    else{
                        GeoJsonSource geoJsonSource = new GeoJsonSource("line-source", featureCollection);
                        style.addSource(geoJsonSource);
                        LineLayer lineLayer = new LineLayer("line-layer", "line-source");
                        lineLayer.setProperties(//                                PropertyFactory.lineDasharray(new Float[]{0.01f, 2f}),
                                PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                PropertyFactory.lineWidth(2f),
                                PropertyFactory.lineOpacity(.7f),
                                PropertyFactory.lineColor(Color.parseColor("#e55e5e"))
                        );
                        style.addLayer(lineLayer);
                    }
                }
                else if(filetype=="point"){
                    if (style.getSourceAs("point-source")!=null){
                        style.removeLayer("layer-point");
                        style.removeSource("point-source");
                        mapboxMap.clear();

                    }
                    else{
                        GeoJsonSource geoJsonSource = new GeoJsonSource("point-source", featureCollection);
                        totalpoi=featureCollection.features().size();
                        style.addSource(geoJsonSource);
                        SymbolLayer symbolLayer = new SymbolLayer("layer-point", "point-source")
                                .withProperties(PropertyFactory.textField(Expression.get("sn")));
                        style.addLayer(symbolLayer);


//                        PropertyFactory.iconOffset(new Float[] {0f, -8f})
                        mapboxMap.setOnMarkerClickListener(LocationComponentActivity.this);
                        manualaddpoint(fileename);
//                        style.addImage(("marker_icon_red"), BitmapFactory.decodeResource(
//                                getResources(), R.drawable.red_marker));
//                        style.addImage(("marker_icon_blue"), BitmapFactory.decodeResource(
//                                getResources(), R.drawable.blue_marker));
//                        SymbolLayer symbolLayer = new SymbolLayer("layer-id", "point-source")
//                                .withProperties(PropertyFactory.iconImage("marker_icon_red"),PropertyFactory.textField(Expression.get("sn")),PropertyFactory.iconOffset(new Float[] {0f, -8f}));
//                        style.addLayer(symbolLayer);
//                        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
//                            @Override
//                            public boolean onMapClick(@NonNull LatLng point) {
//                                PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
//                                List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, "layer-id");
//                                if (!features.isEmpty()) {
//                                    Feature selectedFeature = features.get(0);
////                                    String title = selectedFeature.getStringProperty("sn");
////                                    Toast.makeText(LocationComponentActivity.this, "You selected " + title, Toast.LENGTH_SHORT).show();
//                                    new SweetAlertDialog(LocationComponentActivity.this, SweetAlertDialog.WARNING_TYPE)
//                                            .setTitleText("Point :"+selectedFeature.properties().get("status"))
//                                            .setContentText("X : " + selectedFeature.getStringProperty("X") + ", Y: " + selectedFeature.getStringProperty("Y") + " , Z: " + selectedFeature.getStringProperty("Z"))
//                                            .setConfirmText("Mark!")
//                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
//                                                @Override
//                                                public void onClick(SweetAlertDialog sDialog) {
//                                                    if(selectedFeature.getStringProperty("status")==null){
//                                                    }
//                                                }
//                                            })
//                                            .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
//                                                @Override
//                                                public void onClick(SweetAlertDialog sDialog) {
//                                                    sDialog.dismissWithAnimation();
//                                                }
//                                            })
//                                            .show();
//                                }
//                                return false;
//                            }
//                        });
                    }
                }
            }
        });
    }
    private void initSearchFab() {
        findViewById(R.id.fab_location_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .addInjectedFeature(home)
                                .addInjectedFeature(work)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(LocationComponentActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(symbolIconId),
                iconOffset(new Float[]{0f, -8f})
        ));
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

// Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

// Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
// Then retrieve and update the source designated for showing a selected location's symbol layer icon

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[] {Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }
                }
            }
        }
    }

    public  void designbuttonclick(View view){
        new LoadGeoJson(this).execute();
    }
    public  void manualaddpoint(String inputfile) {
        String data = getAssetJsonData(getApplicationContext(),inputfile);
        FeatureCollection featureCollection = FeatureCollection.fromJson(data);
        //                    Source source = new GeoJsonSource("my.data.source", featureCollection);
        for (Feature feature : featureCollection.features()) {
            mapboxMap.addMarker(new MarkerOptions()
//                    .position(new LatLng(feature.geometry())
                    .position(new LatLng(feature.getProperty("lat").getAsFloat(),
                            feature.getProperty("lon").getAsFloat()))
                    .snippet("X : " + feature.getStringProperty("X") + " , Y: " + feature.getStringProperty("Y") + " , Z: " + feature.getStringProperty("Z"))
                    .title(feature.getStringProperty("sn")));

            mapboxMap.setOnMarkerClickListener(LocationComponentActivity.this);
//            mapboxMap.setInfoWindowAdapter(new MapboxMap.InfoWindowAdapter() {
//                @Nullable
//                @Override
//                public View getInfoWindow(@NonNull Marker marker) {
//                    LinearLayout parent = new LinearLayout(LocationComponentActivity.this);
//                    parent.setLayoutParams(new LinearLayout.LayoutParams(
//                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//                    parent.setOrientation(LinearLayout.VERTICAL);
//
//                    // Depending on the marker title, the correct image source is used. If you
//                    // have many markers using different images, extending Marker and
//                    // baseMarkerOptions, adding additional options such as the image, might be
//                    // a better choice.
//                    Button Done = new Button(LocationComponentActivity.this);
//                    Done.setText("Mark");
//                    parent.addView(Done);
//                    return parent;
//
//                    // return the view which includes the button
//
//                }
//            });
//            mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
//                            @Override
//                            public boolean onMapClick(@NonNull LatLng point) {
//                                PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
//                                List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, "layer-id");
//                                if (!features.isEmpty()) {
//                                    Feature selectedFeature = features.get(0);
////                                    String title = selectedFeature.getStringProperty("sn");
////                                    Toast.makeText(LocationComponentActivity.this, "You selected " + title, Toast.LENGTH_SHORT).show();
//                                    new SweetAlertDialog(LocationComponentActivity.this, SweetAlertDialog.WARNING_TYPE)
//                                            .setTitleText("Point :"+selectedFeature.properties().get("status"))
//                                            .setContentText("X : " + selectedFeature.getStringProperty("X") + ", Y: " + selectedFeature.getStringProperty("Y") + " , Z: " + selectedFeature.getStringProperty("Z"))
//                                            .setConfirmText("Mark!")
//                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
//                                                @Override
//                                                public void onClick(SweetAlertDialog sDialog) {
//
//
//                                                }
//                                            })
//                                            .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
//                                                @Override
//                                                public void onClick(SweetAlertDialog sDialog) {
//                                                    sDialog.dismissWithAnimation();
//                                                }
//                                            })
//                                            .show();
//                                }
//                                return false;
//                            }
//                        });
        }
    }
    private String getAssetJsonData(Context context,String filename) {
        String json;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        Log.e("data", json);
        return json;
    }


    private void drawLines(@NonNull FeatureCollection featureCollection) {
        if (mapboxMap != null) {
            mapboxMap.getStyle(style -> {
                if (featureCollection.features() != null) {
                    if (featureCollection.features().size() > 0) {
                        style.addSource(new GeoJsonSource("line-source", featureCollection));

// The layer properties for our line. This is where we make the line dotted, set the
// color, etc.
                        style.addLayer(new LineLayer("linelayer", "line-source")
                                .withProperties(PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                        PropertyFactory.lineOpacity(.7f),
                                        PropertyFactory.lineWidth(2f),
                                        PropertyFactory.lineColor(Color.parseColor("#3bb2d0"))));
                    }
                }
            });
        }
    }

    private static class LoadGeoJson extends AsyncTask<Void, Void, FeatureCollection> {

        private WeakReference<LocationComponentActivity> weakReference;

        LoadGeoJson(LocationComponentActivity activity) {
            this.weakReference = new WeakReference<>(activity);
        }
        @Override
        protected FeatureCollection doInBackground(Void... voids) {
            try {
                LocationComponentActivity activity = weakReference.get();
                if (activity != null) {
                    InputStream inputStream = activity.getAssets().open("example.geojson");
                    return FeatureCollection.fromJson(convertStreamToString(inputStream));
                }
            } catch (Exception exception) {
//                Timber.e("Exception Loading GeoJSON: %s" , exception.toString());
            }
            return null;
        }

        static String convertStreamToString(InputStream is) {
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }

        @Override
        protected void onPostExecute(@Nullable FeatureCollection featureCollection) {
            super.onPostExecute(featureCollection);
            LocationComponentActivity activity = weakReference.get();
            if (activity != null && featureCollection != null) {
                activity.drawLines(featureCollection);

            }
        }
    }


    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
// Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());
// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);
// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
            initLocationEngine();


        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    @SuppressLint("MissingPermission")
    private void initLocationEngine(){
        locationEngine= LocationEngineProvider.getBestLocationEngine(this);
        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILISECONDS).setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY).setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();
        locationEngine.requestLocationUpdates(request,callback,getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
//                    enableLocationComponent(style);

                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }
    private static class LocationComponentActivityLocationCallback implements LocationEngineCallback<LocationEngineResult>{
        private final WeakReference<LocationComponentActivity> activityWeakReference;
        LocationComponentActivityLocationCallback(LocationComponentActivity activity){
            this.activityWeakReference= new WeakReference<>(activity);
        }
        @Override
        public void onSuccess(LocationEngineResult result){
            LocationComponentActivity activity= activityWeakReference.get();
            if(activity!=null){
                Location location=result.getLastLocation();
                if (location == null){
                    return;
                }

                if(activity.mapboxMap !=null && result.getLastLocation()!= null){
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("LocationChangeActivity",exception.getLocalizedMessage());
            LocationComponentActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }


        }
    }

    /**
     * Listen to and use a tap on the LocationComponent
     */


    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationEngine!= null){
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    private void downloadRegionDialog() {
// Set up download interaction. Display a dialog
// when the user clicks download button and require
// a user-provided region name
        AlertDialog.Builder builder = new AlertDialog.Builder(LocationComponentActivity.this);

        final EditText regionNameEdit = new EditText(LocationComponentActivity.this);
        regionNameEdit.setHint(getString(R.string.set_region_name_hint));

// Build the dialog box
        builder.setTitle(getString(R.string.dialog_title))
                .setView(regionNameEdit)
                .setMessage(getString(R.string.dialog_message))
                .setPositiveButton(getString(R.string.dialog_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String regionName = regionNameEdit.getText().toString();
// Require a region name to begin the download.
// If the user-provided string is empty, display
// a toast message and do not begin download.
                        if (regionName.length() == 0) {
                            Toast.makeText(LocationComponentActivity.this, getString(R.string.dialog_toast), Toast.LENGTH_SHORT).show();
                        } else {
// Begin download process
                            downloadRegion(regionName);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.dialog_negative_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

// Display the dialog
        builder.show();
    }

    private void downloadRegion(final String regionName) {
// Define offline region parameters, including bounds,
// min/max zoom, and metadata

// Start the progressBar
        startProgress();

// Create offline definition using the current
// style and boundaries of visible map area

        map.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                String styleUrl = style.getUri();
                LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
                double minZoom = map.getCameraPosition().zoom;
                double maxZoom = map.getMaxZoomLevel();
                float pixelRatio = LocationComponentActivity.this.getResources().getDisplayMetrics().density;
                OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                        styleUrl, bounds, minZoom, maxZoom, pixelRatio);

// Build a JSONObject using the user-defined offline region title,
// convert it into string, and use it to create a metadata variable.
// The metadata variable will later be passed to createOfflineRegion()
                byte[] metadata;
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(JSON_FIELD_REGION_NAME, regionName);
                    String json = jsonObject.toString();
                    metadata = json.getBytes(JSON_CHARSET);
                } catch (Exception exception) {
                    Timber.e("Failed to encode metadata: %s", exception.getMessage());
                    metadata = null;
                }

// Create the offline region and launch the download
                offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {
                    @Override
                    public void onCreate(OfflineRegion offlineRegion) {
                        Timber.d( "Offline region created: %s" , regionName);
                        LocationComponentActivity.this.offlineRegion = offlineRegion;
                        launchDownload();
                    }

                    @Override
                    public void onError(String error) {
                        Timber.e( "Error: %s" , error);
                    }
                });
            }
        });
    }

    private void launchDownload() {
// Set up an observer to handle download progress and
// notify the user when the region is finished downloading
        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
            @Override
            public void onStatusChanged(OfflineRegionStatus status) {
// Compute a percentage
                double percentage = status.getRequiredResourceCount() >= 0
                        ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                        0.0;

                if (status.isComplete()) {
// Download complete
                    endProgress(getString(R.string.end_progress_success));
                    return;
                } else if (status.isRequiredResourceCountPrecise()) {
// Switch to determinate state
                    setPercentage((int) Math.round(percentage));
                }

// Log what is being currently downloaded
                Timber.d("%s/%s resources; %s bytes downloaded.",
                        String.valueOf(status.getCompletedResourceCount()),
                        String.valueOf(status.getRequiredResourceCount()),
                        String.valueOf(status.getCompletedResourceSize()));
            }

            @Override
            public void onError(OfflineRegionError error) {
                Timber.e("onError reason: %s", error.getReason());
                Timber.e("onError message: %s", error.getMessage());
            }

            @Override
            public void mapboxTileCountLimitExceeded(long limit) {
                Timber.e("Mapbox tile count limit exceeded: %s", limit);
            }
        });

// Change the region state
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }

    private void downloadedRegionList() {
// Build a region list when the user clicks the list button

// Reset the region selected int to 0
        regionSelected = 0;

// Query the DB asynchronously
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override
            public void onList(final OfflineRegion[] offlineRegions) {
// Check result. If no regions have been
// downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.length == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_no_regions_yet), Toast.LENGTH_SHORT).show();
                    return;
                }

// Add all of the region names to a list
                ArrayList<String> offlineRegionsNames = new ArrayList<>();
                for (OfflineRegion offlineRegion : offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion));
                }
                final CharSequence[] items = offlineRegionsNames.toArray(new CharSequence[offlineRegionsNames.size()]);

// Build a dialog containing the list of regions
                AlertDialog dialog = new AlertDialog.Builder(LocationComponentActivity.this)
                        .setTitle(getString(R.string.navigate_title))
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
// Track which region the user selects
                                regionSelected = which;
                            }
                        })
                        .setPositiveButton(getString(R.string.navigate_positive_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                Toast.makeText(LocationComponentActivity.this, items[regionSelected], Toast.LENGTH_LONG).show();

// Get the region bounds and zoom
                                LatLngBounds bounds = (offlineRegions[regionSelected].getDefinition()).getBounds();
                                double regionZoom = (offlineRegions[regionSelected].getDefinition()).getMinZoom();

// Create new camera position
                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(bounds.getCenter())
                                        .zoom(regionZoom)
                                        .build();

// Move camera to new position
                                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                            }
                        })
                        .setNeutralButton(getString(R.string.navigate_neutral_button_title), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
// Make progressBar indeterminate and
// set it to visible to signal that
// the deletion process has begun
                                progressBar.setIndeterminate(true);
                                progressBar.setVisibility(View.VISIBLE);

// Begin the deletion process
                                offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                                    @Override
                                    public void onDelete() {
// Once the region is deleted, remove the
// progressBar and display a toast
                                        progressBar.setVisibility(View.INVISIBLE);
                                        progressBar.setIndeterminate(false);
                                        Toast.makeText(getApplicationContext(), getString(R.string.toast_region_deleted),
                                                Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        progressBar.setIndeterminate(false);
                                        Timber.e( "Error: %s", error);
                                    }
                                });
                            }
                        })
                        .setNegativeButton(getString(R.string.navigate_negative_button_title), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
// When the user cancels, don't do anything.
// The dialog will automatically close
                            }
                        }).create();
                dialog.show();

            }

            @Override
            public void onError(String error) {
                Timber.e( "Error: %s", error);
            }
        });
    }

    private String getRegionName(OfflineRegion offlineRegion) {
// Get the region name from the offline region metadata
        String regionName;

        try {
            byte[] metadata = offlineRegion.getMetadata();
            String json = new String(metadata, JSON_CHARSET);
            JSONObject jsonObject = new JSONObject(json);
            regionName = jsonObject.getString(JSON_FIELD_REGION_NAME);
        } catch (Exception exception) {
            Timber.e("Failed to decode metadata: %s", exception.getMessage());
            regionName = String.format(getString(R.string.region_name), offlineRegion.getID());
        }
        return regionName;
    }

    // Progress bar methods
    private void startProgress() {
// Disable buttons
        downloadButton.setEnabled(false);
        listButton.setEnabled(false);

// Start and show the progress bar
        isEndNotified = false;
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void setPercentage(final int percentage) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(percentage);
    }

    private void endProgress(final String message) {
// Don't notify more than once
        if (isEndNotified) {
            return;
        }

// Enable buttons
        downloadButton.setEnabled(true);
        listButton.setEnabled(true);

// Stop and hide the progress bar
        isEndNotified = true;
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);

// Show a toast
        Toast.makeText(LocationComponentActivity.this, message, Toast.LENGTH_LONG).show();
    }

}
