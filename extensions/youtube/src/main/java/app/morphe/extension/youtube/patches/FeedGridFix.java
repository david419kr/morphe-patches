package app.morphe.extension.youtube.patches;

import android.content.res.Configuration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<Class<?>, Field> binderListFields = new ConcurrentHashMap<>();
    private static final Map<RecyclerView, int[]> feedWidths = new WeakHashMap<>();
    private static int bindLogs;

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

    /** Called before a feed item's layout is calculated, while its render information is available. */
    public static int adjustMeasuredWidth(Object binder, View child, int spec, boolean fullSpan) {
        try {
            logOnce("bindHook", "feed measurement hook active");
            if (binder == null || child == null || Utils.getContext() == null
                    || Utils.getContext().getResources().getConfiguration().screenWidthDp < 600
                    || NavigationBar.isSearchBarActive()
                    || NavigationBar.NavigationButton.getSelectedNavigationButton() != NavigationBar.NavigationButton.HOME
                    || View.MeasureSpec.getMode(spec) != View.MeasureSpec.EXACTLY) return spec;

            Field field = binderListFields.get(binder.getClass());
            if (field == null) {
                for (Field candidate : binder.getClass().getDeclaredFields()) {
                    if (!Modifier.isStatic(candidate.getModifiers())
                            && RecyclerView.class.isAssignableFrom(candidate.getType())) {
                        candidate.setAccessible(true);
                        binderListFields.put(binder.getClass(), candidate);
                        field = candidate;
                        break;
                    }
                }
            }
            if (field == null) {
                logOnce("bindListField", "feed measurement binder list field unavailable");
                return spec;
            }
            if (!(field.get(binder) instanceof RecyclerView)) {
                logOnce("bindDetached", "feed measurement waiting for an attached list");
                return spec;
            }
            RecyclerView list = (RecyclerView) field.get(binder);
            if (list.getId() == View.NO_ID || !"results".equals(
                    list.getResources().getResourceEntryName(list.getId()))) return spec;
            int listWidth = list.getWidth();
            if (listWidth <= 0) return spec;

            int smallest = Integer.MAX_VALUE, left = Integer.MAX_VALUE, count = 0, full = 0;
            for (int i = 0; i < list.getChildCount(); i++) {
                View sibling = list.getChildAt(i);
                if (sibling == null) continue;
                int width = sibling.getWidth();
                if (width * 100L >= listWidth * 25L && width * 100L <= listWidth * 38L) {
                    smallest = Math.min(smallest, width);
                    if (sibling.getLeft() >= 0 && sibling.getLeft() < listWidth / 10) {
                        left = Math.min(left, sibling.getLeft());
                    }
                    count++;
                } else if (width * 100L >= listWidth * 75L && width <= listWidth) {
                    full = Math.max(full, width);
                }
            }
            int[] sizes;
            synchronized (feedWidths) {
                sizes = feedWidths.get(list);
                if (sizes == null || sizes[0] != listWidth) {
                    sizes = new int[]{listWidth, 0, 0};
                    feedWidths.put(list, sizes);
                }
                if (count >= 2) {
                    sizes[1] = smallest;
                    if (left != Integer.MAX_VALUE) sizes[2] = listWidth - 2 * left;
                }
                if (full > 0) sizes[2] = full;
            }

            int before = View.MeasureSpec.getSize(spec);
            int after = FeedWidthCorrection.choose(fullSpan, before, listWidth, sizes[1], sizes[2]);
            synchronized (logged) {
                if (bindLogs++ < 100 || before != after) {
                    Log.i("MorpheFeedBind", "view=" + Integer.toHexString(System.identityHashCode(child))
                            + " full=" + fullSpan + " width=" + before + "->" + after
                            + " list=" + listWidth + " cell=" + sizes[1] + " wide=" + sizes[2]);
                }
            }
            return after == before ? spec : View.MeasureSpec.makeMeasureSpec(after, View.MeasureSpec.EXACTLY);
        } catch (Throwable e) {
            logOnce("bindError:" + e.getClass().getSimpleName(), "bind fallback: " + e.getClass().getSimpleName());
            return spec;
        }
    }
}
