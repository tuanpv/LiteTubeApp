package com.youtubelite.floattube.workers;

import android.content.Context;

import com.youtubelite.floattube.extractor.exceptions.ExtractionException;
import com.youtubelite.floattube.extractor.exceptions.ParsingException;
import com.youtubelite.floattube.report.ErrorActivity;
import com.youtubelite.floattube.MainActivity;
import com.youtubelite.floattube.R;
import com.youtubelite.floattube.extractor.channel.ChannelExtractor;
import com.youtubelite.floattube.extractor.channel.ChannelInfo;

import java.io.IOException;

/**
 * Extract {@link ChannelInfo} with {@link ChannelExtractor} from the given url of the given service
 *
 * @author mauriciocolli
 */
@SuppressWarnings("WeakerAccess")
public class ChannelExtractorWorker extends ExtractorWorker {
    //private static final String TAG = "ChannelExtractorWorker";

    private int pageNumber;
    private boolean onlyVideos;

    private ChannelInfo channelInfo = null;
    private OnChannelInfoReceive callback;

    /**
     * Interface which will be called for result and errors
     */
    public interface OnChannelInfoReceive {
        void onReceive(ChannelInfo info, boolean onlyVideos);
        void onError(int messageId);
        /**
         * Called when an unrecoverable error has occurred.
         * <p> This is a good place to finish the caller. </p>
         */
        void onUnrecoverableError(Exception exception);
    }

    /**
     * @param context           context for error reporting purposes
     * @param serviceId         id of the request service
     * @param channelUrl        channelUrl of the service (e.g. https://www.youtube.com/channel/UC_aEa8K-EOJ3D6gOs7HcyNg)
     * @param pageNumber        which page to extract
     * @param onlyVideos        flag that will be send by {@link OnChannelInfoReceive#onReceive(ChannelInfo, boolean)}
     * @param callback          listener that will be called-back when events occur (check {@link ChannelExtractorWorker.OnChannelInfoReceive})
     */
    public ChannelExtractorWorker(Context context, int serviceId, String channelUrl, int pageNumber, boolean onlyVideos, OnChannelInfoReceive callback) {
        super(context, channelUrl, serviceId);
        this.pageNumber = pageNumber;
        this.callback = callback;
        this.onlyVideos = onlyVideos;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.callback = null;
        this.channelInfo = null;
    }

    @Override
    protected void doWork(int serviceId, String url) throws Exception {
        ChannelExtractor extractor = getService().getChannelExtractorInstance(url, pageNumber);
        channelInfo = ChannelInfo.getInfo(extractor);

        if (!channelInfo.errors.isEmpty()) handleErrorsDuringExtraction(channelInfo.errors, ErrorActivity.REQUESTED_CHANNEL);

        if (callback != null && channelInfo != null && !isInterrupted()) getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (isInterrupted() || callback == null) return;

                callback.onReceive(channelInfo, onlyVideos);
                onDestroy();
            }
        });
    }


    @Override
    protected void handleException(final Exception exception, int serviceId, String url) {
        if (callback == null || getHandler() == null || isInterrupted()) return;

        if (exception instanceof IOException) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(R.string.network_error);
                }
            });
        } else if (exception instanceof ParsingException || exception instanceof ExtractionException) {
            ErrorActivity.reportError(getHandler(), getContext(), exception, MainActivity.class, null, ErrorActivity.ErrorInfo.make(ErrorActivity.REQUESTED_CHANNEL, getServiceName(), url, R.string.parsing_error));
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onUnrecoverableError(exception);
                }
            });
        } else {
            ErrorActivity.reportError(getHandler(), getContext(), exception, MainActivity.class, null, ErrorActivity.ErrorInfo.make(ErrorActivity.REQUESTED_CHANNEL, getServiceName(), url, R.string.general_error));
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onUnrecoverableError(exception);
                }
            });
        }
    }
}

