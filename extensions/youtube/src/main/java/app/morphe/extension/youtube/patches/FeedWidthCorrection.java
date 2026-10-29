package app.morphe.extension.youtube.patches;

/** Correct only a clear full-width versus three-column measurement mismatch. */
final class FeedWidthCorrection {
    static int choose(boolean fullSpan, int measured, int listWidth, int cell, int full) {
        if (listWidth <= 0 || cell <= 0 || full <= 0 || measured <= 0) return measured;
        // Restrict this workaround to a measured three-column layout.
        if (cell * 100L < listWidth * 25L || cell * 100L > listWidth * 38L
                || full * 100L < listWidth * 75L || full > listWidth) return measured;
        if (fullSpan && measured * 100L < listWidth * 50L) return full;
        if (!fullSpan && measured * 100L >= listWidth * 75L) return cell;
        return measured;
    }
}
