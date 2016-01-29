package ielite.app.popularmovies.async;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

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

import ielite.app.popularmovies.R;
import ielite.app.popularmovies.parcels.MovieReviewsParcel;

/**
 * Created by Suraj on 24-01-2016.
 */

/**
 * This class handles connection to themoviedb.org and
 * parsing of JSON object response. Additionally, an
 * interface MoviedbResponse is declared that defines
 * methods setMovieTrailers and setMovieReviews
 */

public class MoviesTask extends AsyncTask<Integer, Void, String[]> {

    private static final String BASE_URL = "http://api.themoviedb.org/3/movie/";
    private final String LOG_TAG = MoviesTask.class.getSimpleName();
    public MoviedbResponse mDelegate = null;
    MovieReviewsParcel[] movieReviewsParcels;
    private ArrayList<MovieReviewsParcel> movieReviewList;
    private String[] movieTrailers;
    private Context mContext;

    public MoviesTask (Context context){
        mContext = context;
    }

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
            String api_key = mContext.getResources().getString(R.string.api_key);

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
            mDelegate.setMovieTrailers(result);
            mDelegate.setMovieReviews(movieReviewList);
        }

    }

    public interface MoviedbResponse {
        void setMovieTrailers(final String[] trailers);
        void setMovieReviews(ArrayList<MovieReviewsParcel> reviews);
    }
}

