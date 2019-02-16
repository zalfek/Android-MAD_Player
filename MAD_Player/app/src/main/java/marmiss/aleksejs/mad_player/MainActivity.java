package marmiss.aleksejs.mad_player;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.renderscript.Double2;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener {
    private MediaPlayer mediaPlayer = null;
    private Button playButton;
    private Button nextButton;
    private Button prevButton;
    private Cursor cur;
    private Song currentSong;
    private TextView songName;
    private TextView songArtist;
    private SeekBar seekBar;
    private  Handler mHandler = new Handler();
    private Runnable mRunnable;
    private int position;
    boolean startPlaying = false;
    TextView textView;
    boolean denied = false;
    static final Integer READ_STORAGE_PERMISSION_REQUEST_CODE=0x3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!checkPermissionForReadExternalStorage()){
          try{
              requestPermissionForReadExternalStorage();
          }catch (Exception e){
              Log.d("Permission request", "Permission request failed");
          }
          while(!checkPermissionForReadExternalStorage()&& denied==false){
              try{
                  Thread.sleep(1000);
              }catch (InterruptedException ie){
                  Log.d("Wait", "Interupted");
              }
          }
        }

        playButton = findViewById(R.id.button2);
        nextButton = findViewById(R.id.nextButton);
        prevButton = findViewById(R.id.prevButton);
        textView = findViewById(R.id.textView);
        playButton.setEnabled(false);
        nextButton.setEnabled(false);
        prevButton.setEnabled(false);
        songName = (TextView) findViewById(R.id.song_info_textview);
        songArtist = (TextView) findViewById(R.id.artist);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mediaPlayer != null && fromUser){
                    mediaPlayer.seekTo(progress);
                    long milis = progress;
                    textView.setText(getTimeString(milis));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        nextButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position <cur.getCount()-1) {
                    changeTrack(++position);
                }else{
                    position =0;
                    changeTrack(position);
                }
            }
        });
        prevButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position >0) {
                    changeTrack(--position);
                }else{
                    position = cur.getCount()-1;
                    changeTrack(position);
                }
            }
        });

        ContentResolver cr = getContentResolver();
        cur =cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,null);

    }


    private String getTimeString(long millis) {
        StringBuffer buf = new StringBuffer();

        int hours = (int) (millis / (1000 * 60 * 60));
        int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

        buf
                .append(String.format("%02d", hours))
                .append(":")
                .append(String.format("%02d", minutes))
                .append(":")
                .append(String.format("%02d", seconds));

        return buf.toString();
    }

    public void playMedia(){
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            int max = mediaPlayer.getDuration();
            seekBar.setMax(max);
            startPlaying = true;

            MainActivity.this.runOnUiThread(mRunnable = new Runnable() {
              @Override
                public void run() {
                  if(mediaPlayer != null){
                        int mCurrentPosition = mediaPlayer.getCurrentPosition();
                        long currentTime = mCurrentPosition;
                        long totalTime = mediaPlayer.getDuration();
                        seekBar.setProgress(mCurrentPosition);
                            textView.setText(getTimeString(currentTime) + "\\" + getTimeString(totalTime));

                    }
                    mHandler.postDelayed(this, 1000);
                }
            });

        } else {
            mediaPlayer.pause();
            startPlaying = false;
        }

    }

    public void changeTrack ( int position){

        mediaPlayer.stop();
        mediaPlayer.release();
        mHandler.removeCallbacks(mRunnable);
        mediaPlayer = null;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        cur.moveToPosition(position);
        String name = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
        String artist = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        String url = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));

        currentSong = new Song(name,artist,url);
        try {
            mediaPlayer.setDataSource(url);
        } catch (IOException e) {
            Log.d("SetData", "Can not set data source!");
        }

        songName.setText(currentSong.getName());
        songArtist.setText(currentSong.getArtist());

        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.prepareAsync();
        seekBar.setProgress(0);
        textView.setText("00:00:00");
    }

    public void requestPermissionForReadExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean checkPermissionForReadExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    @Override
    protected void onPause() {
      super.onPause();
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("songName", currentSong.getName());
        editor.putString("songArtist", currentSong.getArtist());
        editor.putString("songUrl", currentSong.getUrl());
        editor.apply();
        startPlaying = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
            cur.moveToFirst();
            String name = preferences.getString("songName", cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));
            String artist = preferences.getString("songArtist", cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            String url = preferences.getString("songUrl", cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)));
            currentSong = new Song(name, artist, url);
            try {
                mediaPlayer.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            songName.setText(currentSong.getName());
            songArtist.setText(currentSong.getArtist());
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();

        }

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);

    }

    public void buttonClicked(View view){
        switch (view.getId()) {
            case R.id.button:
                Random r = new Random();
                position = r.nextInt(cur.getCount());
                changeTrack(position);
                break;

            case R.id.button2:
           playMedia();
            break;
        }

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
    playButton.setEnabled(true);
    nextButton.setEnabled(true);
    prevButton.setEnabled(true);
    if(startPlaying)
        playMedia();
    textView.setText("00:00:00" + "\\" + getTimeString(mediaPlayer.getDuration()));

    }

}
