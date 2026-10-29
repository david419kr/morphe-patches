package app.morphe.extension.youtube.patches;

/** Classifies only known home-feed templates; unknown content keeps YouTube's decision. */
final class FeedGridClassifier {
    static final int KEEP = -1;
    static final int CELL = 0;
    static final int FULL = 1;

    static int classify(String uri, boolean classicVideo, boolean cardVideo) {
        if (matches(uri, "shorts_shelf.eml") || matches(uri, "horizontal_shelf.eml")) return FULL;
        if ((classicVideo && matches(uri, "video_lockup_with_attachment.eml"))
                || (cardVideo && matches(uri, "lockup.eml"))) return CELL;
        return KEEP;
    }

    private static boolean matches(String uri, String template) {
        if (uri == null || !uri.startsWith(template)) return false;
        int end = template.length();
        int hash = uri.indexOf('|', end);
        if (hash == end) return true;
        if (hash <= end + 1 || uri.charAt(end) != '-') return false;
        for (int i = end + 1; i < hash; i++) {
            char c = uri.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_')) return false;
        }
        return true;
    }
}
