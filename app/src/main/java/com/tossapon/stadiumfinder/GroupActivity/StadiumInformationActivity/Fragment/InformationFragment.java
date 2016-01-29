package com.tossapon.stadiumfinder.GroupActivity.StadiumInformationActivity.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatRatingBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tossapon.projectsport.R;
import com.tossapon.stadiumfinder.Api.GoogleServiceInterface;
import com.tossapon.stadiumfinder.Api.StadiumInterface;
import com.tossapon.stadiumfinder.App.LatLngModule;
import com.tossapon.stadiumfinder.GroupActivity.ReserveActivity.PreReserveActivity;
import com.tossapon.stadiumfinder.Model.Basic.Stadium;
import com.tossapon.stadiumfinder.Model.Response.GoogleRoutingResponse;
import com.tossapon.stadiumfinder.Model.Response.StadiumDetailResponse;
import com.tossapon.stadiumfinder.Network.Server;

import org.parceler.Parcels;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by Tossapon on 24/12/2558.
 */
public class InformationFragment extends Fragment {

    private static final String TAG = "InfomationFragment";
    @Bind(R.id.frag_std_inf_rating) AppCompatRatingBar rating;
    @Bind(R.id.frag_std_inf_tel) TextView textViewTel;
    @Bind(R.id.frag_std_inf_web) TextView textViewWeb;
    @Bind(R.id.frag_std_inf_btn_track) ImageView imageViewTractButton;
    @Bind(R.id.frag_std_inf_track) TextView textViewTrack;
    @Bind(R.id.frag_std_inf_rate) TextView textViewRate;
    @Bind(R.id.frag_std_inf_op_time) TextView textViewOpTime;
    @Bind(R.id.frag_std_inf_book) LinearLayout linearLayout;

    private Stadium stadium;
    private GoogleMap mMap;
    private String type;

    public static Fragment newInstance(Stadium stadium, String type) {
        Bundle b = new Bundle();
        b.putParcelable("stadium", Parcels.wrap(stadium));
        b.putString("type", type);
        Fragment f = new InformationFragment();
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stadium_inf, container, false);
        stadium = Parcels.unwrap(getArguments().getParcelable("stadium"));
        type = getArguments().getString("type");

        ButterKnife.bind(this, v);

        getMapInstanceAndSetup();
        pageContentSetup();
        setTravelTimeContent();

        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(), PreReserveActivity.class);
                i.putExtra("stadium", Parcels.wrap(stadium));
                i.putExtra("type", type);
                startActivity(i);
            }
        });
        return v;
    }

    private void setTravelTimeContent() {
        double lat;
        double lng;
        Retrofit googleClient = new Retrofit.Builder()
                .baseUrl(Server.ROUTE_BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GoogleServiceInterface googleService = googleClient.create(GoogleServiceInterface.class);
        if(LatLngModule.getInstance() != null) {
            lat = LatLngModule.getInstance().latitude;
            lng = LatLngModule.getInstance().longitude;
        }else{
            lat = 0;
            lng = 0;
        }
        Call<GoogleRoutingResponse> callGoogleService = googleService.getRoute(lat+","+lng, stadium.latitude+","+stadium.longitude, "car");
        callGoogleService.enqueue(new Callback<GoogleRoutingResponse>() {
                                      @Override
                                      public void onResponse(Response<GoogleRoutingResponse> response, Retrofit retrofit) {
                                          GoogleRoutingResponse res = response.body();
                                          if(res.routes.size() > 0){
                                              if(res.routes.get(0).legs.size() > 0){
                                                  textViewTrack.setText(res.routes.get(0).legs.get(0).duration.text);
                                                  return;
                                              }
                                          }

                                          textViewTrack.setText("เริ่มต้นแอปนำทาง");
                                      }

                                      @Override
                                      public void onFailure(Throwable t) {
//                                          Log.d(TAG, "onFailure: " + t.getMessage());
                                      }
                                  }
        );
    }

    private void pageContentSetup() {
        Retrofit client = new Retrofit.Builder()
                .baseUrl(Server.BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        StadiumInterface service = client.create(StadiumInterface.class);
        Call<StadiumDetailResponse> call = service.getStadiumDetail(stadium.id+"", type);
//        Log.d(TAG, "onCreateView: " + stadium.id + type);
        call.enqueue(new Callback<StadiumDetailResponse>() {
            @Override
            public void onResponse(Response<StadiumDetailResponse> response, Retrofit retrofit) {
                StadiumDetailResponse res = response.body();
//                Log.d(TAG, String.valueOf(res.data.price.max));
                textViewRate.setText(res.data.price.min + " - " + res.data.price.max + " ต่อ ชม.");
            }

            @Override
            public void onFailure(Throwable t) {
            }
        });

        rating.setRating((float) stadium.rating);
        textViewTel.setText(stadium.tel);
        textViewWeb.setText(stadium.link);
        textViewOpTime.setText(stadium.time_open + " ถึง " + stadium.time_close);
    }

    private void getMapInstanceAndSetup() {
        if(mMap == null){
            mMap = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.frag_std_inf_map)).getMap();
            mMap.getUiSettings().setAllGesturesEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
        }


        LatLng position = new LatLng(stadium.latitude, stadium.longitude);
//        Log.d(TAG, "getMapInstanceAndSetup: "+stadium.latitude+","+stadium.longitude);
        mMap.addMarker(new MarkerOptions().position(position).title(stadium.name));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(position)
                .bearing(0)
                .tilt(0)
                .zoom(16.0f)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
    }
}