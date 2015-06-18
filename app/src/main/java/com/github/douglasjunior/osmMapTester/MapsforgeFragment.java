package com.github.douglasjunior.osmMapTester;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.douglasjunior.osmMapTester.mapsforge.RotateMarker;
import com.github.douglasjunior.osmMapTester.mapsforge.RotateView;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;

/**
 * Example of use of Mapsforge library with offline map file, and rotation.
 * Repository https://github.com/mapsforge/mapsforge
 *
 * @author douglas
 */
public class MapsforgeFragment extends Fragment implements LocationListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener, SensorEventListener {

    private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;

    private static final String MAPFILE = "brazil.map"; // more maps in http://download.mapsforge.org/
    private static final byte ZOOM_LEVEL_MAX = 19;
    private static final byte ZOOM_LEVEL_MIN = 14;

    private RotateView rotateView;
    private MapView mapView;
    private TileCache tileCache;
    private Location lastLocation;
    private LocationManager locationManager;
    private MapViewPosition mapViewPosition;
    private RotateMarker marker;
    private Circle circle;
    private Polyline polyline;

    private Button btnReset;
    private Button btnTrack;
    private TextView tvCompass;
    private boolean trackMarker = true;
    private SeekBar sbRotation;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    public static MapsforgeFragment newInstance() {
        MapsforgeFragment fragment = new MapsforgeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MapsforgeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View root = inflater.inflate(R.layout.fragment_mapsforge, container, false);

        btnReset = (Button) root.findViewById(R.id.btnReset);
        btnReset.setOnClickListener(this);
        btnTrack = (Button) root.findViewById(R.id.btnTrack);
        btnTrack.setOnClickListener(this);

        rotateView = (RotateView) root.findViewById(R.id.rotateView);

        sbRotation = (SeekBar) root.findViewById(R.id.sbRotation);
        sbRotation.setOnSeekBarChangeListener(this);

        tvCompass = (TextView) root.findViewById(R.id.tvCompass);

        mapView = (MapView) root.findViewById(R.id.mapView);
        mapView.setClickable(true);
        mapView.getModel().frameBufferModel.setOverdrawFactor(1.0d);
        mapView.getMapScaleBar().setVisible(false);
        mapView.setBuiltInZoomControls(false);
        mapViewPosition = mapView.getModel().mapViewPosition;
        mapViewPosition.setZoomLevelMax(ZOOM_LEVEL_MIN);
        mapViewPosition.setZoomLevelMax(ZOOM_LEVEL_MAX);
        mapViewPosition.setCenter(new LatLong(-24.1261, -52.3643));
        mapViewPosition.setZoomLevel(ZOOM_LEVEL_MIN);

        // create a tile cache of suitable size
        boolean threaded = true;
        int queueSize = 4;
        boolean persistent = true;

        Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        final int hypot;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            android.graphics.Point point = new android.graphics.Point();
            display.getSize(point);
            hypot = (int) Math.hypot(point.x, point.y);
        } else {
            hypot = (int) Math.hypot(display.getWidth(), display.getHeight());
        }

        tileCache = AndroidUtil.createTileCache(getActivity(), this.getClass().getSimpleName(),
                this.mapView.getModel().displayModel.getTileSize(), hypot, hypot,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor(),
                threaded, queueSize, persistent);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        MapDataStore dataStore = getMapFile();

        if (dataStore != null) {
            // tile renderer layer using internal render theme
            TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(tileCache,
                    mapViewPosition, dataStore, InternalRenderTheme.OSMARENDER, false, true);
            mapView.getLayerManager().getLayers().add(tileRendererLayer);

            // polyline
            polyline = new Polyline(getPaint(Color.RED, 5, Style.STROKE), GRAPHIC_FACTORY);
            mapView.getLayerManager().getLayers().add(polyline);

            // accuracy circle
            circle = new Circle(mapViewPosition.getCenter(), getScaledAccuracy(), getDefaultCircleFill(), getDefaultCircleStroke());
            mapView.getLayerManager().getLayers().add(circle);

            // marker
            Drawable drawable = getResources().getDrawable(R.drawable.ic_arrow);
            Bitmap bitmap = AndroidGraphicFactory.convertToBitmap(drawable);
            marker = new RotateMarker(mapViewPosition.getCenter(), bitmap, 0, 0);
            mapView.getLayerManager().getLayers().add(marker);

            // start gps
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 3, this);

            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    protected File getMapFileDirectory() {
        return Environment.getExternalStorageDirectory();
    }

