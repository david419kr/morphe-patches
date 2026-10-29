package app.morphe.extension.youtube.patches;
import org.junit.Test;
import static org.junit.Assert.*;
public class FeedBindingRecoveryTest {
    @Test public void detectsObservedStaleVideoAndShelfWidths() {
        assertTrue(FeedBindingRecovery.wrongSpan(false, 2104, 2160));
        assertTrue(FeedBindingRecovery.wrongSpan(true, 687, 2160));
        assertFalse(FeedBindingRecovery.wrongSpan(false, 687, 2160));
        assertFalse(FeedBindingRecovery.wrongSpan(true, 2104, 2160));
    }
    @Test public void ignoresUnmeasuredAndIntermediateWidths() {
        assertFalse(FeedBindingRecovery.wrongSpan(true, 0, 2160));
        assertFalse(FeedBindingRecovery.wrongSpan(false, 687, 0));
        assertFalse(FeedBindingRecovery.wrongSpan(false, 1400, 2160));
        assertFalse(FeedBindingRecovery.wrongSpan(true, 1400, 2160));
    }
}
