package fr.free.nrw.commons.nearby;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.services.android.telemetry.MapboxTelemetry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.utils.UriDeserializer;

public class NearbyMapFragment extends android.support.v4.app.Fragment
        implements LoaderManager.LoaderCallbacks<Bundle> {
    private static final Type LIST_TYPE = new TypeToken<List<Place>>() {
    }.getType();
    private static final Type CUR_LAT_LNG_TYPE = new TypeToken<fr.free.nrw.commons.location.LatLng>() {
    }.getType();
    private static final int PLACES_LOADER_ID = 0;

    private MapView mapView;
    private List<NearbyBaseMarker> baseMarkerOptions;
    private fr.free.nrw.commons.location.LatLng curLatLng;
    private LocationServiceManager locationManager;
    private MapboxMap mapboxMap;

    public NearbyMapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setupMapView(savedInstanceState);

        setHasOptionsMenu(true);

        return mapView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // force loader to reload data
                getLoaderManager().restartLoader(PLACES_LOADER_ID, null, this);
                return true;
            default:
                return false;
        }
    }

    private void setupMapView(Bundle savedInstanceState) {
        MapboxMapOptions options = new MapboxMapOptions()
                .styleUrl(Style.OUTDOORS);

        // create map
        mapView = new MapView(getActivity(), options);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mMapboxMap -> {
            mapboxMap = mMapboxMap;

            mapboxMap.setOnMarkerClickListener(marker -> {
                if (marker instanceof NearbyMarker) {
                    NearbyMarker nearbyMarker = (NearbyMarker) marker;
                    Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
                    NearbyInfoDialog.showYourself(getActivity(), place);
                }
                return false;
            });

            getLoaderManager().initLoader(PLACES_LOADER_ID, null, this);
        });
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("theme",false)) {
            mapView.setStyleUrl(getResources().getString(R.string.map_theme_dark));
        } else {
            mapView.setStyleUrl(getResources().getString(R.string.map_theme_light));
        }
    }

    /**
     * Adds a marker for the user's current position. Adds a
     * circle which uses the accuracy * 2, to draw a circle
     * which represents the user's position with an accuracy
     * of 95%.
     */
    private void addCurrentLocationMarker(MapboxMap mapboxMap) {
        MarkerOptions currentLocationMarker = new MarkerOptions()
                .position(new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude()));
        mapboxMap.addMarker(currentLocationMarker);

        List<LatLng> circle = createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                curLatLng.getAccuracy() * 2, 100);

        mapboxMap.addPolygon(
                new PolygonOptions()
                        .addAll(circle)
                        .strokeColor(Color.parseColor("#55000000"))
                        .fillColor(Color.parseColor("#11000000"))
        );
    }

    /**
     * Creates a series of points that create a circle on the map.
     * Takes the center latitude, center longitude of the circle,
     * the radius in meter and the number of nodes of the circle.
     *
     * @return List List of LatLng points of the circle.
     */
    private List<LatLng> createCircleArray(
            double centerLat, double centerLong, float radius, int nodes) {
        List<LatLng> circle = new ArrayList<>();
        float radiusKilometer = radius / 1000;
        double radiusLong = radiusKilometer
                / (111.320 * Math.cos(centerLat * Math.PI / 180));
        double radiusLat = radiusKilometer / 110.574;

        for (int i = 0; i < nodes; i++) {
            double theta = ((double) i / (double) nodes) * (2 * Math.PI);
            double nodeLongitude = centerLong + radiusLong * Math.cos(theta);
            double nodeLatitude = centerLat + radiusLat * Math.sin(theta);
            circle.add(new LatLng(nodeLatitude, nodeLongitude));
        }

        return circle;
    }

    private void placeMarkers() {
        if (mapboxMap != null) {
            mapboxMap.clear();

            mapboxMap.addMarkers(baseMarkerOptions);
            addCurrentLocationMarker(mapboxMap);

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude()))
                    .zoom(11)
                    .build()));
        }
    }

    @Override
    public void onStart() {
        if (mapView != null) {
            mapView.onStart();
        }
        super.onStart();
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mapView != null) {
            mapView.onResume();
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (locationManager != null) {
            locationManager.unregisterLocationManager();
        }
        super.onDestroyView();
    }

    @Override
    public Loader<Bundle> onCreateLoader(int i, Bundle bundle) {
        if (locationManager == null) {
            locationManager = new LocationServiceManager(getContext());
            locationManager.registerLocationManager();
        }
        fr.free.nrw.commons.location.LatLng curLatLang = locationManager.getLatestLocation();

        return new NearbyPlacesLoader(getContext(), curLatLang);
    }

    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle bundle) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriDeserializer())
                .create();

        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList");
            String gsonLatLng = bundle.getString("CurLatLng");
            List<Place> placeList = gson.fromJson(gsonPlaceList, LIST_TYPE);
            curLatLng = gson.fromJson(gsonLatLng, CUR_LAT_LNG_TYPE);
            baseMarkerOptions = NearbyController
                    .loadAttractionsFromLocationToBaseMarkerOptions(curLatLng,
                            placeList,
                            getActivity());
        }

        if (baseMarkerOptions.size() == 0) {
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getContext(), R.string.no_nearby, duration);
            toast.show();
        }

        placeMarkers();
    }

    @Override
    public void onLoaderReset(Loader<Bundle> loader) {

    }
}
