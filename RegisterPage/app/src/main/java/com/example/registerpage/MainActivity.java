package com.example.registerpage;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button logoutButton, getStartedButton, viewRecommendationsButton;
    TextView textView;
    FirebaseUser user;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        logoutButton = findViewById(R.id.logout_button);
        getStartedButton = findViewById(R.id.btn_take_picture);
        viewRecommendationsButton = findViewById(R.id.view_recommendations_button);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), LogIn.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText(user.getEmail());
        }

        logoutButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), LogIn.class);
            startActivity(intent);
            finish();
        });

        getStartedButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, TomatoScanActivity.class);
            startActivity(intent);
        });

        viewRecommendationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, InformationActivity.class);
            startActivity(intent);
        });
    }
}
