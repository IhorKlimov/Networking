package myhexaville.com.networking;

import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import myhexaville.com.networking.databinding.ActivityMainBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends RxAppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    public static final String URL = "http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&page=1&api_key=daa8e62fb35a4e6821d58725b5abb88f";
    String sDetails = "http://api.themoviedb.org/3/movie/";
    private OkHttpClient mOkHttp;
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(mBinding.toolbar);


        mOkHttp = new OkHttpClient();
    }

    public void doNetworking(View view) {
//        asyncTaskWay();
//        okHttpWay();
//        rxJavaWay();
        chainRxJava();
    }

    private void asyncTaskWay() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mOkHttp.newCall(buildRequest(URL)).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Log.d(LOG_TAG, "onPostExecute: ");
                mBinding.text.setText("Yo Yo Yo");
            }
        }.execute();
    }

    private void okHttpWay() {
        mOkHttp.newCall(buildRequest(URL))
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(LOG_TAG, "onFailure: ");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(LOG_TAG, "onResponse: ");
                        runOnUiThread(() -> mBinding.text.setText("hey there"));
                    }
                });
    }

    private void rxJavaWay() {
        Observable.fromCallable(() -> mOkHttp.newCall(buildRequest(URL)).execute().body().string())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<String>() {
                    @Override
                    public void onNext(String value) {
                        Log.d(LOG_TAG, "onNext: " + value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(LOG_TAG, "onError: ", e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(LOG_TAG, "onComplete: ");
                        mBinding.text.setText("hey hey");
                    }
                });
    }

    private void chainRxJava() {
        Observable.fromCallable(() -> mOkHttp.newCall(MainActivity.this.buildRequest(URL)).execute().body().string())
                .flatMap(s -> {
                    String id = new JSONObject(s)
                            .getJSONArray("results")
                            .getJSONObject(0)
                            .getString("id");

                    String details = MainActivity.this.getDetails(id);
                    String title = new JSONObject(details).getString("title");

                    Movie movie = new Movie();
                    movie.title = title;
                    movie.id = id;

//                    return movie;
                    return Observable.fromIterable(new ArrayList<Movie>());

                })
                .map(m -> {
                    String video = getVideo(m.id);
                    m.video = "https://www.youtube.com/watch?v=" + new JSONObject(video)
                            .getJSONArray("results")
                            .getJSONObject(0)
                            .getString("key");
                    return m;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<Movie>() {
                    @Override
                    public void onNext(Movie m) {
                        Log.d(LOG_TAG, "onNext: " + m);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private String getVideo(String id) throws IOException {
        return mOkHttp.newCall(
                buildRequest(sDetails + id + "/videos?api_key=daa8e62fb35a4e6821d58725b5abb88f"))
                .execute()
                .body()
                .string();
    }

    private String getDetails(String id) throws IOException {
        return mOkHttp.newCall(
                buildRequest(sDetails + id + "?api_key=daa8e62fb35a4e6821d58725b5abb88f"))
                .execute()
                .body()
                .string();
    }


    private Request buildRequest(String url) {
        return new Request.Builder()
                .url(url)
                .build();
    }


}

