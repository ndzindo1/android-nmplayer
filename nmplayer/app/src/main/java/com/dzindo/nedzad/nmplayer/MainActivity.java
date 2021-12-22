package com.dzindo.nedzad.nmplayer;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements Runnable {

    private static final int MY_PERMISSION_REQUEST=1;

    Button ppause,stop,sljedeca,prev;
    MediaPlayer mp;
    SeekBar vol =null,pjesma=null;
    AudioManager audiom=null;
    private Handler mHandler = new Handler();
    Integer trenutna=0;
    ArrayList<String> a,putanje;
    TextView nazivpjesme;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSION_REQUEST);
            }
            else{
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSION_REQUEST);
            }
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        initControls();

        a= new ArrayList<String>();
        putanje = new ArrayList<String>();
        //Dobavljanje plejliste
        getMusic();


        Toast.makeText(this,putanje.get(0)+"___"+a.get(0) , Toast.LENGTH_SHORT).show();

        nazivpjesme = findViewById(R.id.nazivP) ;
        ppause = findViewById(R.id.play);
        stop = findViewById(R.id.stop);
        sljedeca = findViewById(R.id.nexts);
        prev = findViewById(R.id.prevs);
        final ListView listaP = findViewById(R.id.dynamic);


        listaP.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,a);
        listaP.setAdapter(aa);

        pjesma = findViewById(R.id.seekBars);



        final TextView pjesmahint = findViewById(R.id.phint);
        pjesmahint.setVisibility(View.INVISIBLE);

        pjesma.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
               pjesmahint.setVisibility(View.VISIBLE);
                int x = (int) Math.ceil(i / 1000f);

                if (x < 10)
                    pjesmahint.setText("0:0" + x);
                else if( x<60 )
                    pjesmahint.setText("0:" + x);
                else{
                    DecimalFormat df = new DecimalFormat("00");
                    pjesmahint.setText(Integer.toString(x/60)+":"+df.format(x%60));
                }

                double percent = i / (double) seekBar.getMax();
                int offset = seekBar.getThumbOffset();
                int seekWidth = seekBar.getWidth();
                int val = (int) Math.round(percent * (seekWidth - 2 * offset));
                int labelWidth = pjesmahint.getWidth();
                pjesmahint.setX(offset + seekBar.getX() + val
                        - Math.round(percent * offset)
                        - Math.round(percent * labelWidth / 2));


                if(pjesma.getProgress()==mp.getDuration()){
                    sljedeca(1);}
                if (i > 0 && mp != null && !mp.isPlaying()) {
                   pjesma.setProgress(0);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pjesmahint.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mp  != null && mp.isPlaying()) {
                    mp.seekTo(seekBar.getProgress());
                }
            }
        });


        ppause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mp!=null){
                    if(!mp.isPlaying()){
                        pustiPjesmu();
                    }
                    else{
                        mp.pause();
                        pjesma.setProgress(pjesma.getProgress());
                        ppause.setBackgroundResource(R.drawable.playbutton);
                    }
                }
                else Toast.makeText(MainActivity.this, "Odaberite jednu od pjesama sa liste!", Toast.LENGTH_SHORT).show();

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp.stop();
                mp=MediaPlayer.create(getBaseContext(),Uri.parse(putanje.get(trenutna)));
                pjesma.setMax(mp.getDuration());
                pjesma.setProgress(0);
                ppause.setBackgroundResource(R.drawable.playbutton);
                Toast.makeText(MainActivity.this,Boolean.toString(mp.isPlaying()) , Toast.LENGTH_SHORT).show();
            }
        });

        listaP.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
      /*          for (int j = 0; j < adapterView.getChildCount(); j++)
                    adapterView.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);

                // change the background color of the selected element
                view.setBackgroundColor(Color.LTGRAY);
*/

              if(mp!=null)  mp.stop();
                try {
                    mp=MediaPlayer.create(getBaseContext(),Uri.parse(putanje.get(i)));
                    trenutna=i;

                }catch (Exception e){
                    Toast.makeText(MainActivity.this,Integer.toString(i), Toast.LENGTH_SHORT).show();
                    }
                mp.setLooping(false);
                pjesma.setMax(mp.getDuration());
                pjesma.setProgress(0);

              pustiPjesmu();

            }



        });

        sljedeca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sljedeca(1);
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sljedeca(-1);
            }
        });
    }

    private void sljedeca(int i) {
        if(mp!=null)  mp.stop();
        if(i==1){
            if(trenutna==a.size()-1){ trenutna=0;}
            else trenutna++;
        }
        else {
            if(trenutna==0){trenutna=a.size()-1;}
            else trenutna--;
        }
        try {
            mp=MediaPlayer.create(getBaseContext(),Uri.parse(putanje.get(trenutna)));


        }catch (Exception e){

        }
        mp.setLooping(false);
        pjesma.setMax(mp.getDuration());
        pjesma.setProgress(0);

        pustiPjesmu();
    }


    private void pustiPjesmu() {

        if(mp==null){
            Toast.makeText(this, "Odaberite jednu od pjesma sa liste!", Toast.LENGTH_SHORT).show();
        }
        else{
            nazivpjesme.setText(a.get(trenutna));
        }
        mp.start();
        new Thread(this).start();
        ppause.setBackgroundResource(R.drawable.pause1);

    }

    void initControls(){
        vol = findViewById(R.id.seekBarvol);
        audiom= (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vol.setMax(audiom.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        vol.setProgress(audiom.getStreamVolume(AudioManager.STREAM_MUSIC));
        vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                audiom.setStreamVolume(AudioManager.STREAM_MUSIC,i,0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        {
            int index = vol.getProgress();
            vol.setProgress(index + 1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            int index = vol.getProgress();
            vol.setProgress(index - 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void run() {
        int currentPosition = mp.getCurrentPosition();
        int total = mp.getDuration();

        while (mp != null && mp.isPlaying() && currentPosition < total) {
            try {
                Thread.sleep(1000);
                currentPosition = mp.getCurrentPosition();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                return;
            }

            pjesma.setProgress(currentPosition);

        }

    }





    public void getMusic(){

        ContentResolver cr=getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = cr.query(songUri,null,null,null,null);

        if(songCursor!=null && songCursor.moveToFirst()){
            int sTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int sPath = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do{
                String stitle = songCursor.getString(sTitle);
                String sdata = songCursor.getString(sPath);
                a.add(stitle);
                putanje.add(sdata);
            }while(songCursor.moveToNext());
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST :{
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Dozvoljeno!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
