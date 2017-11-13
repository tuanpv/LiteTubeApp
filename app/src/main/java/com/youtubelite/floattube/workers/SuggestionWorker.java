package com.youtubelite.floattube.workers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;

import com.youtubelite.floattube.extractor.exceptions.ExtractionException;
import com.youtubelite.floattube.report.ErrorActivity;
import com.youtubelite.floattube.R;
import com.youtubelite.floattube.extractor.SuggestionExtractor;

import java.io.IOException;
import java.util.List;

/**
 * Worker that get suggestions based on the query
 *
 * @author mauriciocolli
 */
public class SuggestionWorker extends AbstractWorker {

    private String query;
    private OnSuggestionResult callback;

    /**
     * Interface which will be called for result and errors
     */
    public interface OnSuggestionResult {
        void onSuggestionResult(@NonNull List<String> suggestions);
        void onSuggestionError(int messageId);
    }

    public SuggestionWorker(Context context, int serviceId, String query, OnSuggestionResult callback) {
        super(context, serviceId);
        this.callback = callback;
        this.query = query;
    }

    public static SuggestionWorker startForQuery(Context context, int serviceId, @NonNull String query, OnSuggestionResult callback) {
        SuggestionWorker worker = new SuggestionWorker(context, serviceId, query, callback);
        worker.start();
        return worker;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.callback = null;
        this.query = null;
    }

    @Override
    protected void doWork(int serviceId) throws Exception {
        SuggestionExtractor suggestionExtractor = getService().getSuggestionExtractorInstance();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String searchLanguageKey = getContext().getString(R.string.search_language_key);
        String searchLanguage = sharedPreferences.getString(searchLanguageKey, getContext().getString(R.string.default_language_value));

        final List<String> suggestions = suggestionExtractor.suggestionList(query, searchLanguage);

        if (callback != null && suggestions != null && !isInterrupted()) getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (isInterrupted() || callback == null) return;

                callback.onSuggestionResult(suggestions);
                onDestroy();
            }
        });
    }

    @Override
    protected void handleException(final Exception exception, int serviceId) {
        if (callback == null || getHandler() == null || isInterrupted()) return;

        if (exception instanceof ExtractionException) {
            View rootView = getContext() instanceof Activity ? ((Activity) getContext()).findViewById(android.R.id.content) : null;
            ErrorActivity.reportError(getHandler(), getContext(), exception, null, rootView, ErrorActivity.ErrorInfo.make(ErrorActivity.GET_SUGGESTIONS, getServiceName(), query, R.string.parsing_error));
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuggestionError(R.string.parsing_error);
                }
            });
        } else if (exception instanceof IOException) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuggestionError(R.string.network_error);
                }
            });
        } else {
            View rootView = getContext() instanceof Activity ? ((Activity) getContext()).findViewById(android.R.id.content) : null;
            ErrorActivity.reportError(getHandler(), getContext(), exception, null, rootView, ErrorActivity.ErrorInfo.make(ErrorActivity.GET_SUGGESTIONS, getServiceName(), query, R.string.general_error));
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuggestionError(R.string.general_error);
                }
            });
        }

    }
}
