package com.movilesunal.movichat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.movilesunal.movichat.R;
import com.movilesunal.movichat.model.Message;

import java.sql.Time;
import java.util.Calendar;
import java.util.LinkedList;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout lytMessages;
    private NestedScrollView sclMessages;
    private FirebaseUser user;
    private LinkedList<String> sending = new LinkedList<>();

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
                    if (message.getUser().equals(user.getDisplayName())) {
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
                    message.setUser(user.getDisplayName());
                    message.setText(text);
                    message.setHour(calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE));
                    addMessageToScreen(message, true);
                    addMessageToFirebase(message);
                }
            }
        });
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
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.action_report:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.report_bug);
                builder.setMessage("Describe con detalle el bug.");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                builder.setView(input);

                builder.setPositiveButton(R.string.report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseCrash.report(new Throwable(user.getDisplayName() + " dice: "
                                + input.getText().toString()));
                        Snackbar.make(getCurrentFocus(), "Tu reporte ha sido enviado", Snackbar.LENGTH_LONG).show();
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
        }
        TextView txtUser = (TextView) view.findViewById(R.id.txtUser);
        TextView txtText = (TextView) view.findViewById(R.id.txtText);
        TextView txtHour = (TextView) view.findViewById(R.id.txtHour);
        txtUser.setText(message.getUser());
        txtText.setText(message.getText());
        txtHour.setText(message.getHour());

        lytMessages.addView(view);
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
}