    protected MapDataStore getMapFile() {
        File mapFile = new File(getMapFileDirectory(), MAPFILE);
        if (!mapFile.exists()) {
            new AlertDialog.Builder(getActivity()).setMessage("Map file " + MAPFILE + " not found in SDCARD").setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).show();
            return null;
        }
        return new MapFile(mapFile);
    }

    private void updateMarker(LatLong point) {
        polyline.getLatLongs().add(point);
        polyline.requestRedraw();
        marker.setLatLong(point);
        circle.setLatLong(point);
        circle.setRadius(getScaledAccuracy());
        if (trackMarker) {
            mapViewPosition.setZoomLevel(mapViewPosition.getZoomLevelMax());
            mapViewPosition.animateTo(point);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationManager != null)
            locationManager.removeUpdates(this);
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, accelerometer);
            mSensorManager.unregisterListener(this, magnetometer);
        }
        for (Layer layer : mapView.getLayerManager().getLayers()) {
            mapView.getLayerManager().getLayers().remove(layer);
            layer.onDestroy();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        tileCache.destroy();
        mapViewPosition.destroy();
        mapView.destroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.title_fragment_mapsforge);
        activity.getActionBar().setTitle(R.string.title_fragment_mapsforge);
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(getActivity(), location.getProvider() + "\n" +
                location.getSpeed() + " m/s\n" +
                (location.getSpeed() * 3.6) + " km/h\n" +
                location.getAccuracy() + " accuracy/m"
                , Toast.LENGTH_SHORT).show();

        if (LocationUtil.isBetterLocation(location, lastLocation)) {
            lastLocation = location;
            LatLong point = new LatLong(location.getLatitude(), location.getLongitude());
            updateMarker(point);

        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    public float getScaledAccuracy() {
        float accuracy = lastLocation != null ? lastLocation.getAccuracy() : 100;
        return accuracy * this.mapView.getModel().displayModel.getScaleFactor();
    }

    private static Paint getDefaultCircleFill() {
        return getPaint(GRAPHIC_FACTORY.createColor(48, 0, 0, 255), 0, Style.FILL);
    }

    private static Paint getDefaultCircleStroke() {
        return getPaint(GRAPHIC_FACTORY.createColor(160, 0, 0, 255), 2, Style.STROKE);
    }

    private static Paint getPaint(int color, int strokeWidth, Style style) {
        Paint paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(style);
        return paint;
    }

    @Override
    public void onClick(View view) {
        if (view == btnReset) {
            if (polyline != null) {
                polyline.getLatLongs().clear();
                polyline.requestRedraw();
            }
        } else if (view == btnTrack) {
            trackMarker = true;
            if (lastLocation != null) {
                LatLong point = new LatLong(lastLocation.getLatitude(), lastLocation.getLongitude());
                mapViewPosition.setZoomLevel(mapViewPosition.getZoomLevelMax());
                mapViewPosition.animateTo(point);
            }
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, int i, boolean b) {
        rotateMap(seekBar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    float[] mGravity;
    float[] mGeomagnetic;

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, I);
                float orientation[] = new float[3];
                SensorManager.getOrientation(I, orientation);
                // convert azimuth to heading rotation
                final int heading = (int) (360 - (-orientation[0] * 360f / (2f * 3.14159f)));
                tvCompass.setText("azimuth: " + orientation[0]);
                tvCompass.append("\npitch: " + orientation[1]);
                tvCompass.append("\nroll: " + orientation[2]);
                tvCompass.append("\nheading: " + heading);
                // if difference > 2
                if (Math.abs(rotateView.getHeading() - heading) > 2f) {
                    rotateMap(heading);
                }
            }
        }
    }

    private void rotateMap(final int heading) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rotateView.setHeading(heading);
                rotateView.invalidate();
                marker.setHeading(rotateView.getHeading());
                marker.requestRedraw();
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
