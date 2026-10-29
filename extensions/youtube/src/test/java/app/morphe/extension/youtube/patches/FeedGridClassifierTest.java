package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FeedGridClassifierTest {
    @Test public void knownTemplatesUseExpectedSpan() {
        assertEquals(FeedGridClassifier.FULL, FeedGridClassifier.classify("shorts_shelf.eml|abc", false, false));
        assertEquals(FeedGridClassifier.FULL, FeedGridClassifier.classify("horizontal_shelf.eml-fe|abc", false, false));
        assertEquals(FeedGridClassifier.CELL, FeedGridClassifier.classify("video_lockup_with_attachment.eml|abc", true, false));
        assertEquals(FeedGridClassifier.CELL, FeedGridClassifier.classify("lockup.eml|abc", false, true));
    }

    @Test public void unknownOrNonVideoItemsKeepOriginalSpan() {
        assertEquals(FeedGridClassifier.KEEP, FeedGridClassifier.classify(null, false, false));
        assertEquals(FeedGridClassifier.KEEP, FeedGridClassifier.classify("lockup.eml|abc", false, false));
        assertEquals(FeedGridClassifier.KEEP, FeedGridClassifier.classify("video_lockup_with_attachment.eml|abc", false, false));
        assertEquals(FeedGridClassifier.KEEP, FeedGridClassifier.classify("unknown.eml|abc", true, true));
        assertEquals(FeedGridClassifier.KEEP, FeedGridClassifier.classify("shorts_shelf.eml_fake|abc", false, false));
        assertEquals(FeedGridClassifier.KEEP, FeedGridClassifier.classify("shorts_shelf.eml-evil", false, false));
        assertEquals(FeedGridClassifier.KEEP, FeedGridClassifier.classify("shorts_shelf.eml-x/y|abc", false, false));
    }
}
