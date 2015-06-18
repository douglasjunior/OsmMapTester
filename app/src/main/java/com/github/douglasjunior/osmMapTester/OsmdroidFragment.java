package com.github.douglasjunior.osmMapTester;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polygon;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;


/**
 * Example of use of Osmdroid library with online maps.
 * Repository https://github.com/osmdroid/osmdroid
 *
 * @author douglas
 */
public class OsmdroidFragment extends Fragment implements LocationListener, View.OnClickListener, MapListener {

    private Button btnReset;
    private Button btnTrack;
    private MapView mapView;
    private MapController mc;
    private Polyline polyline;
    private Polygon circle;

    private LocationManager locationManager;

    private boolean trackMarker = true;
    private GeoPoint lastPoint = new GeoPoint(-24.1261, -52.3643);
    private int lastZoomLevel = 12;
    private float lastSpeed = 0;
    private float lastAccuracy = 0;
    private List<GeoPoint> points;
    private Location lastLocation;


    public static OsmdroidFragment newInstance() {
        OsmdroidFragment fragment = new OsmdroidFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public OsmdroidFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_osmdroid, container, false);

        btnTrack = (Button) root.findViewById(R.id.btnTrack);
        btnTrack.setOnClickListener(this);

        btnReset = (Button) root.findViewById(R.id.btnReset);
        btnReset.setOnClickListener(this);

        mapView = (MapView) root.findViewById(R.id.mapView);

        MapTileProviderBase provider = new MapTileProviderBasic(getActivity());
        mapView.setTileSource(provider.getTileSource());
        mapView.setMapListener(this);
        mapView.setTilesScaledToDpi(true);
        mapView.setMultiTouchControls(true);
        mapView.setMaxZoomLevel(null);

        mc = (MapController) mapView.getController();

        mc.setCenter(lastPoint);

        points = new ArrayList<>();
        polyline = new Polyline(getActivity());
        polyline.setColor(Color.RED);
        polyline.setWidth(5);

        circle = new Polygon(getActivity());
        circle.setStrokeColor(Color.BLUE);
        circle.setFillColor(Color.argb(100, 0, 0, 255));
        circle.setStrokeWidth(2);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 3, this);

        return root;
    }

    private void addMarker(GeoPoint point, int zoomLevel) {
        lastPoint = point;

        points.add(point);
        polyline.setPoints(points);

        circle.setPoints(Polygon.pointsAsCircle(point, lastAccuracy));

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mc.setZoom(zoomLevel);

        mapView.getOverlays().clear();
        mapView.getOverlays().add(new CustomOverlay(getActivity()));
        mapView.getOverlays().add(polyline);
        mapView.getOverlays().add(circle);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.title_fragment_osmdroid);
        activity.getActionBar().setTitle(R.string.title_fragment_osmdroid);
    }

    @Override
    public void onDetach() {
        super.onDetach();

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
            lastSpeed = location.getSpeed();
            lastAccuracy = location.getAccuracy();
            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());

            int zoomLevel = lastZoomLevel;
            if (trackMarker) {
                mc.animateTo(point);
                zoomLevel = mapView.getMaxZoomLevel();
            }
            addMarker(point, zoomLevel);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnTrack) {
            trackMarker = true;

            if (lastPoint != null) {
                addMarker(lastPoint, mapView.getMaxZoomLevel());
                mc.animateTo(lastPoint);
            }

        } else if (v == btnReset) {
            points.clear();
            polyline.setPoints(points);
            mapView.invalidate();
        }
    }

    @Override
    public boolean onScroll(ScrollEvent event) {
        return false;
    }

    @Override
    public boolean onZoom(ZoomEvent event) {
        lastZoomLevel = event.getZoomLevel();
        return true;
    }


    /**
     * Customized overlay to capture events
     */
    public class CustomOverlay extends Overlay {

        public CustomOverlay(Context ctx) {
            super(ctx);
        }

        @Override
        protected void draw(Canvas c, MapView osmv, boolean shadow) {

        }

        @Override
        public boolean onFling(MotionEvent pEvent1, MotionEvent pEvent2, float pVelocityX, float pVelocityY, MapView pMapView) {
            trackMarker = false;
            return super.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY, pMapView);
        }

    }
}
