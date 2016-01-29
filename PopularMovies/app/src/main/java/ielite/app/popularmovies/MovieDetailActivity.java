package ielite.app.popularmovies;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.picasso.Picasso;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import ielite.app.popularmovies.async.MoviesTask;
import ielite.app.popularmovies.parcels.MovieParcel;
import ielite.app.popularmovies.parcels.MovieReviewsParcel;
import ielite.app.popularmovies.utility.Utility;

/**
 * An activity representing a single Movie detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * Movie details are presented side-by-side with a list of Movies
 * in a {@link MovieListActivity}.
 */
public class MovieDetailActivity extends AppCompatActivity implements MoviesTask.MoviedbResponse {

    private static final String MOVIE_TRAILERS_KEY = "movietrailers";
    private static final String MOVIE_REVIEWS_KEY = "moviereviews";
    private static final String BASE_URL = "http://api.themoviedb.org/3/movie/";
    private final String LOG_TAG = MovieDetailActivity.class.getSimpleName();
    int movie_id;
    MovieReviewsParcel[] movieReviewsParcels;
    private MovieParcel movie;
    private String title;
    private String poster;
    private String overview;
    private String release_date;
    private String votes;
    private ViewGroup trailersLayout;
    private ViewGroup reviewsLayout;
    private Button showReviews;
    private String[] movieTrailers;
    private boolean isSaveInstance;
    private boolean isShowReviews = false;
    private ArrayList<MovieReviewsParcel> movieReviewList;
    private android.support.v7.widget.ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(LOG_TAG, "In method: OnCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_movie_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //get a reference to the View objects in the Detail layout
        TextView titleView = (TextView) findViewById(R.id.title);
        TextView dateView = (TextView) findViewById(R.id.releaseDate);
        TextView voteView = (TextView) findViewById(R.id.votes);
        ImageView posterView = (ImageView) findViewById(R.id.posterDetail);
        TextView overViewTextView = (TextView) findViewById(R.id.overviewText);
        final FloatingActionButton favoriteButton = (FloatingActionButton) findViewById(R.id.fab);
        trailersLayout = (ViewGroup) findViewById(R.id.movie_trailers);
        reviewsLayout = (ViewGroup) findViewById(R.id.movie_reviews);
        showReviews = (Button) findViewById(R.id.show_reviews);

        //get a reference to the Movie object
        movie = getIntent().getParcelableExtra("movie");

        //attach movie attributes to the Detail layout
        if (movie != null) {
            this.movie_id = movie.id;
            this.title = movie.title;
            this.poster = movie.poster;
            this.overview = movie.overview;
            this.release_date = movie.release_date;
            this.votes = movie.vote;
        }

        //Load ImageView with poster image
        final String posterImageBaseUrl = getApplicationContext().getResources().
                getString(R.string.image_base_url);

        if (poster.equals("null") || poster.equals(null) || poster.equals("")) {
            posterView.setImageResource(R.drawable.empty_photo);
        } else {
            Picasso.with(this)
                    .load(posterImageBaseUrl + poster)
                    .into(posterView);
        }

        //Populate TextViews with Movie Details
        titleView.setText(title);
        dateView.setText(getString(R.string.release_date) + release_date);
        voteView.setText(getString(R.string.ratings) + votes);
        overViewTextView.setText(getString(R.string.synopsis) + overview);

        if (savedInstanceState == null || !savedInstanceState.containsKey(MOVIE_REVIEWS_KEY) ||
                !savedInstanceState.containsKey(MOVIE_TRAILERS_KEY)) {
            Log.d(LOG_TAG, "MovieTrailers and Reviews not available");
            isSaveInstance = false;
            if (Utility.isNetworkAvailable(this)) {
                new FetchMovieTrailersTask().execute(movie_id);

            } else {

                Toast.makeText(this, "Please enable Internet Connection!",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            movieTrailers = savedInstanceState.getStringArray(MOVIE_TRAILERS_KEY);
            movieReviewList = savedInstanceState.getParcelableArrayList(MOVIE_REVIEWS_KEY);

            if (movieTrailers != null && movieReviewList != null) {
                Log.d(LOG_TAG, "movieTrailers retrieved: " + movieTrailers.length + " and reviews : " +
                        movieReviewList.size());
                setMovieTrailers(movieTrailers);
                setMovieReviews(movieReviewList);
            }
            isSaveInstance = true;
        }

        showReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reviewsLayout.setVisibility(View.VISIBLE);
                setMovieReviews(movieReviewList);
                reviewsLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        reviewsLayout.requestFocus();
                    }
                });
            }
        });

        //Handle Favourite Movies
        checkIfFavorite(movie_id, favoriteButton);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleFavorite(movie_id, favoriteButton);

            }
        });

        //Option to Share Movie Trailers
        if (movieTrailers != null && movieTrailers.length != 0 && mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createMovieShareIntent(movieTrailers[0]));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //represents the Home or Up button.
            NavUtils.navigateUpTo(this, new Intent(this, MovieListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void watchYoutubeVideo(String id) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.youtube.com/watch?v=" + id));
            startActivity(intent);
        }
    }

    public void setMovieTrailers(final String[] trailers) {

        if (trailers == null)
            return;
        if (trailers != null && trailers.length != 0 && mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createMovieShareIntent(trailers[0]));
        }
        final ViewGroup viewGroup = trailersLayout;

        // Remove all existing trailers (everything but first child, which is the header)
        for (int i = viewGroup.getChildCount() - 1; i >= 1; i--) {
            viewGroup.removeViewAt(i);
        }

        final LayoutInflater inflater = getLayoutInflater();
        boolean hasTrailers = true;

        for (int i = 0; i < trailers.length; i++) {

            int j = i + 1;

            final View trailerView = inflater
                    .inflate(R.layout.trailer_single_item, viewGroup, false);
            final TextView trailerTitle = (TextView) trailerView
                    .findViewById(R.id.trailerTitle);
            final ImageButton trailerPlay = (ImageButton) trailerView
                    .findViewById(R.id.trailerPlay);
            hasTrailers = true;
            trailerTitle.setText(String.format(Locale.US, "Trailer %d", j));
            final String source = trailers[i];
            trailerPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    watchYoutubeVideo(source);
                    Log.d(LOG_TAG, "clicked on source :" + source);
                }
            });
            viewGroup.addView(trailerView);
        }

        viewGroup.setVisibility(hasTrailers ? View.VISIBLE : View.GONE);

    }

    public void setMovieReviews(ArrayList<MovieReviewsParcel> reviews) {

        if (reviews == null || reviews.size() == 0 || isShowReviews)
            return;

        final ViewGroup viewGroup = reviewsLayout;

        final LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < reviews.size(); i++) {
            MovieReviewsParcel review = reviews.get(i);
            final View reviewView = inflater
                    .inflate(R.layout.review_single_item, viewGroup, false);
            final TextView authorView = (TextView) reviewView
                    .findViewById(R.id.reviewAuthor);
            final TextView contentView = (TextView) reviewView
                    .findViewById(R.id.reviewContent);
            final String author = review.author;
            final String content = review.content;
            authorView.setText(author);
            contentView.setText(content);
            viewGroup.addView(reviewView);
        }

        isShowReviews = true;


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray(MOVIE_TRAILERS_KEY, movieTrailers);
        outState.putParcelableArrayList(MOVIE_REVIEWS_KEY, movieReviewList);
    }

    private void handleFavorite(int movie_id, FloatingActionButton favoriteButton) {

        SharedPreferences prefs = getSharedPreferences(Utility.SHARED_PREFS_MOVIE_APP,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        Set<String> movieIdSet = getFavorites(prefs);
        Log.d(LOG_TAG, "favs from prefs with size : " + movieIdSet.size());
        String movieId = String.valueOf(movie_id);
        Drawable drawable = getResources().getDrawable(R.drawable.ic_star_white_48dp);
        drawable = DrawableCompat.wrap(drawable);
        if (movieIdSet.contains(movieId)) {

            if (movieIdSet.remove(movieId)) {
                Log.d(LOG_TAG, "remove movie ID from favs");
            }
            DrawableCompat.setTint(drawable, getResources().getColor(R.color.textWhite));
            favoriteButton.setImageDrawable(drawable);
            favoriteButton.setBackgroundTintList(ColorStateList.valueOf(getResources().
                    getColor(R.color.colorAccent)));

        } else {
            if (movieIdSet.add(movieId)) {
                Log.d(LOG_TAG, "ad movie ID to favs");
            }
            DrawableCompat.setTint(drawable, Color.YELLOW);
            favoriteButton.setImageDrawable(drawable);
            favoriteButton.setBackgroundTintList(ColorStateList.valueOf(getResources().
                    getColor(android.R.color.holo_green_light)));

        }
        editor.putStringSet(Utility.MOVIE_FAV_KEY, movieIdSet);
        Log.d(LOG_TAG, "current  favs " + movieIdSet.size());
        editor.commit();

    }

    private Set<String> getFavorites(SharedPreferences prefs) {
        Set<String> movieIdSet = prefs.getStringSet(Utility.MOVIE_FAV_KEY, null);
        if (movieIdSet == null) {
            movieIdSet = new HashSet<String>();
        }
        return movieIdSet;
    }

    private void checkIfFavorite(Integer movie_id, FloatingActionButton favoriteButton) {
        String id = String.valueOf(movie_id);
        SharedPreferences prefs = getSharedPreferences(Utility.SHARED_PREFS_MOVIE_APP,
                Context.MODE_PRIVATE);
        Drawable drawable = getResources().getDrawable(R.drawable.ic_star_white_48dp);
        drawable = DrawableCompat.wrap(drawable);
        if (getFavorites(prefs).contains(id)) {
            DrawableCompat.setTint(drawable, Color.YELLOW);
            favoriteButton.setImageDrawable(drawable);
            favoriteButton.setBackgroundTintList(ColorStateList.valueOf(getResources().
                    getColor(android.R.color.holo_green_light)));

        } else {

            DrawableCompat.setTint(drawable, getResources().getColor(R.color.textWhite));
            favoriteButton.setImageDrawable(drawable);
            favoriteButton.setBackgroundTintList(ColorStateList.valueOf(getResources().
                    getColor(R.color.colorAccent)));
        }

    }

    private Intent createMovieShareIntent(String source) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + source);
        return shareIntent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (android.support.v7.widget.ShareActionProvider)
                MenuItemCompat.getActionProvider(menuItem);
        return true;
    }

    public class FetchMovieTrailersTask extends AsyncTask<Integer, Void, String[]> {

        private final String LOG_TAG = FetchMovieTrailersTask.class.getSimpleName();

        private String[] getMovieDataFromJson(String movieJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String MOVIE_TRAILERS = "trailers";
            final String MOVIE_TRAILER_LIST = "youtube";
            final String MOVIE_REVIEWS = "reviews";
            final String MOVIE_REVIEWS_RESULT = "results";
            final String MOVIE_TRAILER_SOURCE = "source";
            final String MOVIE_REVIEW_AUTHOR = "author";
            final String MOVIE_REVIEWS_CONTENT = "content";

            JSONObject movieJson = new JSONObject(movieJsonStr);
            JSONObject movieTrailerObj = movieJson.getJSONObject(MOVIE_TRAILERS);
            JSONArray movieYoutubeArray = movieTrailerObj.getJSONArray(MOVIE_TRAILER_LIST);

            Log.d(LOG_TAG, "Movie Trailer Size :" + movieYoutubeArray.length());

            JSONObject movieReviewsObj = movieJson.getJSONObject(MOVIE_REVIEWS);
            JSONArray movieReviewArray = movieReviewsObj.getJSONArray(MOVIE_REVIEWS_RESULT);

            String[] resultStrs = new String[movieYoutubeArray.length()];
            for (int i = 0; i < movieYoutubeArray.length(); i++) {

                JSONObject movieData = movieYoutubeArray.getJSONObject(i);
                resultStrs[i] = movieData.getString(MOVIE_TRAILER_SOURCE);

                Log.d(LOG_TAG, "Movie Trailer :" + i + "\n" + resultStrs[i]);
            }

            Log.d(LOG_TAG, "Movie Reviews Size : " + movieReviewArray.length());

            if (movieReviewArray != null && movieReviewArray.length() > 0) {
                movieReviewsParcels = new MovieReviewsParcel[movieReviewArray.length()];

                for (int j = 0; j < movieReviewArray.length(); j++) {
                    JSONObject movieReviewData = movieReviewArray.getJSONObject(j);
                    movieReviewsParcels[j] = new MovieReviewsParcel(
                            movieReviewData.getString(MOVIE_REVIEW_AUTHOR),
                            movieReviewData.getString(MOVIE_REVIEWS_CONTENT)
                    );

                }
                movieReviewList = new ArrayList<MovieReviewsParcel>(Arrays.asList(movieReviewsParcels));
            }
            return resultStrs;
        }


        @Override
        protected String[] doInBackground(Integer... params) {

            if (params.length == 0)
                return null;

            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            String movieJsonStr = null;


            try {
                final String MOVIES_BASE_URL = BASE_URL + params[0] + "?";
                final String API_PARAM = "api_key";
                final String APPEND_PARAMS = "append_to_response";
                final String APPEND_TRAILERS_REVIEWS = "trailers,reviews";
                String api_key = getApplicationContext().getResources().getString(R.string.api_key);

                Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                        .appendQueryParameter(API_PARAM, api_key)
                        .appendQueryParameter(APPEND_PARAMS, APPEND_TRAILERS_REVIEWS)
                        .build();
                URL url = new URL(builtUri.toString());

                Log.d(LOG_TAG, "movies Uri  : " + builtUri.toString());

                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null) {
                    return null;
                }

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = bufferedReader.readLine()) != null) {

                    buffer.append(line + "\n");

                }

                if (buffer.length() == 0) {
                    return null;
                }

                movieJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "IO Error " + e);
                return null;
            } finally {
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }

            }
            try {
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final String[] result) {
            if (result != null) {
                movieTrailers = result;
                setMovieTrailers(result);
                setMovieReviews(movieReviewList);
            }

        }
    }
}
