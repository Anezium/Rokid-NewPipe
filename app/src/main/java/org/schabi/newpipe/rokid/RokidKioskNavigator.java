package org.schabi.newpipe.rokid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.ServiceHelper;

import java.util.Set;

public final class RokidKioskNavigator {
    private RokidKioskNavigator() {
    }

    public static void open(@NonNull final Fragment fragment, final boolean live)
            throws ExtractionException {
        final StreamingService service = ServiceHelper.getSelectedService(
                fragment.requireContext());
        final String kioskId = findKiosk(service, live);
        if (service == null || kioskId == null) {
            return;
        }
        NavigationHelper.openKioskFragment(fragment.getParentFragmentManager(),
                service.getServiceId(), kioskId);
    }

    @Nullable
    public static String findKiosk(@Nullable final StreamingService service, final boolean live)
            throws ExtractionException {
        if (service == null) {
            return null;
        }

        final Set<String> kiosks = service.getKioskList().getAvailableKiosks();
        if (live) {
            return kiosks.contains("live") ? "live" : null;
        }

        return findVideoKiosk(kiosks);
    }

    @Nullable
    static String findVideoKioskForTesting(@NonNull final Set<String> kiosks) {
        return findVideoKiosk(kiosks);
    }

    @Nullable
    private static String findVideoKiosk(@NonNull final Set<String> kiosks) {
        final String[] preferredVideoKiosks = {
                "trending_gaming",
                "trending_movies_and_shows",
                "New & hot",
                "Recently added",
                "Top 50",
                "Trending"
        };
        for (final String kioskId : preferredVideoKiosks) {
            if (kiosks.contains(kioskId) && isVideoKiosk(kioskId)) {
                return kioskId;
            }
        }
        for (final String kioskId : kiosks) {
            if (isVideoKiosk(kioskId)) {
                return kioskId;
            }
        }
        return null;
    }

    private static boolean isVideoKiosk(@NonNull final String kioskId) {
        return !"live".equals(kioskId)
                && !"trending_music".equals(kioskId)
                && !"trending_podcasts_episodes".equals(kioskId);
    }
}
