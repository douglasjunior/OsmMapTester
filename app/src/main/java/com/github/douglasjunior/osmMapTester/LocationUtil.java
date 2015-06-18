package com.github.douglasjunior.osmMapTester;

import android.location.Location;
import android.location.LocationManager;

/**
 * Created by douglas on 18/06/15.
 */
public class LocationUtil {

    private static final int TIMER_RATE = 0;
    private static final float MIN_ACCURACY = 15.0f;

    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (!LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
            return false;
        }

        if (MIN_ACCURACY < location.getAccuracy()) {
            return false;
        }

        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TIMER_RATE;
        boolean isSignificantlyOlder = timeDelta < -TIMER_RATE;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > MIN_ACCURACY;

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate) {
            return true;
        }
        return false;
    }
}
