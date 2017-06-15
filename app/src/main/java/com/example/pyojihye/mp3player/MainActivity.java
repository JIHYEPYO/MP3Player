package com.example.pyojihye.mp3player;

import android.content.Intent;
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

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, AdapterView.OnItemClickListener {
    private static final int REQUEST_RUNTIME_PERMISSION = 1;
    final static String TAG = "MainActivity";
    private final String MESSAGES_CHILD = "music";

    ListView listView;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference musicRef;

    ArrayList items;
    ArrayList<String> listFile;
    ArrayAdapter<String> adapter;
    MediaPlayer mMediaplayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Log.d(TAG, "onCreate() 실행");

        initListView();
        initStorage();

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

        mMediaplayer = new MediaPlayer();
        mMediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void dataSet() {
        for (int i = 1; i < items.size(); i++) {
            items.set(i, items.get(i).toString().replace("{name=", ""));
            items.set(i, items.get(i).toString().replace("}", ""));
            listFile.add(i-1, items.get(i).toString());
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchAudioUrlFromFirebase() {
        musicRef.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        try {
                            final String url = uri.toString();
                            mMediaplayer.setDataSource(url);
                            mMediaplayer.setOnPreparedListener(MainActivity.this);
                            onPrepared(mMediaplayer);
                            mMediaplayer.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
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

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    private final Handler httpHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage() 실행");
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: //HTTP SERVICE FAIL;
                    Toast.makeText(MainActivity.this, "Server Connection failed.", Toast.LENGTH_SHORT).show();

                    break;
                case 1: //HTTP SERVICE SUCCESS;
                    Intent intent = new Intent(MainActivity.this, MusicActivity.class);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "Server Connection success.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        musicRef = storageRef.child("music/" + items.get(position+1));
        fetchAudioUrlFromFirebase();
    }
}