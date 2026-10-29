package app.morphe.extension.youtube.patches;

import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

/** Optional, content-free diagnostics for feed grid measurement and view reuse. */
@SuppressWarnings("unused")
public final class FeedLayoutDiagnostics {
    private static final String TAG = "MorpheFeedDiag";
    private static final Map<RecyclerView, Boolean> attached = new WeakHashMap<>();

    public static void onChildAdded(RecyclerView list) {
        try {
            if (list.getId() == View.NO_ID || !"results".equals(
                    list.getResources().getResourceEntryName(list.getId())) || attached.containsKey(list)) return;
            attached.put(list, Boolean.TRUE);
            WeakReference<RecyclerView> reference = new WeakReference<>(list);
            Log.i(TAG, "attach list=" + identity(list) + " version=1");
            list.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                private long lastCheck;
                private String previous = "";

                @Override public boolean onPreDraw() {
                    RecyclerView current = reference.get();
                    if (current == null) return true;
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastCheck < 500) return true;
                    lastCheck = now;
                    try {
                        String snapshot = snapshot(current);
                        if (!snapshot.equals(previous)) {
                            previous = snapshot;
                            Log.i(TAG, snapshot);
                        }
                    } catch (Throwable e) {
                        Log.w(TAG, "snapshot failed: " + e.getClass().getSimpleName());
                    }
                    return true;
                }
            });
        } catch (Throwable e) {
            Log.w(TAG, "attach failed: " + e.getClass().getSimpleName());
        }
    }

    private static String snapshot(RecyclerView list) {
        StringBuilder out = new StringBuilder("list=").append(identity(list))
                .append(" bounds=").append(list.getWidth()).append('x').append(list.getHeight())
                .append(" rotation=").append(list.getDisplay() == null ? -1 : list.getDisplay().getRotation());
        appendGridManager(out, list);
        int count = list.getChildCount();
        out.append(" children=").append(count);
        for (int i = 0; i < Math.min(count, 12); i++) {
            View child = list.getChildAt(i);
            if (child == null) continue;
            out.append(" | ").append(i).append(':').append(identity(child))
                    .append(" xy=").append(child.getLeft()).append(',').append(child.getTop())
                    .append('-').append(child.getRight()).append(',').append(child.getBottom())
                    .append(" measured=").append(child.getMeasuredWidth()).append('x').append(child.getMeasuredHeight());
            ViewGroup.LayoutParams params = child.getLayoutParams();
            if (params != null) appendIntegerFields(out, params, "params");
        }
        return out.toString();
    }

    private static void appendGridManager(StringBuilder out, RecyclerView list) {
        for (Class<?> type = list.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(list);
                    if (value == null) continue;
                    for (Class<?> c = value.getClass(); c != null; c = c.getSuperclass()) {
                        if ("android.support.v7.widget.GridLayoutManager".equals(c.getName())) {
                            out.append(" manager=").append(identity(value));
                            appendDeclaredInts(out, value, c, "grid");
                            return;
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) { }
            }
        }
        out.append(" manager=unavailable");
    }

    private static void appendIntegerFields(StringBuilder out, Object value, String label) {
        for (Class<?> c = value.getClass(); c != null && !c.getName().startsWith("android.view."); c = c.getSuperclass()) {
            appendDeclaredInts(out, value, c, label);
        }
    }

    private static void appendDeclaredInts(StringBuilder out, Object value, Class<?> type, String label) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() != int.class || Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                out.append(' ').append(label).append('.').append(type.getSimpleName()).append('.')
                        .append(field.getName()).append('=').append(field.getInt(value));
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
    }

    private static String identity(Object object) {
        return object.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(object));
    }
}
