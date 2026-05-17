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
// This imports the Firebase Authentication library we just added!
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    MaterialButton btnBack, btnRegisterSubmit;
    TextView tvLoginLink;
    TextInputEditText etRegisterUsername, etRegisterPassword, etConfirmPassword;

    // NOTE: This creates a variable to hold the Firebase Authentication engine.
    // Think of mAuth as your app's personal security guard that talks to the cloud.
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // NOTE: Immersive Mode - This hides the battery and notification bars at the top of the phone
        // to make your app look like a full-screen modern application.
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        setContentView(R.layout.activity_register);

        // NOTE: Connecting our Java variables to the visual elements in the XML design.
        btnBack = findViewById(R.id.btnBack);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // NOTE: This wakes up the Firebase security guard so it is ready to accept commands.
        mAuth = FirebaseAuth.getInstance();

        // NOTE: Simple navigation buttons. 'finish()' simply closes this screen and goes back.
        btnBack.setOnClickListener(v -> finish());
        tvLoginLink.setOnClickListener(v -> finish());

        // --- FIREBASE REGISTRATION LOGIC ---
        btnRegisterSubmit.setOnClickListener(v -> {

            // NOTE: .trim() is very important! It cuts off any accidental spaces
            // the user might have typed at the beginning or end of their email/password.
            String email = etRegisterUsername.getText().toString().trim();
            String password = etRegisterPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // NOTE: Bagrut Validation 1 - Did they leave any box completely empty?
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return; // Stops the code here so it doesn't try to send blank info to Firebase
            }

            // NOTE: Bagrut Validation 2 - Did they make a typo in their password?
            if (!password.equals(confirmPassword)) {
                Toast.makeText(RegisterActivity.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            // NOTE: Bagrut Validation 3 - Firebase requires passwords to be at least 6 characters long!
            if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // NOTE: This is the actual magic command. We hand the email and password to the Firebase engine.
            // It sends it to Google's servers in the background.
            mAuth.createUserWithEmailAndPassword(email, password)
                    // NOTE: This "Listener" waits patiently for Google's servers to reply.
                    .addOnCompleteListener(this, task -> {

                        // NOTE: 'task.isSuccessful()' asks Firebase: "Did it work?"
                        if (task.isSuccessful()) {
                            // NOTE: If yes, the user is now registered and logged in simultaneously!
                            Toast.makeText(RegisterActivity.this, "Account created!", Toast.LENGTH_SHORT).show();

                            // Move them directly to the main menu
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);

                            // Close the register screen so they can't press the back button to return here
                            finish();
                        } else {
                            // NOTE: If it failed (e.g., they typed an invalid email format, or the email is already taken),
                            // Firebase sends back an error message. 'task.getException().getMessage()' grabs that exact error text to show the user.
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}