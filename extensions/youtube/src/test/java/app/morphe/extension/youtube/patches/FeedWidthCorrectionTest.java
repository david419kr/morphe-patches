package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FeedWidthCorrectionTest {
    @Test public void correctsOppositeExtremesInThreeColumnFeed() {
        assertEquals(687, FeedWidthCorrection.choose(false, 2104, 2160, 687, 2104));
        assertEquals(2104, FeedWidthCorrection.choose(true, 687, 2160, 687, 2104));
        assertEquals(687, FeedWidthCorrection.choose(false, 687, 2160, 687, 2104));
        assertEquals(2104, FeedWidthCorrection.choose(true, 2104, 2160, 687, 2104));
    }

    @Test public void preservesUnknownOrOtherGrids() {
        assertEquals(2104, FeedWidthCorrection.choose(false, 2104, 2160, 0, 2104));
        assertEquals(2104, FeedWidthCorrection.choose(false, 2104, 2160, 1000, 2104));
        assertEquals(687, FeedWidthCorrection.choose(true, 687, 2160, 687, 0));
        assertEquals(1400, FeedWidthCorrection.choose(false, 1400, 2160, 687, 2104));
    }
}
