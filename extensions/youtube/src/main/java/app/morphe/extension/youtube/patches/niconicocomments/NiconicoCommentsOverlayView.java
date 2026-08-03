package app.morphe.extension.youtube.patches.niconicocomments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;

import app.morphe.extension.youtube.patches.VideoInformation;

/**
 * Allocation-free draw loop used to prove overlay placement and playback-time synchronization.
 */
final class NiconicoCommentsOverlayView extends View {
    private static final String[] COMMENTS = {
            "Android PoC comment",
            "Synced to playback time",
            "Pauses with the video",
    };
    private static final long[] START_OFFSETS_MS = {800, 2_100, 3_500};
    private static final long COMMENT_DURATION_MS = 7_000;
    private static final long UNSET_TIME = Long.MIN_VALUE;
    private static final long INITIAL_STABILIZATION_MS = 3_000;
    private static final long IDLE_FRAME_DELAY_MS = 250;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final float laneHeight;
    private final float firstBaseline;

    private String videoId = "";
    private boolean overlayActive;
    private boolean videoPlaying;
    private long timelineAnchorMs = UNSET_TIME;
    private long videoChangedAtMs;

    NiconicoCommentsOverlayView(Context context) {
        super(context);

        float density = context.getResources().getDisplayMetrics().density;
        float scaledDensity = density * context.getResources().getConfiguration().fontScale;
        float textSize = 20 * scaledDensity;

        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setTextSize(textSize);
        fillPaint.setTypeface(Typeface.DEFAULT_BOLD);

        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2 * density);
        strokePaint.setTextSize(textSize);
        strokePaint.setTypeface(Typeface.DEFAULT_BOLD);

        fillPaint.getFontMetrics(fontMetrics);
        laneHeight = fontMetrics.descent - fontMetrics.ascent + (8 * density);
        firstBaseline = -fontMetrics.ascent + (12 * density);

        setWillNotDraw(false);
        setVisibility(GONE);
    }

    void setVideoId(String nextVideoId) {
        if (nextVideoId.equals(videoId)) {
            return;
        }
        videoId = nextVideoId;
        timelineAnchorMs = UNSET_TIME;
        videoChangedAtMs = SystemClock.elapsedRealtime();
        invalidate();
    }

    void setOverlayActive(boolean active) {
        if (overlayActive == active) {
            return;
        }
        overlayActive = active;
        setVisibility(active ? VISIBLE : GONE);
        requestNextFrame();
    }

    void setVideoPlaying(boolean playing) {
        if (videoPlaying == playing) {
            return;
        }
        videoPlaying = playing;
        requestNextFrame();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!overlayActive || !videoPlaying || videoId.isEmpty()) {
            return;
        }

        long videoTimeMs = VideoInformation.getVideoTime();
        if (videoTimeMs < 0) {
            postInvalidateDelayed(IDLE_FRAME_DELAY_MS);
            return;
        }

        if (timelineAnchorMs == UNSET_TIME) {
            timelineAnchorMs = videoTimeMs;
        } else if (SystemClock.elapsedRealtime() - videoChangedAtMs < INITIAL_STABILIZATION_MS
                && videoTimeMs + 500 < timelineAnchorMs) {
            // The video ID hook can arrive while the previous video's final time is still visible.
            timelineAnchorMs = videoTimeMs;
        }

        boolean hasActiveOrUpcomingComment = false;
        for (int i = 0; i < COMMENTS.length; i++) {
            long commentStartMs = timelineAnchorMs + START_OFFSETS_MS[i];
            long elapsedMs = videoTimeMs - commentStartMs;
            if (elapsedMs < 0) {
                hasActiveOrUpcomingComment = true;
                continue;
            }
            if (elapsedMs > COMMENT_DURATION_MS) {
                continue;
            }

            hasActiveOrUpcomingComment = true;
            String text = COMMENTS[i];
            float textWidth = fillPaint.measureText(text);
            float progress = (float) elapsedMs / COMMENT_DURATION_MS;
            float x = getWidth() - (progress * (getWidth() + textWidth));
            float y = firstBaseline + (i * laneHeight);

            canvas.drawText(text, x, y, strokePaint);
            canvas.drawText(text, x, y, fillPaint);
        }

        if (hasActiveOrUpcomingComment) {
            postInvalidateOnAnimation();
        } else {
            // Poll slowly so seeking back into the PoC timeline redraws the comments.
            postInvalidateDelayed(IDLE_FRAME_DELAY_MS);
        }
    }

    private void requestNextFrame() {
        if (overlayActive && videoPlaying) {
            postInvalidateOnAnimation();
        }
    }
}
