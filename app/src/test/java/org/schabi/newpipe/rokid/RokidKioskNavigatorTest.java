package org.schabi.newpipe.rokid;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RokidKioskNavigatorTest {
    @Test
    public void videosPreferGenericTrendingOverMusicEvenWhenMusicIsFirst() {
        assertEquals("Trending", RokidKioskNavigator.findVideoKioskForTesting(
                kiosks("trending_music", "Trending", "live")));
    }

    @Test
    public void videosFallbackToNonMusicVideoKiosk() {
        assertEquals("trending_gaming", RokidKioskNavigator.findVideoKioskForTesting(
                kiosks("trending_music", "trending_podcasts_episodes", "trending_gaming")));
    }

    @Test
    public void videosPreferWorkingYoutubeVideoCategoryOverBrokenGenericTrending() {
        assertEquals("trending_gaming", RokidKioskNavigator.findVideoKioskForTesting(kiosks(
                "live",
                "trending_podcasts_episodes",
                "trending_gaming",
                "trending_movies_and_shows",
                "trending_music",
                "Trending"
        )));
    }

    @Test
    public void videosDoNotOpenMusicOrPodcastsWhenNoVideoKiosk() {
        assertNull(RokidKioskNavigator.findVideoKioskForTesting(
                kiosks("live", "trending_music", "trending_podcasts_episodes")));
    }

    private static Set<String> kiosks(final String... kioskIds) {
        return new LinkedHashSet<>(Arrays.asList(kioskIds));
    }
}
