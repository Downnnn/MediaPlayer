package com.example.downn.mediaplayer;



import android.app.Notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.Service;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicPlayerService extends Service{//implements Runnable {
    private static final String TAG = "MusicPlayerService";
    private static final String CHANNEL_ID = "1";
    private static final String CHANNEL_NAME = "MusicPlayerServiceChannel";
    private static final int NOTIFICATION_ID = 1; // 如果id设置为0,会导致不能设置为前台service1
    public static MediaPlayer mediaPlayer = null;
    private String url = null;
    private String MSG = null;
    private static int curposition;//第几首音乐
    private musicBinder musicbinder = null;
    private int currentPosition = 0;// 设置默认进度条当前位置
    private ArrayList<MusicMedia> musiclist;//音乐列表
    private MusicMedia musicMedia;//当前播放的音乐信息

    public MusicPlayerService() {
        Log.i(TAG,"MusicPlayerService......1");
        musicbinder = new musicBinder();
    }

    //通过bind 返回一个IBinder对象，然后改对象调用里面的方法实现参数的传递
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"onBind......");
        return musicbinder;
    }


    /**
     * 自定义的 Binder对象
     */
    public class musicBinder extends Binder {
        public MusicPlayerService getPlayInfo(){
            return MusicPlayerService.this;
        }
    }
    //得到当前播放位置
    public  int getCurrentPosition(){

        if(mediaPlayer != null){
            currentPosition = mediaPlayer.getCurrentPosition();
        }
        return currentPosition;
    }
    //得到总时长
    public  int getDuration(){
        return mediaPlayer.getDuration();// 总时长
    }
    //当前播放音乐
    public MusicMedia getMusicMedia() {
        return musicMedia;
    }

    //得到 mediaPlayer
    public MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }
    //得到 当前播放第几个音乐
    public int getCurposition(){
        return curposition;
    }
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate......2");
        super.onCreate();


        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        musiclist = MusicActivity.musicList;
        // 监听播放是否完成
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //我目前也不知道该干嘛,下一首嘛
                playnew();
            }
        });

    }

    private void playnew() {
        switch (MusicActivity.sharedPreferences.getInt("play_mode",-1)){
            case 0://随机
                curposition = (new Random()).nextInt(musiclist.size());
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            case 1://顺序
                curposition = (++curposition) % musiclist.size();
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            case 2://单曲
                url =  musiclist.get(curposition ).getUrl();
                palyer();
                break;
            default:
                break;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand......3");
        if(intent != null){

            MSG = intent.getStringExtra("MSG");

            if(MSG.equals("0")){
                url = intent.getStringExtra("url");
                curposition = intent.getIntExtra("curposition", 0);
                musicMedia = musiclist.get(curposition);
                Log.i(TAG, url + "......." + Thread.currentThread().getName());
                palyer();
            }else if(MSG.equals("1")){
                mediaPlayer.pause();
            }else if(MSG.equals("2")){
                mediaPlayer.start();
            }

            String name = "Current: "+ url.substring(url.lastIndexOf("/") + 1 , url.lastIndexOf("."));
            Log.i(TAG,name);
//        //开启前台service
            Notification notification;

            if (Build.VERSION.SDK_INT < 16) {
                notification = new Notification.Builder(this,CHANNEL_ID)
                        .setContentTitle("Enter the MusicPlayer").setContentText(name)
                        .setSmallIcon(R.drawable.musicfile).getNotification();
            } else {
                Notification.Builder builder = new Notification.Builder(this,CHANNEL_ID);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, MusicActivity.class), 0);
                builder.setContentIntent(contentIntent);
                builder.setSmallIcon(R.drawable.musicfile);
                builder.setContentTitle("Enter the MusicPlayer");
                builder.setContentText(name);
                notification = builder.build();
            }




            startForeground(NOTIFICATION_ID, notification);
        }


        return super.onStartCommand(intent, flags, startId);
    }


    private void palyer() {
        Log.i(TAG,"palyer......");
        try {
            mediaPlayer.reset();

            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.start();
            musicMedia = musiclist.get(curposition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG,"onUnbind......");
        return super.onUnbind(intent);

    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG, "onRebind......");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy......");
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        //关闭线程
        Thread.currentThread().interrupt();
        stopForeground(true);

    }
    public String toTime(int time){
        time /= 1000;
        int minute = time / 60;
        int hour = minute / 60;
        int second = time % 60;
        minute %= 60;
        return String.format("%02d:%02d", minute, second);
    }
}
