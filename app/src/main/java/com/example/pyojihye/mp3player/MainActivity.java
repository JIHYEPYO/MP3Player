package com.example.pyojihye.mp3player;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final int REQUEST_RUNTIME_PERMISSION = 1;
    final static String TAG = "MainActivity";
    private final String MESSAGES_CHILD = "music";

    private ProgressBar mProgressBar;
    private ListView listView;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference musicRef;

    ArrayList items;
    ArrayList<String> listFile;
    ArrayAdapter<String> adapter;
    MediaPlayer mediaPlayer;
    boolean isPlay;
    String musicName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Log.d(TAG, "onCreate() 실행");
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        Toast.makeText(MainActivity.this,"Please Internet Connect!!!", Toast.LENGTH_SHORT).show();
        initListView();
        initStorage();
        isPlay = false;

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference(MESSAGES_CHILD);

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                items = (ArrayList) dataSnapshot.getValue();
                dataSet();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }

    public void initListView() {
//        Log.d(TAG, "initListView() 실행");

        listFile = new ArrayList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listFile);

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    public void initStorage() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://mp3player-37680.appspot.com");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void dataSet() {
        for (int i = 1; i < items.size(); i++) {
            items.set(i, items.get(i).toString().replace("{name=", ""));
            items.set(i, items.get(i).toString().replace("}", ""));
            listFile.add(i - 1, items.get(i).toString());
        }
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        adapter.notifyDataSetChanged();
    }

    private void fetchAudioUrlFromFirebase() {
        musicRef.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        if (!isPlay) {
                            isPlay = true;
                            final String url = uri.toString();
                            mediaPlayer = new MediaPlayer();
                            try {
                                mediaPlayer.setDataSource(url);
                                mediaPlayer.prepare();
                                mediaPlayer.start();
                                handler.sendEmptyMessage(1);
                            } catch (IOException e) {
                            }
                        } else {
                            isPlay = false;
                            handler.sendEmptyMessage(0);
                        }
                    }
                })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("TAG", e.getMessage());
                    }
                });
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage() 실행");
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: //Music Stop
                    Toast.makeText(MainActivity.this, "Music Stop", Toast.LENGTH_SHORT).show();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    break;
                case 1: //HTTP SERVICE SUCCESS;
                    Toast.makeText(MainActivity.this, "Play : " + musicName, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        musicRef = storageRef.child("music/" + items.get(position + 1));
        musicName = items.get(position + 1).toString();
        fetchAudioUrlFromFirebase();
    }
}