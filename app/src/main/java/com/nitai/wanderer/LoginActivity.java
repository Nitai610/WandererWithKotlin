package com.nitai.wanderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
// This imports the Firebase Authentication library
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    MaterialButton btnLogin, btnBack;
    TextView tvRegisterLink;
    TextInputEditText etUsername, etPassword;

    // NOTE: Declaring the Firebase Authentication engine variable again for this screen.
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode (same as before)
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_login);

        // NOTE: Connecting Java variables to XML elements
        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.btnBack);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        // NOTE: Waking up the Firebase engine for the Login screen
        mAuth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // --- FIREBASE LOGIN LOGIC ---
        btnLogin.setOnClickListener(v -> {

            // NOTE: Grab what the user typed and remove accidental spaces
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // NOTE: Validation - Don't bother the Firebase server if the boxes are empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return; // Stop the code here
            }

            // NOTE: The magic login command! We ask Firebase to check if this email/password combination exists in our database.
            mAuth.signInWithEmailAndPassword(email, password)
                    // NOTE: Wait patiently for the server to check the database...
                    .addOnCompleteListener(this, task -> {

                        // NOTE: Did the email and password match perfectly?
                        if (task.isSuccessful()) {
                            // NOTE: Success! The user is now authenticated.
                            Toast.makeText(LoginActivity.this, "Welcome Back!", Toast.LENGTH_SHORT).show();

                            // Move them to the main dashboard
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);

                            // Destroy the login screen so pressing the phone's back arrow doesn't log them out
                            finish();
                        } else {
                            // NOTE: Failed! They either typed the wrong password, or the account doesn't exist.
                            // We use a generic error message here so hackers don't know if they guessed a correct email or not.
                            Toast.makeText(LoginActivity.this, "Login Failed. Check credentials.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}