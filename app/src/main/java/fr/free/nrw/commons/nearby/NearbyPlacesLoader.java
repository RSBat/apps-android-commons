package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.UriSerializer;

public class NearbyPlacesLoader extends AsyncTaskLoader<Bundle> {
    private LatLng curLatLang;
    private Bundle data;

    public NearbyPlacesLoader(Context context) {
        super(context);
    }

    public NearbyPlacesLoader(Context context, LatLng latLng) {
        super(context);

        curLatLang = latLng;
    }

    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public Bundle loadInBackground() {
        List<Place> placeList = NearbyController
                .loadAttractionsFromLocation(curLatLang, CommonsApplication.getInstance());

        Bundle bundle = new Bundle();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriSerializer())
                .create();
        String gsonPlaceList = gson.toJson(placeList);
        String gsonCurLatLng = gson.toJson(curLatLang);

        bundle.clear();
        bundle.putString("PlaceList", gsonPlaceList);
        bundle.putString("CurLatLng", gsonCurLatLng);

        return bundle;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    public void setLatLng(LatLng latLng) {
        curLatLang = latLng;
    }
}