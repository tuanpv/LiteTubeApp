package com.youtubelite.floattube.fragments.detail;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.nirhart.parallaxscroll.views.ParallaxScrollView;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.youtubelite.floattube.ImageErrorLoadingListener;
import com.youtubelite.floattube.extractor.InfoItem;
import com.youtubelite.floattube.extractor.MediaFormat;
import com.youtubelite.floattube.extractor.NewPipe;
import com.youtubelite.floattube.extractor.stream_info.AudioStream;
import com.youtubelite.floattube.extractor.stream_info.VideoStream;
import com.youtubelite.floattube.fragments.BaseFragment;
import com.youtubelite.floattube.info_list.InfoItemBuilder;
import com.youtubelite.floattube.player.MainVideoPlayer;
import com.youtubelite.floattube.player.PopupVideoPlayer;
import com.youtubelite.floattube.report.ErrorActivity;
import com.youtubelite.floattube.util.AnimationUtils;
import com.youtubelite.floattube.util.Connectivity;
import com.youtubelite.floattube.util.Constants;
import com.youtubelite.floattube.util.Localization;
import com.youtubelite.floattube.util.PermissionHelper;
import com.youtubelite.floattube.util.Utils;
import com.youtubelite.floattube.workers.StreamExtractorWorker;

import com.youtubelite.floattube.R;
import com.youtubelite.floattube.ReCaptchaActivity;
import com.youtubelite.floattube.download.DownloadDialog;
import com.youtubelite.floattube.extractor.stream_info.StreamInfo;
import com.youtubelite.floattube.player.PlayVideoActivity;
import com.youtubelite.floattube.util.NavigationHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;

import static com.youtubelite.floattube.util.AnimationUtils.animateView;

public class VideoDetailFragment extends BaseFragment implements StreamExtractorWorker.OnStreamInfoReceivedListener, SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {
    private final String TAG = "VideoDetailFragment@" + Integer.toHexString(hashCode());

    private boolean showVideoPopup;

    // Amount of videos to show on start
    private static final int INITIAL_RELATED_VIDEOS = 8;

    private static final String KORE_PACKET = "org.xbmc.kore";
    private static final String STACK_KEY = "stack_key";
    private static final String INFO_KEY = "info_key";
    private static final String WAS_RELATED_EXPANDED_KEY = "was_related_expanded_key";

    public static final String AUTO_PLAY = "auto_play";

    private String thousand;
    private String million;
    private String billion;

    private ArrayList<VideoStream> sortedStreamVideosList;
    private ActionBarHandler actionBarHandler;

    private InfoItemBuilder infoItemBuilder = null;
    private StreamInfo currentStreamInfo = null;
    private StreamExtractorWorker curExtractorWorker;

    private String videoTitle;
    private String videoUrl;
    private int serviceId = -1;

    private static final int RELATED_STREAMS_UPDATE_FLAG = 0x1;
    private static final int RESOLUTIONS_MENU_UPDATE_FLAG = 0x2;
    private static final int TOOLBAR_ITEMS_UPDATE_FLAG = 0x4;
    private int updateFlags = 0;

    private boolean autoPlayEnabled;
    private boolean showRelatedStreams;
    private boolean wasRelatedStreamsExpanded = false;

    /*//////////////////////////////////////////////////////////////////////////
    // Views
    //////////////////////////////////////////////////////////////////////////*/

    private Spinner spinnerToolbar;

    private ParallaxScrollView parallaxScrollRootView;
    private RelativeLayout contentRootLayoutHiding;

    private Button thumbnailBackgroundButton;
    private ImageView thumbnailImageView;
    private ImageView thumbnailPlayButton;

    private View videoTitleRoot;
    private TextView videoTitleTextView;
    private ImageView videoTitleToggleArrow;
    private TextView videoCountView;

    private TextView detailControlsBackground;
    private TextView detailControlsPopup;

    private RelativeLayout videoDescriptionRootLayout;
    private TextView videoUploadDateView;
    private TextView videoDescriptionView;

    private View uploaderRootLayout;
    private Button uploaderButton;
    private TextView uploaderTextView;
    private ImageView uploaderThumb;

    private TextView thumbsUpTextView;
    private ImageView thumbsUpImageView;
    private TextView thumbsDownTextView;
    private ImageView thumbsDownImageView;
    private TextView thumbsDisabledTextView;

    private TextView nextStreamTitle;
    private RelativeLayout relatedStreamRootLayout;
    private LinearLayout relatedStreamsView;
    private ImageButton relatedStreamExpandButton;

    private InterstitialAd interstitial;
    private final static int LIMIT_INDEX = 6;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor editor;
    private int index = 0;
    private final static String INDEX_ADMOB = "index_admob";

    /*////////////////////////////////////////////////////////////////////////*/

    public static VideoDetailFragment getInstance(int serviceId, String url) {
        return getInstance(serviceId, url, "");
    }

    public static VideoDetailFragment getInstance(int serviceId, String videoUrl, String videoTitle) {
        VideoDetailFragment instance = getInstance();
        instance.selectVideo(serviceId, videoUrl, videoTitle);
        return instance;
    }

