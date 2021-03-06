package com.example.music.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.music.R;
import com.example.music.Song;
import com.example.music.SongData;
import com.example.music.adapters.SongListAdapter;
import com.example.music.interfaces.SongItemClickListener;
import com.example.music.services.MediaPlaybackService;

import java.util.LinkedList;

public abstract class BaseSongsFragment extends Fragment {
    private static final String TAG = BaseSongsFragment.class.getSimpleName();

    protected boolean isPlaying = true;
    protected MediaPlaybackService mediaPlaybackService;
    protected int mSongCurrentPosition = -1;
    protected int mSongCurrentId = -1;
    protected Boolean isPortrait;
    protected LinkedList<Song> mSongList = new LinkedList<>();
    protected SongListAdapter mAdapter;
    protected SongItemClickListener mSongItemClickListener;
    protected SongData mSongData;
    protected RecyclerView mRecyclerView;
    protected AllSongsFragment.SongPlayClickListener songPlayClickListener;
    protected View view;
    protected LinearLayout mLinearLayout;
    protected ImageView mSongImage;
    protected TextView mSongName;
    protected TextView mSongArtist;
    protected ImageView mSongPlayBtn;
    protected Song mSong;


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() == MediaPlaybackService.SONG_PLAY_COMPLETE) {
                if (intent.getAction() == MediaPlaybackService.SONG_PLAY_COMPLETE) {
                    String state = intent.getStringExtra(MediaPlaybackService.MESSAGE_SONG_PLAY_COMPLETE);
                    if (state == "play_normal") {
                        isPlaying = false;
                    } else isPlaying = true;
                    onReceiverSongComplete();
                }
            }
            if (intent.getAction() == MediaPlaybackService.SONG_PLAY_CHANGE) {
                String state = intent.getStringExtra(MediaPlaybackService.MESSAGE_SONG_PLAY_CHANGE);
                if (state == "song_state_play") {
                    isPlaying = true;
                } else if (state == "song_state_pause") {
                    isPlaying = false;
                } else {
                    mSongCurrentPosition = Integer.parseInt(intent.getStringExtra(MediaPlaybackService.MESSAGE_SONG_PLAY_CHANGE));
                    isPlaying = true;
                }
                onReceiverSongChange();
            }
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSongData = new SongData(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MediaPlaybackService.SONG_PLAY_COMPLETE);
        intentFilter.addAction(MediaPlaybackService.SONG_PLAY_CHANGE);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: " + mSongCurrentPosition);
        view = inflater.inflate(R.layout.fragment_all_songs, container, false);
        mLinearLayout = view.findViewById(R.id.play_song_layout);
        mSongImage = view.findViewById(R.id.song_image);
        mSongName = view.findViewById(R.id.song_name_play);
        mSongArtist = view.findViewById(R.id.song_artist_name);
        mSongPlayBtn = view.findViewById(R.id.song_play_button);
        mRecyclerView = view.findViewById(R.id.song_recyclerview);
        mRecyclerView.setHasFixedSize(true);

        updateAdapter();

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
//            mRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(VERTICAL_ITEM_SPACE));

        if (mAdapter != null) {
            mAdapter.setOnSongItemClickListener(mSongItemClickListener);
        }

        Log.d(TAG, String.valueOf(mediaPlaybackService != null));
        if (mediaPlaybackService != null && mSongCurrentPosition >= 0) {
            mSongCurrentPosition = mediaPlaybackService.getCurrentSongPosition();
            isPlaying = mediaPlaybackService.isPlaying();
            updateUI();
        }
        if (mSongCurrentPosition >= 0) {
            updateUI();
        }
        mSongPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: start" + mediaPlaybackService.isPlaying());
                if (mediaPlaybackService.isPlaying()) {
                    mediaPlaybackService.pause();
                    mSongPlayBtn.setImageResource(R.drawable.ic_media_play_light);
                    mediaPlaybackService.startForegroundService(mediaPlaybackService.getCurrentSongPosition(), false);
                    isPlaying = false;
                    mSongData.setPlaying(isPlaying);
                    mAdapter.notifyDataSetChanged();
                } else {
                    if (mediaPlaybackService.isFirst()) {
                        mediaPlaybackService.play(mSongCurrentPosition);
                        mediaPlaybackService.setFirst(false);
                    } else
                        mediaPlaybackService.start();
                    mSongPlayBtn.setImageResource(R.drawable.ic_media_pause_light);
                    mediaPlaybackService.startForegroundService(mediaPlaybackService.getCurrentSongPosition(), true);
                    isPlaying = true;
                    mSongData.setPlaying(isPlaying);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        // chuyen dang media fragment
        mLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songPlayClickListener != null)
                    Log.d(TAG, String.valueOf(mediaPlaybackService == null));
                isPlaying = mediaPlaybackService.isPlaying();
                int mCurrentStreamPosition = mediaPlaybackService.getCurrentStreamPosition() > mSong.getDuration() ? 0 : mediaPlaybackService.getCurrentStreamPosition();
                Log.d(TAG, "onClick: " + String.valueOf(mediaPlaybackService.getDuration()) + " " + mSong.getDuration());
                songPlayClickListener.onSongPlayClickListener(v, mSong, mSongCurrentPosition, mCurrentStreamPosition, mediaPlaybackService.isPlaying());
            }
        });

        // song menu click listener
        mAdapter.setOnSongBtnClickListener(new SongListAdapter.SongBtnClickListener() {
            @Override
            public void onSongBtnClickListener(ImageButton btn, View v, final Song song, final int pos) {
                updatePopupMenu(v,song,pos);
            }
        });
        return view;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mReceiver);
        }
    }

    protected abstract void updatePopupMenu(View v,Song song, int pos);

    public abstract void setSongCurrentPosition(int position);

    public abstract void setSongCurrentId(int id);

    public abstract void onReceiverSongComplete();

    public abstract void onReceiverSongChange();

    public abstract void setMediaPlaybackService(MediaPlaybackService mediaPlaybackService);

    public abstract void setOnSongPlayClickListener(AllSongsFragment.SongPlayClickListener songplayclicklistener);

    public abstract void setOnSongItemClickListener(SongItemClickListener songItemClickListener);

    public abstract void setPlaying(boolean playing);

    public abstract void updateAdapter();

    //    public abstract void updateUI();
    public void updateUI() {
        mSongData.setCurrentSongPossition(mSongCurrentPosition);
        mSongData.setPlaying(isPlaying);
        if (mediaPlaybackService != null)
            mSongData.setSongCurrentId(mediaPlaybackService.getCurrentSongId());
        mAdapter.setCurrentPos(mSongCurrentPosition);
        mRecyclerView.scrollToPosition(mSongCurrentPosition);
        mAdapter.notifyDataSetChanged();
        Song song = mSongData.getSongId(mSongData.getSongCurrentId());
        if (isPortrait) updatePlaySongLayout(song);
        Log.d(TAG, "updateUI: " + isPlaying);
    }

    public void updatePlaySongLayout(Song mSong) {
        this.mSong = mSong;
        mLinearLayout.setVisibility(View.VISIBLE);

        mSongName.setText(mSong.getTitle());
        mSongArtist.setText(mSong.getArtistName());
        if (isPlaying) {
            mSongPlayBtn.setImageResource(R.drawable.ic_media_pause_light);
        } else mSongPlayBtn.setImageResource(R.drawable.ic_media_play_light);
        Bitmap albumArt = SongData.getAlbumArt(mSong.getData());
        if (albumArt != null) {
            mSongImage.setImageBitmap(albumArt);
        } else {
            mSongImage.setImageResource(R.drawable.art_song_default);
        }
    }

    public void updateUILand() {
    }
}
