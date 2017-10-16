package com.movilesunal.movichat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.movilesunal.movichat.R;
import com.movilesunal.movichat.model.Message;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout lytMessages;
    private NestedScrollView sclMessages;
    private FirebaseUser user;
    private LinkedList<String> sending = new LinkedList<>();
    private AtomicInteger msgId = new AtomicInteger();
    private GoogleApiClient mGoogleApiClient;
    private String uidLastMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        sclMessages = (NestedScrollView) findViewById(R.id.sclMessages);
        lytMessages = (LinearLayout) findViewById(R.id.lytMessages);
        final EditText edtMessage = (EditText) findViewById(R.id.edtMessage);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        user = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseDatabase.getInstance().getReference().child("Room").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                if (!sending.contains(dataSnapshot.getKey())) {
                    if (message.getUid().equals(user.getUid())) {
                        addMessageToScreen(message, true);
                    } else {
                        addMessageToScreen(message, false);
                    }
                } else {
                    sending.remove(dataSnapshot.getKey());
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        findViewById(R.id.fabSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!edtMessage.getText().toString().isEmpty()) {
                    String text = edtMessage.getText().toString();
                    edtMessage.setText("");
                    Calendar calendar = Calendar.getInstance();

                    Message message = new Message();
                    message.setName(user.getDisplayName());
                    message.setUid(user.getUid());
                    message.setText(text);
                    message.setHour(calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE));
                    addMessageToScreen(message, true);
                    addMessageToFirebase(message);
                }
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(ChatActivity.this)
                .enableAutoManage(ChatActivity.this,
                        new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                                Snackbar.make(getCurrentFocus(),
                                        R.string.cannot_connect_api_google,
                                        Snackbar.LENGTH_LONG).show();
                            }
                        })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        FirebaseMessaging.getInstance().subscribeToTopic("MoviChat");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                if (mGoogleApiClient.isConnected()) {
                    FirebaseAuth.getInstance().signOut();

                    Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                    finish();
                    startActivity(new Intent(this, LoginActivity.class));
                } else {
                    Snackbar.make(getCurrentFocus(), R.string.cannot_connect_api_google, Snackbar.LENGTH_LONG).show();
                }
                break;
            case R.id.action_report:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.report_bug);
                builder.setMessage(R.string.describe_bug);
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                builder.setView(input);

                builder.setPositiveButton(R.string.report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseCrash.report(new Throwable(user.getDisplayName() + " dice: "
                                + input.getText().toString()));
                        Snackbar.make(getCurrentFocus(), R.string.sended_bug, Snackbar.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addMessageToScreen(Message message, boolean ownMessage) {
        View view;
        if (ownMessage) {
            view = getLayoutInflater().inflate(R.layout.own_message, lytMessages, false);
        } else {
            view = getLayoutInflater().inflate(R.layout.other_message, lytMessages, false);
            if (!uidLastMessage.equals(message.getUid())) {
                downloadImage(((ImageView) view.findViewById(R.id.imgUser)), "Users/" + message.getUid());
            } else {
                ((ImageView) view.findViewById(R.id.imgUser)).setImageDrawable(null);
            }
        }
        TextView txtUser = (TextView) view.findViewById(R.id.txtUser);
        TextView txtText = (TextView) view.findViewById(R.id.txtText);
        TextView txtHour = (TextView) view.findViewById(R.id.txtHour);
        txtUser.setText(message.getName());
        txtText.setText(message.getText());
        txtHour.setText(message.getHour());

        lytMessages.addView(view);
        uidLastMessage = message.getUid();
        sclMessages.post(new Runnable() {
            @Override
            public void run() {
                sclMessages.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void addMessageToFirebase(Message message) {
        String key = FirebaseDatabase.getInstance().getReference().child("Room").push().getKey();
        sending.add(key);
        FirebaseDatabase.getInstance().getReference().child("Room").child(key).setValue(message);
    }

    public void downloadImage(ImageView img, String path) {
        StorageReference ref = FirebaseStorage.getInstance().getReference();
        ref = ref.child(path);
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(ref)
                .into(img);
    }
}