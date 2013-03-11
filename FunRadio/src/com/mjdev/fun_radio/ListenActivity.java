package com.mjdev.fun_radio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.mjdev.fun_radio.util.M3uParser;
import com.mjdev.fun_radio.util.PlaylistParser;
import com.mjdev.fun_radio.util.PlaylistProvider;
import com.mjdev.fun_radio.util.PlsParser;
import com.mjdev.fun_radio.util.PlaylistProvider.Items;
import com.mjdev.fun_radio.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ListenActivity extends Activity implements OnClickListener,
    OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,
    OnInfoListener, OnSeekBarChangeListener, OnPreparedListener
     {
  public static final String EXTRA_CONTENT_URL = "extra_content_url";
  public static final String EXTRA_CONTENT_TITLE = "extra_content_title";
  public static final String EXTRA_CONTENT_ID = "extra_content_id";
  public static final String EXTRA_ENQUEUE = "extra_enqueue";
  public static final String EXTRA_PLAY_IMMEDIATELY = "extra_play_immediately";
  public static final String EXTRA_STREAM = "extra_stream";
  private PlaylistEntry current = null;
  private ImageButton   playButton;
  private SeekBar       progressBar;
  private TextView      lengthText;
  private TextView      infoText;
  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case 0:
        setPlayButton();
        break;
      case 1:
        updateProgress();
        break;
      case 2:
        enableProgress(true);
        break;
      case 3:
        enableProgress(false);
        break;
      }
    }
  };
  private Thread updateProgressThread;
  private BroadcastReceiver receiver = new ListenBroadcastReceiver();
  private ServiceConnection conn;
  private PlaybackService player;
  private boolean isPausedInCall = false;
  private TelephonyManager telephonyManager = null;
  private PhoneStateListener listener = null;
  private final static int RESUME_REWIND_TIME = 3000;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.listen);
    playButton = (ImageButton) findViewById(R.id.StreamPlayButton);
    playButton.setEnabled(false);
    playButton.setOnClickListener(this);
    progressBar = (SeekBar) findViewById(R.id.StreamProgressBar);
    progressBar.setMax(100);
    progressBar.setOnSeekBarChangeListener(this);
    progressBar.setEnabled(false);
    lengthText = (TextView) findViewById(R.id.StreamLengthText);
    lengthText.setText("00:00 - 00:00");
    infoText   = (TextView) findViewById(R.id.StreamTextView);
    telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    listener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
        switch (state) {
        case TelephonyManager.CALL_STATE_OFFHOOK:
        case TelephonyManager.CALL_STATE_RINGING:
          if (player != null && player.isPlaying()) {
            player.pause();
            isPausedInCall = true;
            setPlayButton();
          }
          break;
        case TelephonyManager.CALL_STATE_IDLE:
          if (isPausedInCall && player != null) {
            int resumePosition = player.getPosition() - RESUME_REWIND_TIME;
            if (resumePosition < 0) {
              resumePosition = 0;
            }
            player.seekTo(resumePosition);
            player.play();
            setPlayButton();
          }
          break;
        }
      }
    };
    telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    registerReceiver(receiver, new IntentFilter(this.getClass().getName()));
    Intent serviceIntent = new Intent(this, PlaybackService.class);
    conn = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        player = ((PlaybackService.ListenBinder) service).getService();
        onBindComplete((PlaybackService.ListenBinder) service);
      }
      @Override
      public void onServiceDisconnected(ComponentName name) {
        player = null;
      }
    };
    if (!PlaybackService.isRunning) getApplicationContext().startService(serviceIntent);
    getApplicationContext().bindService(serviceIntent, conn, 1);
    Intent i = new Intent(ListenActivity.class.getName())
    .putExtra(ListenActivity.EXTRA_CONTENT_URL, "0")
    .putExtra(ListenActivity.EXTRA_CONTENT_TITLE, "Rogy Radio")
    .putExtra(ListenActivity.EXTRA_ENQUEUE, false)
    .putExtra(ListenActivity.EXTRA_CONTENT_URL, getString(R.string.radio_url))
    .putExtra(ListenActivity.EXTRA_STREAM, true)
    .putExtra(ListenActivity.EXTRA_PLAY_IMMEDIATELY, true);
    sendBroadcast(i);
  }
  private void onBindComplete(PlaybackService.ListenBinder binder) {
    binder.setListener(this);
    if (player.isPlaying()) {
      current = player.getCurrentEntry();
      setPlayButton();
      play();
      startUpdateThread();
    }
  }
  private void play() {
    resetUI();
    new Thread(new Runnable() {
      public void run() {
        startListening();
      }
    }).start();
  }
  private void resetUI() {
    progressBar.setProgress(0);
    progressBar.setSecondaryProgress(0);
  }
  private void enableProgress(boolean enabled) {
    progressBar.setEnabled(enabled);
  }
  private void togglePlay() {
    if (isPausedInCall) {
      isPausedInCall = false;
    } 
    if (player.isPlaying()) {
      player.pause();
    } else {
      player.play();
    }
    setPlayButton();
  }
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
    case R.id.StreamPlayButton:
      togglePlay();
      break;
    }
  }
  private void setPlayButton() {
    playButton.setEnabled(true);
    if (player.isPlaying()) {
      playButton.setImageResource(android.R.drawable.ic_media_pause);
    } else {
      playButton.setImageResource(android.R.drawable.ic_media_play);
    }
  }
  public void updateProgress() {
    try {
      if (player.isPlaying()) {
        if (player.getDuration() > 0) {
          int progress = 100 * player.getPosition() / player.getDuration();
          progressBar.setProgress(progress);
        }
        updatePlayTime();
        infoText.setText("NO INFO");
      }
    } catch (IllegalStateException e) {
    }
  }
  private void listen(final String url, boolean stream)
      throws IllegalArgumentException, IllegalStateException, IOException {
    if (updateProgressThread != null && updateProgressThread.isAlive()) {
      updateProgressThread.interrupt();
      try {
        updateProgressThread.join();
      } catch (InterruptedException e) {
      }
    }
    player.stop();
    player.listen(url, stream);
    handler.sendEmptyMessage(stream ? 3 : 2);
  }
  private void startListening() {
    String url = current.url;
    try {
      if (url == null || url.equals("")) {
      } else if (isPlaylist(url)) {
        downloadPlaylistAndPlay();
      } else {
        listen(url, current.isStream);
      }
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalStateException e) {
    } catch (IOException e) {
    }
  }
  private boolean isPlaylist(String url) { return url.indexOf("m3u") > -1 || url.indexOf("pls") > -1; }
  private void downloadPlaylistAndPlay() throws MalformedURLException, IOException {
    String url = current.url;
    Log.i("FunRadio","playlist url:" + url);
    Log.i("FunRadio","getting playlist data");
    URL url1 = new URL((getString(R.string.radio_url)).replace(" ", "%20"));
    InputStream stream = url1.openStream();
    Log.i("FunRadio","saving playlist data");    
    File downloadingMediaFile = new File(getCacheDir(), "playlist_data");
    FileOutputStream out = new FileOutputStream(downloadingMediaFile);
    byte buf[] = new byte[1024];
    int totalBytesRead = 0, incrementalBytesRead = 0;
    int numread;
    while ((numread = stream.read(buf)) > 0) {
      out.write(buf, 0, numread);
      totalBytesRead += numread;
      incrementalBytesRead += numread;
    }
    stream.close();
    out.close();
    Log.i("FunRadio","parsing playlist data");
    Log.i("FunRadio","playlist data:" + buf.toString());
    PlaylistParser parser;
    if (url.indexOf(".m3u") > -1)      {
    	parser = new M3uParser(downloadingMediaFile);
    	Log.i("FunRadio","stream is m3u");
    }
    else if (url.indexOf(".pls") > -1) {
    	parser = new PlsParser(downloadingMediaFile);
    	Log.i("FunRadio","stream is pls");
    }
    else return;
    String mp3Url = parser.getNextUrl();
    Log.i("FunRadio","stream url:" + mp3Url);
    listen(mp3Url, current.isStream);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (telephonyManager != null && listener != null) {
      telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
    }
    unregisterReceiver(receiver);
    getApplicationContext().unbindService(conn);
  }

  class ListenBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String url = intent.getStringExtra(EXTRA_CONTENT_URL);
      String title = intent.getStringExtra(EXTRA_CONTENT_TITLE);
      long id = intent.getLongExtra(EXTRA_CONTENT_ID, -1);
      boolean enqueue = intent.getBooleanExtra(EXTRA_ENQUEUE, false);
      boolean playImmediately = intent.getBooleanExtra(EXTRA_PLAY_IMMEDIATELY,
          false);
      boolean stream = intent.getBooleanExtra(EXTRA_STREAM, false);
      String storyID = intent.getStringExtra(Constants.EXTRA_STORY_ID);
      PlaylistEntry entry;
      if (id != -1) {
        entry = retrievePlaylistEntryById(id);
        if (entry == null) {
          return;
        }
      } else {
        entry = new PlaylistEntry(id, url, title, stream, -1, storyID);
      }

      if (enqueue) {
        addPlaylistItem(entry);
      }
      if (playImmediately) {
        current = entry;
        PlaybackService.setCurrent(current);
        play();
      }
    }
  }

  private static String msecToTime(int msec) {
    int sec = (msec / 1000) % 60;
    int min = (msec / 1000 / 60) % 60;
    int hour = msec / 1000 / 60 / 60;
    StringBuilder output = new StringBuilder();
    if (hour > 0) {
      output.append(hour).append(":");
      output.append(String.format("%02d", min)).append(":");
    } else {
      output.append(String.format("%d", min)).append(":");
    }
    output.append(String.format("%02d", sec));
    return output.toString();
  }

  public void updatePlayTime() {
    if (player.isPlaying()) {
      String current = msecToTime(player.getCurrentPosition());
      String total = msecToTime(player.getDuration());
      lengthText.setText(current + " / " + total);
    }
  }

  @Override
  public void onBufferingUpdate(MediaPlayer mp, int percent) {
    if (percent > 20 && percent % 5 != 0) {
      return;
    }
    progressBar.setSecondaryProgress(percent);
    updatePlayTime();
  }

  @Override
  public void onCompletion(MediaPlayer mp) {

  }

  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    new AlertDialog.Builder(this).setMessage(
        "Received error: " + what + ", " + extra).setCancelable(true).show();
    setPlayButton();
    return false;
  }

  @Override
  public boolean onInfo(MediaPlayer mp, int what, int extra) {
    return false;
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    int possibleProgress = progress > seekBar.getSecondaryProgress() ? seekBar
        .getSecondaryProgress() : progress;
    if (fromUser) {
      // Only seek to position if we've downloaded the content.
      int msec = player.getDuration() * possibleProgress / seekBar.getMax();
      player.seekTo(msec);
    }
    updatePlayTime();
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    current = player.getCurrentEntry();
    resetUI();
    startUpdateThread();
  }

  private void startUpdateThread() {
    handler.sendEmptyMessage(0);
    updateProgressThread = new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        while (true) {
          handler.sendEmptyMessage(1);
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    });
    updateProgressThread.start();
  }

  public static class PlaylistEntry {
    long id;
    final String url;
    final String title;
    final boolean isStream;
    int order;
    final String storyID;

    public PlaylistEntry(long id, String url, String title, boolean isStream,
        int order) {
      this(id, url, title, isStream, order, null);
    }

    public PlaylistEntry(long id, String url, String title, boolean isStream,
        int order, String storyID) {
      this.id = id;
      this.url = url;
      this.title = title;
      this.isStream = isStream;
      this.order = order;
      this.storyID = storyID;
    }
  }

  private void addPlaylistItem(PlaylistEntry entry) {
    ContentValues values = new ContentValues();
    values.put(Items.NAME, entry.title);
    values.put(Items.URL, entry.url);
    values.put(Items.IS_READ, false);
    values.put(Items.PLAY_ORDER, PlaylistProvider.getMax(this) + 1);
    values.put(Items.STORY_ID, entry.storyID);
    Uri insert = getContentResolver().insert(PlaylistProvider.CONTENT_URI,
        values);
    entry.id = ContentUris.parseId(insert);
  }

  private PlaylistEntry retrievePlaylistEntryById(long id) {
    Uri query = ContentUris.withAppendedId(PlaylistProvider.CONTENT_URI, id);
    Cursor cursor = getContentResolver().query(query, null, null, null,
        PlaylistProvider.Items.PLAY_ORDER);
    startManagingCursor(cursor);
    return getFromCursor(cursor);
  }

  private PlaylistEntry getFromCursor(Cursor c) {
    String title = null, url = null, storyID = null;
    long id;
    int order;
    if (c.moveToFirst()) {
      id = c.getInt(c.getColumnIndex(PlaylistProvider.Items._ID));
      title = c.getString(c.getColumnIndex(PlaylistProvider.Items.NAME));
      url = c.getString(c.getColumnIndex(PlaylistProvider.Items.URL));
      order = c.getInt(c.getColumnIndex(PlaylistProvider.Items.PLAY_ORDER));
      storyID = c.getString(c.getColumnIndex(PlaylistProvider.Items.STORY_ID));
      c.close();
      return new PlaylistEntry(id, url, title, false, order, storyID);
    }
    c.close();
    return null;
  }
}