    public static VideoDetailFragment getInstance() {
        return new VideoDetailFragment();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Fragment's Lifecycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");

        if (savedInstanceState != null) {
            videoTitle = savedInstanceState.getString(Constants.KEY_TITLE);
            videoUrl = savedInstanceState.getString(Constants.KEY_URL);
            serviceId = savedInstanceState.getInt(Constants.KEY_SERVICE_ID, 0);
            wasRelatedStreamsExpanded = savedInstanceState.getBoolean(WAS_RELATED_EXPANDED_KEY, false);
            Serializable serializable = savedInstanceState.getSerializable(STACK_KEY);
            if (serializable instanceof Stack) {
                //noinspection unchecked
                Stack<StackItem> list = (Stack<StackItem>) serializable;
                stack.clear();
                stack.addAll(list);
            }

            Serializable serial = savedInstanceState.getSerializable(INFO_KEY);
            if (serial instanceof StreamInfo) currentStreamInfo = (StreamInfo) serial;
        }

        showRelatedStreams = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(getString(R.string.show_next_video_key), true);
        PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this);

        thousand = getString(R.string.short_thousand);
        million = getString(R.string.short_million);
        billion = getString(R.string.short_billion);


        // Preferences
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        try {
            editor = mPreferences.edit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create the interstitial.
        interstitial = new InterstitialAd(getActivity());
        interstitial.setAdUnitId(getString(R.string.interstitial_lite_tube));
        requestNewInterstitial();
        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int i) {
                requestNewInterstitial();
            }

            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("B36624619B602BB4EEEEB3EE6B0ECBC3")
                .build();
        interstitial.loadAd(adRequest);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreateView() called with: inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");
        return inflater.inflate(R.layout.fragment_video_detail, container, false);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        if (currentStreamInfo == null) selectAndLoadVideo(serviceId, videoUrl, videoTitle);
        else prepareAndLoad(currentStreamInfo, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Currently only used for enable/disable related videos
        // but can be extended for other live settings changes
        if (updateFlags != 0) {
            if (!isLoading.get() && currentStreamInfo != null) {
                if ((updateFlags & RELATED_STREAMS_UPDATE_FLAG) != 0) initRelatedVideos(currentStreamInfo);
                if ((updateFlags & RESOLUTIONS_MENU_UPDATE_FLAG) != 0) setupActionBarHandler(currentStreamInfo);
            }

            if ((updateFlags & TOOLBAR_ITEMS_UPDATE_FLAG) != 0 && actionBarHandler != null) actionBarHandler.updateItemsVisibility();
            updateFlags = 0;
        }

        // Check if it was loading when the activity was stopped/paused,
        // because when this happen, the curExtractorWorker is cancelled
        if (wasLoading.getAndSet(false)) selectAndLoadVideo(serviceId, videoUrl, videoTitle);
    }

