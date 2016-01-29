package ielite.app.popularmovies.utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Suraj on 19-01-2016.
 */
public class Utility {
    public static final String SHARED_PREFS_MOVIE_APP  ="moviefavpref";
    public static final String MOVIE_FAV_KEY = "movieFavList";

    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}
