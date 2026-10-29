package app.morphe.extension.youtube.patches;

import android.content.res.Configuration;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.ComponentType;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.Element;
import app.morphe.extension.youtube.innertube.FeedCardLayoutOuterClass.ElementModel;
import app.morphe.extension.youtube.shared.NavigationBar;

/** Experimental correction applied before YouTube creates the item render metadata. */
@SuppressWarnings("unused")
public final class FeedGridFix {
    public interface ElementBytes {
        byte[] patch_getFeedGridElementBytes();
    }

    private static final Set<String> logged = new HashSet<>();

    public static boolean fullSpan(Object item, boolean original) {
        try {
            logOnce("hook", "feed span hook active");
            if (!(item instanceof ElementBytes)) {
                logOnce("interface", "element bytes unavailable");
                return original;
            }
            if (Utils.getContext() == null) {
                logOnce("context", "app context unavailable");
                return original;
            }
            if (NavigationBar.isSearchBarActive()
                    || NavigationBar.NavigationButton.getSelectedNavigationButton() != NavigationBar.NavigationButton.HOME) {
                logOnce("navigation", "skipped outside the home feed or before navigation is ready");
                return original;
            }
            Configuration config = Utils.getContext().getResources().getConfiguration();
            if (config.screenWidthDp < 600) {
                logOnce("width", "skipped because screen width is below 600dp");
                return original;
            }
            byte[] bytes = ((ElementBytes) item).patch_getFeedGridElementBytes();
            if (bytes == null || bytes.length == 0) return original;
            ComponentType component = Element.parseFrom(bytes).getType().getComponentType();
            String uri = component.getTemplateConfig().getUriTemplateConfig().getUri();
            ElementModel model = component.getModel();
            boolean classicVideo = model.hasVideoWithContextModel();
            boolean cardVideo = model.hasLockupModel()
                    && model.getLockupModel().getViewModel().getLockupViewModel().getContentType() == 1;
            int decision = FeedGridClassifier.classify(uri, classicVideo, cardVideo);
            if (decision == FeedGridClassifier.KEEP) {
                int separator = uri.indexOf('|');
                String template = separator < 0 ? "" : uri.substring(0, separator);
                if (template.matches("[A-Za-z0-9_]{1,80}\\.eml(?:-[A-Za-z0-9_]+)?")) {
                    logOnce("unknown:" + template, "unhandled template=" + template);
                }
                return original;
            }
            boolean result = decision == FeedGridClassifier.FULL;
            String kind = result ? "shelf" : "video";
            logOnce(kind + ':' + original, "template=" + kind + " fullSpan=" + original + "->" + result);
            return result;
        } catch (Throwable e) {
            logOnce("error:" + e.getClass().getSimpleName(), "fallback: " + e.getClass().getSimpleName());
            return original;
        }
    }

    private static void logOnce(String key, String message) {
        synchronized (logged) {
            if (logged.size() < 64 && logged.add(key)) Log.i("MorpheFeedFix", message);
        }
    }
}
