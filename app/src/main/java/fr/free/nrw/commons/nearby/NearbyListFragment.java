package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.utils.UriDeserializer;
import timber.log.Timber;

import static android.view.View.GONE;

public class NearbyListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Bundle> {
    private static final Type LIST_TYPE = new TypeToken<List<Place>>() {
    }.getType();
    private static final Type CUR_LAT_LNG_TYPE = new TypeToken<LatLng>() {
    }.getType();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Uri.class, new UriDeserializer())
            .create();
    private static final int PLACES_LOADER_ID = 0;

    private NearbyAdapterFactory adapterFactory;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LocationServiceManager locationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("NearbyListFragment created");
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // show animation and start loading data
            swipeRefreshLayout.setRefreshing(true);
            getLoaderManager().restartLoader(PLACES_LOADER_ID, null, this);
        });

        recyclerView = view.findViewById(R.id.listView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterFactory = new NearbyAdapterFactory(place -> NearbyInfoDialog.showYourself(getActivity(), place));

        progressBar = view.findViewById(R.id.progressBar);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getLoaderManager().initLoader(PLACES_LOADER_ID, null, this);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // show animation and force loader to reload data
                swipeRefreshLayout.setRefreshing(true);
                getLoaderManager().restartLoader(PLACES_LOADER_ID, null, this);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationManager != null) {
            locationManager.unregisterLocationManager();
        }
    }

    @Override
    public Loader<Bundle> onCreateLoader(int id, Bundle bundle) {
        if (locationManager == null) {
            locationManager = new LocationServiceManager(getContext());
            locationManager.registerLocationManager();
        }
        LatLng curLatLang = locationManager.getLatestLocation();

        return new NearbyPlacesLoader(getContext(), curLatLang);
    }

    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle bundle) {
        List<Place> placeList = Collections.emptyList();

        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList");
            placeList = gson.fromJson(gsonPlaceList, LIST_TYPE);

            String gsonLatLng = bundle.getString("CurLatLng");
            LatLng curLatLng = gson.fromJson(gsonLatLng, CUR_LAT_LNG_TYPE);

            placeList = NearbyController.loadAttractionsFromLocationToPlaces(curLatLng, placeList);
        }

        recyclerView.setAdapter(adapterFactory.create(placeList));
        swipeRefreshLayout.setRefreshing(false);

        progressBar.setVisibility(GONE);

        if (placeList.size() == 0) {
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getContext(), R.string.no_nearby, duration);
            toast.show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Bundle> loader) {

    }
}
