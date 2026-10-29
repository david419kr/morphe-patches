package app.morphe.extension.youtube.patches;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Rebind stale views using the current dataset, without rewriting content, flags or widths. */
@SuppressWarnings("unused")
public final class FeedBindingRecovery {
    public interface Binder {
        View patch_feedView();
        int patch_feedFullSpan(int position);
        void patch_rebindFeed();
    }
    public interface Position { int patch_feedPosition(); }
    private static final Map<View, Boolean> installed = new WeakHashMap<>();

    static boolean wrongSpan(boolean full, int width, int parent) {
        return parent > 0 && width > 0 && (full ? width * 100L < parent * 55L
                : width * 100L > parent * 75L);
    }

    public static void observe(Object object) {
        try {
            if (!(object instanceof Binder)) return;
            Binder binder = (Binder) object;
            View view = binder.patch_feedView();
            if (!(view instanceof ViewGroup) || view.getId() == View.NO_ID
                    || view.getResources().getConfiguration().screenWidthDp < 600
                    || !"results".equals(view.getResources().getResourceEntryName(view.getId()))) return;
            synchronized (installed) {
                if (installed.put(view, Boolean.TRUE) != null) return;
            }
            WeakReference<Binder> owner = new WeakReference<>(binder);
            WeakReference<ViewGroup> reference = new WeakReference<>((ViewGroup) view);
            view.post(() -> {
                ViewGroup list = reference.get();
                if (list == null) return;
                list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    long checked, lastRepair, windowStart;
                    int repeats, repairs;
                    String previous = "";
                    @Override public boolean onPreDraw() {
                        long now = SystemClock.elapsedRealtime();
                        if (now - checked < 250) return true;
                        checked = now;
                        ViewGroup current = reference.get();
                        Binder data = owner.get();
                        if (current == null || data == null || current.getWidth() <= 0) return true;
                        try {
                            StringBuilder mismatch = new StringBuilder();
                            int cells = 0;
                            for (int i = 0; i < current.getChildCount(); i++) {
                                View child = current.getChildAt(i);
                                if (!(child.getLayoutParams() instanceof Position)) continue;
                                int position = ((Position) child.getLayoutParams()).patch_feedPosition();
                                int full = data.patch_feedFullSpan(position);
                                if (full < 0) continue;
                                int width = child.getWidth();
                                if (width * 100L >= current.getWidth() * 20L
                                        && width * 100L <= current.getWidth() * 40L) cells++;
                                if (wrongSpan(full == 1, width, current.getWidth())) {
                                    mismatch.append(position).append(':').append(full).append(':').append(width).append(';');
                                }
                            }
                            String key = mismatch.toString();
                            if (key.isEmpty() || cells < 2) { previous = ""; repeats = 0; return true; }
                            if (key.equals(previous)) repeats++; else { previous = key; repeats = 1; }
                            // A static screen may have no next frame: schedule the confirmation draw.
                            if (repeats == 1) current.postDelayed(current::invalidate, 300);
                            if (now - windowStart > 60000) { windowStart = now; repairs = 0; }
                            if (repeats >= 2 && now - lastRepair > 2000 && repairs < 6) {
                                lastRepair = now; repairs++; repeats = 0;
                                Log.i("MorpheFeedRecovery", "stale bindings=" + key);
                                current.post(() -> {
                                    Binder target = owner.get();
                                    if (target != null) {
                                        try { target.patch_rebindFeed(); Log.i("MorpheFeedRecovery", "rebound current dataset"); }
                                        catch (Throwable e) { Log.w("MorpheFeedRecovery", "rebind failed", e); }
                                    }
                                });
                            }
                        } catch (Throwable e) { Log.w("MorpheFeedRecovery", "inspection failed", e); }
                        return true;
                    }
                });
                Log.i("MorpheFeedRecovery", "watching current feed");
            });
        } catch (Throwable e) { Log.w("MorpheFeedRecovery", "attach failed", e); }
    }
}
