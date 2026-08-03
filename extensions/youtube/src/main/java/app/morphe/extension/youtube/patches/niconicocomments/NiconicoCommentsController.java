package app.morphe.extension.youtube.patches.niconicocomments;

import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.VideoState;
import kotlin.Unit;

/** Coordinates the injected player hooks with the lightweight overlay view. */
public final class NiconicoCommentsController {
    private static WeakReference<ViewGroup> overlayRootRef = new WeakReference<>(null);
    private static WeakReference<NiconicoCommentsOverlayView> overlayViewRef =
            new WeakReference<>(null);

    private static volatile String currentVideoId = "";
    private static volatile boolean playerVisible = isPlayerVisible(PlayerType.getCurrent());
    private static volatile boolean videoPlaying = VideoState.getCurrent() == VideoState.PLAYING;

    static {
        PlayerType.getOnChange().addObserver(playerType -> {
            playerVisible = isPlayerVisible(playerType);
            updateViewState();
            return Unit.INSTANCE;
        });
        VideoState.getOnChange().addObserver(videoState -> {
            videoPlaying = videoState == VideoState.PLAYING;
            updateViewState();
            return Unit.INSTANCE;
        });
    }

    private NiconicoCommentsController() {
    }

    /** Injection point called when YouTube creates its player overlay. */
    public static void initialize(ViewGroup overlayRoot) {
        if (overlayRoot == null) {
            return;
        }
        overlayRoot.post(() -> {
            try {
                attachToOverlay(overlayRoot);
            } catch (Exception exception) {
                Logger.printException(() -> "Niconico comments initialization failed", exception);
            }
        });
    }

    /** Injection point called when the foreground video changes. */
    public static void onVideoIdChanged(String videoId) {
        String nextVideoId = videoId == null ? "" : videoId;
        if (nextVideoId.equals(currentVideoId)) {
            return;
        }

        currentVideoId = nextVideoId;
        NiconicoCommentsOverlayView view = overlayViewRef.get();
        if (view != null) {
            view.post(() -> {
                view.setVideoId(nextVideoId);
                applyViewState(view);
            });
        }
    }

    private static void attachToOverlay(ViewGroup overlayRoot) {
        NiconicoCommentsOverlayView currentView = overlayViewRef.get();
        ViewGroup currentRoot = overlayRootRef.get();
        if (currentRoot == overlayRoot && currentView != null && currentView.getParent() == overlayRoot) {
            currentView.setVideoId(currentVideoId);
            applyViewState(currentView);
            return;
        }

        if (currentView != null && currentView.getParent() instanceof ViewGroup parent) {
            parent.removeView(currentView);
        }

        NiconicoCommentsOverlayView newView = new NiconicoCommentsOverlayView(overlayRoot.getContext());
        newView.setVideoId(currentVideoId);
        newView.setClickable(false);
        newView.setFocusable(false);
        newView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        overlayRoot.addView(
                newView,
                0,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        overlayRootRef = new WeakReference<>(overlayRoot);
        overlayViewRef = new WeakReference<>(newView);
        applyViewState(newView);
    }

    private static boolean isPlayerVisible(PlayerType playerType) {
        return playerType != null && playerType.isMaximizedOrFullscreen();
    }

    private static void updateViewState() {
        NiconicoCommentsOverlayView view = overlayViewRef.get();
        if (view != null) {
            view.post(() -> applyViewState(view));
        }
    }

    private static void applyViewState(NiconicoCommentsOverlayView view) {
        view.setOverlayActive(playerVisible && !currentVideoId.isEmpty());
        view.setVideoPlaying(videoPlaying);
    }
}