    @Override
    public void onStop() {
        super.onStop();
        wasLoading.set(curExtractorWorker != null && curExtractorWorker.isRunning());
        if (curExtractorWorker != null && curExtractorWorker.isRunning()) curExtractorWorker.cancel();
        StreamInfoCache.getInstance().removeOldEntries();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        if (DEBUG) Log.d(TAG, "onDestroyView() called");
        thumbnailImageView.setImageBitmap(null);
        relatedStreamsView.removeAllViews();
        spinnerToolbar.setOnItemSelectedListener(null);

        spinnerToolbar = null;

        parallaxScrollRootView = null;
        contentRootLayoutHiding = null;

        thumbnailBackgroundButton = null;
        thumbnailImageView = null;
        thumbnailPlayButton = null;

        videoTitleRoot = null;
        videoTitleTextView = null;
        videoTitleToggleArrow = null;
        videoCountView = null;

        detailControlsBackground = null;
        detailControlsPopup = null;

        videoDescriptionRootLayout = null;
        videoUploadDateView = null;
        videoDescriptionView = null;

        uploaderButton = null;
        uploaderTextView = null;
        uploaderThumb = null;

        thumbsUpTextView = null;
        thumbsUpImageView = null;
        thumbsDownTextView = null;
        thumbsDownImageView = null;
        thumbsDisabledTextView = null;

        nextStreamTitle = null;
        relatedStreamRootLayout = null;
        relatedStreamsView = null;
        relatedStreamExpandButton = null;

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.d(TAG, "onSaveInstanceState() called with: outState = [" + outState + "]");
        outState.putString(Constants.KEY_URL, videoUrl);
        outState.putString(Constants.KEY_TITLE, videoTitle);
        outState.putInt(Constants.KEY_SERVICE_ID, serviceId);
        outState.putSerializable(STACK_KEY, stack);

        int nextCount = currentStreamInfo != null && currentStreamInfo.next_video != null ? 2 : 0;
        if (relatedStreamsView != null && relatedStreamsView.getChildCount() > INITIAL_RELATED_VIDEOS + nextCount) {
            outState.putSerializable(WAS_RELATED_EXPANDED_KEY, true);
        }

        if (!isLoading.get() && (curExtractorWorker == null || !curExtractorWorker.isRunning())) {
            outState.putSerializable(INFO_KEY, currentStreamInfo);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ReCaptchaActivity.RECAPTCHA_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    NavigationHelper.openVideoDetailFragment(getFragmentManager(), serviceId, videoUrl, videoTitle);

                    try {
                        index = mPreferences.getInt(INDEX_ADMOB, 0);
                        index++;

                        if (Connectivity.isConnected(getActivity())) {
                            if (index % LIMIT_INDEX == 0) {
                                index = 0;
                                if (interstitial.isLoaded()) {
                                    interstitial.show();
                                } else {
                                    index--;
                                }
                            }
                        } else {
                            if (index % LIMIT_INDEX == 0) {
                                index--;
                            }
                        }
                        editor.putInt(INDEX_ADMOB, index).apply();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else Log.e(TAG, "ReCaptcha failed");
                break;
            default:
                Log.e(TAG, "Request code from activity not supported [" + requestCode + "]");
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.show_next_video_key))) {
            showRelatedStreams = sharedPreferences.getBoolean(key, true);
            updateFlags |= RELATED_STREAMS_UPDATE_FLAG;
        } else if (key.equals(getString(R.string.preferred_video_format_key))
                || key.equals(getString(R.string.default_resolution_key))
                || key.equals(getString(R.string.show_higher_resolutions_key))
                || key.equals(getString(R.string.use_external_video_player_key))) {
            updateFlags |= RESOLUTIONS_MENU_UPDATE_FLAG;
        } else if (key.equals(getString(R.string.show_play_with_kodi_key))) {
            updateFlags |= TOOLBAR_ITEMS_UPDATE_FLAG;
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // OnClick
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onClick(View v) {
        if (isLoading.get() || currentStreamInfo == null) return;

        switch (v.getId()) {
            case R.id.detail_controls_background:
                openInBackground();
                break;
            case R.id.detail_controls_popup:
                openInPopup();
                break;
            case R.id.detail_uploader_button:
                NavigationHelper.openChannelFragment(getFragmentManager(), currentStreamInfo.service_id, currentStreamInfo.channel_url, currentStreamInfo.uploader);

                try {
                    index = mPreferences.getInt(INDEX_ADMOB, 0);
                    index++;

                    if (Connectivity.isConnected(getActivity())) {
                        if (index % LIMIT_INDEX == 0) {
                            index = 0;
                            if (interstitial.isLoaded()) {
                                interstitial.show();
                            } else {
                                index--;
                            }
                        }
                    } else {
                        if (index % LIMIT_INDEX == 0) {
                            index--;
                        }
                    }
                    editor.putInt(INDEX_ADMOB, index).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.detail_thumbnail_background_button:
                playVideo(currentStreamInfo);
                break;
            case R.id.detail_title_root_layout:
                toggleTitleAndDescription();
                break;
            case R.id.detail_related_streams_expand:
                toggleExpandRelatedVideos(currentStreamInfo);
                break;
        }
    }

    @Override
    protected void reloadContent() {
        if (DEBUG) Log.d(TAG, "reloadContent() called");
        if (currentStreamInfo != null) StreamInfoCache.getInstance().removeInfo(currentStreamInfo);
        currentStreamInfo = null;
        for (StackItem stackItem : stack) if (stackItem.getUrl().equals(videoUrl)) stackItem.setInfo(null);
        prepareAndLoad(null, true);
    }

    private void openInBackground() {
        if (isLoading.get()) return;

        boolean useExternalAudioPlayer = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(activity.getString(R.string.use_external_audio_player_key), false);
        Intent intent;
        AudioStream audioStream = currentStreamInfo.audio_streams.get(Utils.getPreferredAudioFormat(activity, currentStreamInfo.audio_streams));
        if (!useExternalAudioPlayer && android.os.Build.VERSION.SDK_INT >= 16) {
            activity.startService(NavigationHelper.getOpenBackgroundPlayerIntent(activity, currentStreamInfo, audioStream));
            Toast.makeText(activity, R.string.background_player_playing_toast, Toast.LENGTH_SHORT).show();
        } else {
            intent = new Intent();
            try {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(audioStream.url),
                        MediaFormat.getMimeById(audioStream.format));
                intent.putExtra(Intent.EXTRA_TITLE, currentStreamInfo.title);
                intent.putExtra("title", currentStreamInfo.title);
                // HERE !!!
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.no_player_found)
                        .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(activity.getString(R.string.fdroid_vlc_url)));
                                activity.startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(TAG, "You unlocked a secret unicorn.");
                            }
                        });
                builder.create().show();
                Log.e(TAG, "Either no Streaming player for audio was installed, or something important crashed:");
                e.printStackTrace();
            }
        }
    }

    private void openInPopup() {
        if (isLoading.get()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !PermissionHelper.checkSystemAlertWindowPermission(activity)) {
            Toast toast = Toast.makeText(activity, R.string.msg_popup_permission, Toast.LENGTH_LONG);
            TextView messageView = (TextView) toast.getView().findViewById(android.R.id.message);
            if (messageView != null) messageView.setGravity(Gravity.CENTER);
            toast.show();
            return;
        }

        Toast.makeText(activity, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show();
        Intent mIntent = NavigationHelper.getOpenVideoPlayerIntent(activity, PopupVideoPlayer.class, currentStreamInfo, actionBarHandler.getSelectedVideoStream());
        activity.startService(mIntent);
    }

    private void toggleTitleAndDescription() {
        if (videoDescriptionRootLayout.getVisibility() == View.VISIBLE) {
            videoTitleTextView.setMaxLines(1);
            videoDescriptionRootLayout.setVisibility(View.GONE);
            videoTitleToggleArrow.setImageResource(R.drawable.arrow_down);
        } else {
            videoTitleTextView.setMaxLines(10);
            videoDescriptionRootLayout.setVisibility(View.VISIBLE);
            videoTitleToggleArrow.setImageResource(R.drawable.arrow_up);
        }
    }

    private void toggleExpandRelatedVideos(StreamInfo info) {
        if (DEBUG) Log.d(TAG, "toggleExpandRelatedVideos() called with: info = [" + info + "]");
        if (!showRelatedStreams) return;

        int nextCount = info.next_video != null ? 2 : 0;
        int initialCount = INITIAL_RELATED_VIDEOS + nextCount;

        if (relatedStreamsView.getChildCount() > initialCount) {
            relatedStreamsView.removeViews(initialCount, relatedStreamsView.getChildCount() - (initialCount));
            relatedStreamExpandButton.setImageDrawable(ContextCompat.getDrawable(activity, getResourceIdFromAttr(R.attr.expand)));
            return;
        }

        //Log.d(TAG, "toggleExpandRelatedVideos() called with: info = [" + info + "], from = [" + INITIAL_RELATED_VIDEOS + "]");
        for (int i = INITIAL_RELATED_VIDEOS; i < info.related_streams.size(); i++) {
            InfoItem item = info.related_streams.get(i);
            //Log.d(TAG, "i = " + i);
            relatedStreamsView.addView(infoItemBuilder.buildView(relatedStreamsView, item));
        }
        relatedStreamExpandButton.setImageDrawable(ContextCompat.getDrawable(activity, getResourceIdFromAttr(R.attr.collapse)));
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        spinnerToolbar = (Spinner) toolbar.findViewById(R.id.toolbar_spinner);

        parallaxScrollRootView = (ParallaxScrollView) rootView.findViewById(R.id.detail_main_content);

        //thumbnailRootLayout = (RelativeLayout) rootView.findViewById(R.id.detail_thumbnail_root_layout);
        thumbnailBackgroundButton = (Button) rootView.findViewById(R.id.detail_thumbnail_background_button);
        thumbnailImageView = (ImageView) rootView.findViewById(R.id.detail_thumbnail_image_view);
        thumbnailPlayButton = (ImageView) rootView.findViewById(R.id.detail_thumbnail_play_button);

        contentRootLayoutHiding = (RelativeLayout) rootView.findViewById(R.id.detail_content_root_hiding);

        videoTitleRoot = rootView.findViewById(R.id.detail_title_root_layout);
        videoTitleTextView = (TextView) rootView.findViewById(R.id.detail_video_title_view);
        videoTitleToggleArrow = (ImageView) rootView.findViewById(R.id.detail_toggle_description_view);
        videoCountView = (TextView) rootView.findViewById(R.id.detail_view_count_view);

        detailControlsBackground = (TextView) rootView.findViewById(R.id.detail_controls_background);
        detailControlsPopup = (TextView) rootView.findViewById(R.id.detail_controls_popup);

        videoDescriptionRootLayout = (RelativeLayout) rootView.findViewById(R.id.detail_description_root_layout);
        videoUploadDateView = (TextView) rootView.findViewById(R.id.detail_upload_date_view);
        videoDescriptionView = (TextView) rootView.findViewById(R.id.detail_description_view);

        //thumbsRootLayout = (LinearLayout) rootView.findViewById(R.id.detail_thumbs_root_layout);
        thumbsUpTextView = (TextView) rootView.findViewById(R.id.detail_thumbs_up_count_view);
        thumbsUpImageView = (ImageView) rootView.findViewById(R.id.detail_thumbs_up_img_view);
        thumbsDownTextView = (TextView) rootView.findViewById(R.id.detail_thumbs_down_count_view);
        thumbsDownImageView = (ImageView) rootView.findViewById(R.id.detail_thumbs_down_img_view);
        thumbsDisabledTextView = (TextView) rootView.findViewById(R.id.detail_thumbs_disabled_view);

        uploaderRootLayout = rootView.findViewById(R.id.detail_uploader_root_layout);
        uploaderButton = (Button) rootView.findViewById(R.id.detail_uploader_button);
        uploaderTextView = (TextView) rootView.findViewById(R.id.detail_uploader_text_view);
        uploaderThumb = (ImageView) rootView.findViewById(R.id.detail_uploader_thumbnail_view);

        relatedStreamRootLayout = (RelativeLayout) rootView.findViewById(R.id.detail_related_streams_root_layout);
        nextStreamTitle = (TextView) rootView.findViewById(R.id.detail_next_stream_title);
        relatedStreamsView = (LinearLayout) rootView.findViewById(R.id.detail_related_streams_view);
        relatedStreamExpandButton = ((ImageButton) rootView.findViewById(R.id.detail_related_streams_expand));

        actionBarHandler = new ActionBarHandler(activity);
        videoDescriptionView.setMovementMethod(LinkMovementMethod.getInstance());

        infoItemBuilder = new InfoItemBuilder(activity, rootView.findViewById(android.R.id.content));

        setHeightThumbnail();
    }

    protected void initListeners() {
        super.initListeners();
        infoItemBuilder.setOnStreamInfoItemSelectedListener(new InfoItemBuilder.OnInfoItemSelectedListener() {
            @Override
            public void selected(int serviceId, String url, String title) {
                //NavigationHelper.openVideoDetail(activity, url, serviceId);
                selectAndLoadVideo(serviceId, url, title);

                try {
                    index = mPreferences.getInt(INDEX_ADMOB, 0);
                    index++;

                    if (Connectivity.isConnected(getActivity())) {
                        if (index % LIMIT_INDEX == 0) {
                            index = 0;
                            if (interstitial.isLoaded()) {
                                interstitial.show();
                            } else {
                                index--;
                            }
                        }
                    } else {
                        if (index % LIMIT_INDEX == 0) {
                            index--;
                        }
                    }
                    editor.putInt(INDEX_ADMOB, index).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        videoTitleRoot.setOnClickListener(this);
        uploaderButton.setOnClickListener(this);
        thumbnailBackgroundButton.setOnClickListener(this);
        detailControlsBackground.setOnClickListener(this);
        detailControlsPopup.setOnClickListener(this);
        relatedStreamExpandButton.setOnClickListener(this);
    }

    private void initThumbnailViews(StreamInfo info) {
        if (info.thumbnail_url != null && !info.thumbnail_url.isEmpty()) {
            imageLoader.displayImage(info.thumbnail_url, thumbnailImageView, displayImageOptions, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    ErrorActivity.reportError(activity, failReason.getCause(), null, activity.findViewById(android.R.id.content), ErrorActivity.ErrorInfo.make(ErrorActivity.LOAD_IMAGE, NewPipe.getNameOfService(currentStreamInfo.service_id), imageUri, R.string.could_not_load_thumbnails));
                }
            });
        } else thumbnailImageView.setImageResource(R.drawable.dummy_thumbnail_dark);

        if (info.uploader_thumbnail_url != null && !info.uploader_thumbnail_url.isEmpty()) {
            imageLoader.displayImage(info.uploader_thumbnail_url, uploaderThumb, displayImageOptions,
                    new ImageErrorLoadingListener(activity, activity.findViewById(android.R.id.content), info.service_id));
        }
    }

    private void initRelatedVideos(StreamInfo info) {
        if (relatedStreamsView.getChildCount() > 0) relatedStreamsView.removeAllViews();

        if (info.next_video != null && showRelatedStreams) {
            nextStreamTitle.setVisibility(View.VISIBLE);
            relatedStreamsView.addView(infoItemBuilder.buildView(relatedStreamsView, info.next_video));
            relatedStreamsView.addView(getSeparatorView());
            relatedStreamRootLayout.setVisibility(View.VISIBLE);
        } else nextStreamTitle.setVisibility(View.GONE);

        if (info.related_streams != null && !info.related_streams.isEmpty() && showRelatedStreams) {
            //long first = System.nanoTime(), each;
            int to = info.related_streams.size() >= INITIAL_RELATED_VIDEOS ? INITIAL_RELATED_VIDEOS : info.related_streams.size();
            for (int i = 0; i < to; i++) {
                InfoItem item = info.related_streams.get(i);
                //each = System.nanoTime();
                relatedStreamsView.addView(infoItemBuilder.buildView(relatedStreamsView, item));
                //if (DEBUG) Log.d(TAG, "each took " + ((System.nanoTime() - each) / 1000000L) + "ms");
            }
            //if (DEBUG) Log.d(TAG, "Total time " + ((System.nanoTime() - first) / 1000000L) + "ms");

            relatedStreamRootLayout.setVisibility(View.VISIBLE);
            relatedStreamExpandButton.setVisibility(View.VISIBLE);

            relatedStreamExpandButton.setImageDrawable(ContextCompat.getDrawable(activity, getResourceIdFromAttr(R.attr.expand)));
        } else {
            if (info.next_video == null) relatedStreamRootLayout.setVisibility(View.GONE);
            relatedStreamExpandButton.setVisibility(View.GONE);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Menu
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        actionBarHandler.setupMenu(menu, inflater);
        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return actionBarHandler.onItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void setupActionBarHandler(final StreamInfo info) {
        sortedStreamVideosList = Utils.getSortedStreamVideosList(activity, info.video_streams, info.video_only_streams, false);
        actionBarHandler.setupStreamList(sortedStreamVideosList, spinnerToolbar);
        actionBarHandler.setOnShareListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                if (isLoading.get()) return;

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, info.webpage_url);
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, activity.getString(R.string.share_dialog_title)));
            }
        });

        actionBarHandler.setOnOpenInBrowserListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                if (isLoading.get()) return;

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(info.webpage_url));
                startActivity(Intent.createChooser(intent, activity.getString(R.string.choose_browser)));
            }
        });

        actionBarHandler.setOnPlayWithKodiListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {
                if (isLoading.get()) return;

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setPackage(KORE_PACKET);
                    intent.setData(Uri.parse(info.webpage_url.replace("https", "http")));
                    activity.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(R.string.kore_not_found)
                            .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(activity.getString(R.string.fdroid_kore_url)));
                                    activity.startActivity(intent);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    builder.create().show();
                }
            }
        });

        actionBarHandler.setOnDownloadListener(new ActionBarHandler.OnActionListener() {
            @Override
            public void onActionSelected(int selectedStreamId) {

                if (isLoading.get() || !PermissionHelper.checkStoragePermissions(activity)) {
                    return;
                }

                try {
                    DownloadDialog downloadDialog = DownloadDialog.newInstance(info, sortedStreamVideosList, selectedStreamId);
                    downloadDialog.show(activity.getSupportFragmentManager(), "downloadDialog");
                } catch (Exception e) {
                    Toast.makeText(activity, R.string.could_not_setup_download_menu, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
    }

    /*//////////////////////////////////////////////////////////////////////////
    // OwnStack
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Stack that contains the "navigation history".<br>
     * The peek is the current video.
     */
    private final Stack<StackItem> stack = new Stack<>();

    public void clearHistory() {
        stack.clear();
    }

    public void pushToStack(String videoUrl, String videoTitle) {
        if (DEBUG) Log.d(TAG, "pushToStack() called with: videoUrl = [" + videoUrl + "], videoTitle = [" + videoTitle + "]");
        if (stack.size() > 0 && stack.peek().getUrl().equals(videoUrl)) return;
        stack.push(new StackItem(videoUrl, videoTitle));
    }

    public void setTitleToUrl(String videoUrl, String videoTitle) {
        if (videoTitle != null && !videoTitle.isEmpty()) {
            for (StackItem stackItem : stack) {
                if (stackItem.getUrl().equals(videoUrl)) stackItem.setTitle(videoTitle);
            }
        }
    }

    public void setStreamInfoToUrl(String videoUrl, StreamInfo info) {
        if (info != null) {
            for (StackItem stackItem : stack) {
                if (stackItem.getUrl().equals(videoUrl)) stackItem.setInfo(info);
            }
        }
    }

    public boolean onActivityBackPressed() {
        if (DEBUG) Log.d(TAG, "onActivityBackPressed() called");
        // That means that we are on the start of the stack,
        // return false to let the MainActivity handle the onBack
        if (stack.size() == 1) return false;
        // Remove top
        stack.pop();
        // Get url from the new top
        StackItem peek = stack.peek();

        if (peek.getInfo() != null) selectAndHandleInfo(peek.getInfo());
        else selectAndLoadVideo(0, peek.getUrl(), !TextUtils.isEmpty(peek.getTitle()) ? peek.getTitle() : "");
        return true;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    public void setAutoplay(boolean autoplay) {
        this.autoPlayEnabled = autoplay;
    }

    public void selectVideo(int serviceId, String videoUrl, String videoTitle) {
        this.videoUrl = videoUrl;
        this.videoTitle = videoTitle;
        this.serviceId = serviceId;
    }

    public void selectAndHandleInfo(StreamInfo info) {
        selectAndHandleInfo(info, true);
    }

    public void selectAndHandleInfo(StreamInfo info, boolean scrollToTop) {
        if (DEBUG) Log.d(TAG, "selectAndHandleInfo() called with: info = [" + info + "], scrollToTop = [" + scrollToTop + "]");
        selectVideo(info.service_id, info.webpage_url, info.title);
        prepareAndLoad(info, scrollToTop);
    }

    public void selectAndLoadVideo(int serviceId, String videoUrl, String videoTitle) {
        selectAndLoadVideo(serviceId, videoUrl, videoTitle, true);
    }

    public void selectAndLoadVideo(int serviceId, String videoUrl, String videoTitle, boolean scrollToTop) {
        if (DEBUG) {
            Log.d(TAG, "selectAndLoadVideo() called with: serviceId = [" + serviceId + "], videoUrl = [" + videoUrl + "], videoTitle = [" + videoTitle + "], scrollToTop = [" + scrollToTop + "]");
        }

        selectVideo(serviceId, videoUrl, videoTitle);
        prepareAndLoad(null, scrollToTop);
    }

    /**
     * Prepare the UI for loading the info.</br>
     * If the argument info is not null, it'll be passed in {@link #handleStreamInfo(StreamInfo, boolean)}.</br>
     * If it is, check if the cache contains the info already.</br>
     * If the cache doesn't have the info, load from the network.
     *
     * @param info        info to prepare and load, can be null
     * @param scrollToTop whether or not scroll the scrollView to y = 0
     */
    public void prepareAndLoad(StreamInfo info, boolean scrollToTop) {
        if (DEBUG) Log.d(TAG, "prepareAndLoad() called with: info = [" + info + "]");
        isLoading.set(true);

        // Only try to get from the cache if the passed info IS null
        if (info == null && StreamInfoCache.getInstance().hasKey(videoUrl)) {
            info = StreamInfoCache.getInstance().getFromKey(videoUrl);
        }

        if (info != null) selectVideo(info.service_id, info.webpage_url, info.title);
        pushToStack(videoUrl, videoTitle);

        if (curExtractorWorker != null && curExtractorWorker.isRunning()) curExtractorWorker.cancel();
        AnimationUtils.animateView(spinnerToolbar, false, 200);
        AnimationUtils.animateView(errorPanel, false, 200);

        videoTitleTextView.setText(videoTitle != null ? videoTitle : "");
        videoTitleTextView.setMaxLines(1);
        AnimationUtils.animateView(videoTitleTextView, true, 0);

        videoDescriptionRootLayout.setVisibility(View.GONE);
        videoTitleToggleArrow.setImageResource(R.drawable.arrow_down);
        videoTitleToggleArrow.setVisibility(View.GONE);
        videoTitleRoot.setClickable(false);

        AnimationUtils.animateView(thumbnailPlayButton, false, 50);
        imageLoader.cancelDisplayTask(thumbnailImageView);
        imageLoader.cancelDisplayTask(uploaderThumb);
        thumbnailImageView.setImageBitmap(null);
        uploaderThumb.setImageBitmap(null);

        if (info != null) {
            final StreamInfo infoFinal = info;
            final boolean greaterThanThreshold = parallaxScrollRootView.getScrollY() >
                    (int) (getResources().getDisplayMetrics().heightPixels * .1f);

            if (scrollToTop) {
                if (greaterThanThreshold) parallaxScrollRootView.smoothScrollTo(0, 0);
                else parallaxScrollRootView.scrollTo(0, 0);
            }

            AnimationUtils.animateView(contentRootLayoutHiding, false, greaterThanThreshold ? 250 : 0, 0, new Runnable() {
                @Override
                public void run() {
                    handleStreamInfo(infoFinal, false);
                    isLoading.set(false);

                    showVideoPopup = PreferenceManager.getDefaultSharedPreferences(activity)
                            .getBoolean("pref_default_show_popup", false);
                    if (showVideoPopup) {
                        openInPopup();
                    }

                    showContentWithAnimation(greaterThanThreshold ? 120 : 200, 0, .02f);
                }
            });
        } else {
            if (scrollToTop) parallaxScrollRootView.smoothScrollTo(0, 0);
            curExtractorWorker = new StreamExtractorWorker(activity, serviceId, videoUrl, this);
            curExtractorWorker.start();
            AnimationUtils.animateView(loadingProgressBar, true, 200);
            AnimationUtils.animateView(contentRootLayoutHiding, false, 200);
        }
    }

    private void handleStreamInfo(@NonNull StreamInfo info, boolean fromNetwork) {
        if (DEBUG) Log.d(TAG, "handleStreamInfo() called with: info = [" + info + "]");
        currentStreamInfo = info;
        selectVideo(info.service_id, info.webpage_url, info.title);

        loadingProgressBar.setVisibility(View.GONE);
        AnimationUtils.animateView(thumbnailPlayButton, true, 200);

        // Since newpipe is designed to work even if certain information is not available,
        // the UI has to react on missing information.
        if (fromNetwork) videoTitleTextView.setText(info.title);

        if (!TextUtils.isEmpty(info.uploader)) uploaderTextView.setText(info.uploader);
        uploaderTextView.setVisibility(!TextUtils.isEmpty(info.uploader) ? View.VISIBLE : View.GONE);
        uploaderButton.setVisibility(!TextUtils.isEmpty(info.channel_url) ? View.VISIBLE : View.GONE);
        uploaderThumb.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.buddy));

        if (info.view_count >= 0) videoCountView.setText(Localization.localizeViewCount(info.view_count, activity));
        videoCountView.setVisibility(info.view_count >= 0 ? View.VISIBLE : View.GONE);

        if (info.dislike_count == -1 && info.like_count == -1) {
            thumbsDownImageView.setVisibility(View.VISIBLE);
            thumbsUpImageView.setVisibility(View.VISIBLE);
            thumbsUpTextView.setVisibility(View.GONE);
            thumbsDownTextView.setVisibility(View.GONE);

            thumbsDisabledTextView.setVisibility(View.VISIBLE);
        } else {
            thumbsDisabledTextView.setVisibility(View.GONE);

            if (info.dislike_count >= 0) thumbsDownTextView.setText(getShortCount((long) info.dislike_count));
            thumbsDownTextView.setVisibility(info.dislike_count >= 0 ? View.VISIBLE : View.GONE);
            thumbsDownImageView.setVisibility(info.dislike_count >= 0 ? View.VISIBLE : View.GONE);

            if (info.like_count >= 0) thumbsUpTextView.setText(getShortCount((long) info.like_count));
            thumbsUpTextView.setVisibility(info.like_count >= 0 ? View.VISIBLE : View.GONE);
            thumbsUpImageView.setVisibility(info.like_count >= 0 ? View.VISIBLE : View.GONE);
        }

        if (!TextUtils.isEmpty(info.upload_date)) videoUploadDateView.setText(Localization.localizeDate(info.upload_date, activity));
        videoUploadDateView.setVisibility(!TextUtils.isEmpty(info.upload_date) ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(info.description)) { //noinspection deprecation
            videoDescriptionView.setText(Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(info.description, 0) : Html.fromHtml(info.description));
        }
        videoDescriptionView.setVisibility(!TextUtils.isEmpty(info.description) ? View.VISIBLE : View.GONE);

        videoDescriptionRootLayout.setVisibility(View.GONE);
        videoTitleToggleArrow.setImageResource(R.drawable.arrow_down);
        videoTitleToggleArrow.setVisibility(View.VISIBLE);
        videoTitleRoot.setClickable(true);

        AnimationUtils.animateView(spinnerToolbar, true, 500);
        setupActionBarHandler(info);
        initThumbnailViews(info);
        initRelatedVideos(info);
        if (wasRelatedStreamsExpanded) {
            toggleExpandRelatedVideos(currentStreamInfo);
            wasRelatedStreamsExpanded = false;
        }

        setTitleToUrl(info.webpage_url, info.title);
        setStreamInfoToUrl(info.webpage_url, info);
    }

    public void playVideo(StreamInfo info) {
        // ----------- THE MAGIC MOMENT ---------------
        VideoStream selectedVideoStream = sortedStreamVideosList.get(actionBarHandler.getSelectedVideoStream());

        if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(this.getString(R.string.use_external_video_player_key), false)) {

            // External Player
            Intent intent = new Intent();
            try {
                intent.setAction(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(selectedVideoStream.url), MediaFormat.getMimeById(selectedVideoStream.format))
                        .putExtra(Intent.EXTRA_TITLE, info.title)
                        .putExtra("title", info.title);
                this.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.no_player_found)
                        .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent()
                                        .setAction(Intent.ACTION_VIEW)
                                        .setData(Uri.parse(getString(R.string.fdroid_vlc_url)));
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                builder.create().show();
            }
        } else {
            Intent mIntent;
            boolean useOldPlayer = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getBoolean(getString(R.string.use_old_player_key), false)
                    || (Build.VERSION.SDK_INT < 16);
            if (!useOldPlayer) {
                // ExoPlayer
                mIntent = NavigationHelper.getOpenVideoPlayerIntent(activity, MainVideoPlayer.class, info, actionBarHandler.getSelectedVideoStream());
            } else {
                // Internal Player
                mIntent = new Intent(activity, PlayVideoActivity.class)
                        .putExtra(PlayVideoActivity.VIDEO_TITLE, info.title)
                        .putExtra(PlayVideoActivity.STREAM_URL, selectedVideoStream.url)
                        .putExtra(PlayVideoActivity.VIDEO_URL, info.webpage_url)
                        .putExtra(PlayVideoActivity.START_POSITION, info.start_position);
            }
            startActivity(mIntent);
        }
    }

    private View getSeparatorView() {
        View separator = new View(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        int m8 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        int m5 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        params.setMargins(m8, m5, m8, m5);
        separator.setLayoutParams(params);

        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.separatorColor, typedValue, true);
        separator.setBackgroundColor(typedValue.data);

        return separator;
    }

    private void setHeightThumbnail() {
        boolean isPortrait = getResources().getDisplayMetrics().heightPixels > getResources().getDisplayMetrics().widthPixels;
        int height = isPortrait ? (int) (getResources().getDisplayMetrics().widthPixels / (16.0f / 9.0f))
                : (int) (getResources().getDisplayMetrics().heightPixels / 2f);
        thumbnailImageView.setScaleType(isPortrait ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        thumbnailImageView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height));
        thumbnailImageView.setMinimumHeight(height);
        thumbnailBackgroundButton.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height));
        thumbnailBackgroundButton.setMinimumHeight(height);
    }

    public String getShortCount(Long viewCount) {
        if (viewCount >= 1000000000) {
            return Long.toString(viewCount / 1000000000) + billion;
        } else if (viewCount >= 1000000) {
            return Long.toString(viewCount / 1000000) + million;
        } else if (viewCount >= 1000) {
            return Long.toString(viewCount / 1000) + thousand;
        } else {
            return Long.toString(viewCount);
        }
    }

    private void showContentWithAnimation(long duration, long delay, @FloatRange(from = 0.0f, to = 1.0f) float translationPercent) {
        int translationY = (int) (getResources().getDisplayMetrics().heightPixels *
                (translationPercent > 0.0f ? translationPercent : .12f));

        contentRootLayoutHiding.animate().setListener(null).cancel();
        contentRootLayoutHiding.setAlpha(0f);
        contentRootLayoutHiding.setTranslationY(translationY);
        contentRootLayoutHiding.setVisibility(View.VISIBLE);
        contentRootLayoutHiding.animate().alpha(1f).translationY(0)
                .setStartDelay(delay).setDuration(duration).setInterpolator(new FastOutSlowInInterpolator()).start();

        uploaderRootLayout.animate().setListener(null).cancel();
        uploaderRootLayout.setAlpha(0f);
        uploaderRootLayout.setTranslationY(translationY);
        uploaderRootLayout.setVisibility(View.VISIBLE);
        uploaderRootLayout.animate().alpha(1f).translationY(0)
                .setStartDelay((long) (duration * .5f) + delay).setDuration(duration).setInterpolator(new FastOutSlowInInterpolator()).start();

        if (showRelatedStreams) {
            relatedStreamRootLayout.animate().setListener(null).cancel();
            relatedStreamRootLayout.setAlpha(0f);
            relatedStreamRootLayout.setTranslationY(translationY);
            relatedStreamRootLayout.setVisibility(View.VISIBLE);
            relatedStreamRootLayout.animate().alpha(1f).translationY(0)
                    .setStartDelay((long) (duration * .8f) + delay).setDuration(duration).setInterpolator(new FastOutSlowInInterpolator()).start();
        }
    }
    /*//////////////////////////////////////////////////////////////////////////
    // OnStreamInfoReceivedListener callbacks
    //////////////////////////////////////////////////////////////////////////*/

    private void setErrorImage(final int imageResource) {
        if (thumbnailImageView == null || activity == null) return;
        thumbnailImageView.setImageDrawable(ContextCompat.getDrawable(activity, imageResource));
        AnimationUtils.animateView(thumbnailImageView, false, 0, 0, new Runnable() {
            @Override
            public void run() {
                AnimationUtils.animateView(thumbnailImageView, true, 500);
            }
        });
    }

    @Override
    protected void setErrorMessage(String message, boolean showRetryButton) {
        super.setErrorMessage(message, showRetryButton);

        if (!TextUtils.isEmpty(videoUrl)) StreamInfoCache.getInstance().removeInfo(videoUrl);
        currentStreamInfo = null;
    }

    @Override
    public void onReceive(StreamInfo info) {
        if (DEBUG) Log.d(TAG, "onReceive() called with: info = [" + info + "]");
        if (info == null || isRemoving() || !isVisible()) return;

        handleStreamInfo(info, true);
        showContentWithAnimation(300, 0, 0);

        AnimationUtils.animateView(loadingProgressBar, false, 200);

        if (autoPlayEnabled) {
            playVideo(info);
            // Only auto play in the first open
            autoPlayEnabled = false;
        }

        StreamInfoCache.getInstance().putInfo(info);
        isLoading.set(false);

        showVideoPopup = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean("pref_default_show_popup", false);
        if (showVideoPopup) {
            openInPopup();
        }
    }

    @Override
    public void onError(int messageId) {
        if (DEBUG) Log.d(TAG, "onError() called with: messageId = [" + messageId + "]");
        //Toast.makeText(activity, messageId, Toast.LENGTH_LONG).show();
        setErrorImage(R.drawable.not_available_monkey);
        setErrorMessage(getString(messageId), true);
    }

    @Override
    public void onReCaptchaException() {
        Toast.makeText(activity, R.string.recaptcha_request_toast, Toast.LENGTH_LONG).show();
        // Starting ReCaptcha Challenge Activity
        startActivityForResult(new Intent(activity, ReCaptchaActivity.class), ReCaptchaActivity.RECAPTCHA_REQUEST);

        setErrorMessage(getString(R.string.recaptcha_request_toast), false);
    }

    @Override
    public void onBlockedByGemaError() {

        thumbnailBackgroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.c3s_url)));
                startActivity(intent);
            }
        });

        setErrorImage(R.drawable.gruese_die_gema);
        setErrorMessage(getString(R.string.blocked_by_gema), false);
    }

    @Override
    public void onContentErrorWithMessage(int messageId) {
        setErrorImage(R.drawable.not_available_monkey);
        setErrorMessage(getString(messageId), false);
    }

    @Override
    public void onContentError() {
        setErrorImage(R.drawable.not_available_monkey);
        setErrorMessage(getString(R.string.content_not_available), false);
    }

    @Override
    public void onUnrecoverableError(Exception exception) {
        activity.finish();
    }
}
