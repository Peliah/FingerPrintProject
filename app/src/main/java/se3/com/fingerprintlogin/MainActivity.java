package se3.com.fingerprintlogin;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 101010;

    private static final String KEY_NAME = "my_key";
    ImageView fingerprintLogin;

    Button btnLogin;
    private TextInputEditText editTextName;
    private TextInputEditText editTextPassword;

    private FirebaseAuth mAuth;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        TextView txtSignUp = findViewById(R.id.txtSignUp);
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Logging In...");
        progressDialog.setCancelable(false);


        txtSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnLogin=findViewById(R.id.btnSignIn);
        editTextName = findViewById(R.id.edtSignInName);
        editTextPassword = findViewById(R.id.edtSignInPassword);

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Cannot find Biometric Scanner", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric Scanner Busy or Unavailable", Toast.LENGTH_SHORT).show();

                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // Prompts the user to create credentials that your app accepts.
                final Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
                startActivityForResult(enrollIntent, REQUEST_CODE);
                break;
        }

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                                "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build();


        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                String email = editTextName.getText().toString();
                String password = editTextPassword.getText().toString();
                if (TextUtils.isEmpty(email)) {
                    // Show an error message or handle the case when email or password is empty
                    progressDialog.dismiss();
                    editTextName.setError("Email required!");
                    editTextName.requestFocus();
                    Toast.makeText(MainActivity.this, "Please enter valid email", Toast.LENGTH_SHORT).show();
                    return;
                }else if(TextUtils.isEmpty(password)){
                    progressDialog.dismiss();
                    editTextPassword.setError("Password required!");
                    editTextPassword.requestFocus();
                    Toast.makeText(MainActivity.this, "Please enter valid password", Toast.LENGTH_SHORT).show();
                    return;
                }

                //loginUser(email, password);
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    String userId = user.getUid();

                                    DocumentReference userRef = FirebaseFirestore.getInstance()
                                            .collection("Registered Users")
                                            .document(userId);
                                    userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()){
                                                DocumentSnapshot documentSnapshot = task.getResult();
                                                if (documentSnapshot.exists()){
                                                    UserDetails loggedInUser = documentSnapshot.toObject(UserDetails.class);
                                                    //Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                                    biometricPrompt.authenticate(promptInfo);
                                                    Toast.makeText(MainActivity.this, "Logged In successfully", Toast.LENGTH_SHORT).show();
                                                    progressDialog.dismiss();


                                                }else{
                                                    progressDialog.dismiss();
                                                    Toast.makeText(MainActivity.this, "User document not found", Toast.LENGTH_SHORT).show();
                                                }
                                            }else {
                                                progressDialog.dismiss();
                                                String errorMessage = task.getException().getMessage();
                                                Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();

                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            progressDialog.dismiss();
                                            if (exception instanceof FirebaseAuthInvalidUserException) {
                                                Toast.makeText(MainActivity.this, "Invalid user", Toast.LENGTH_SHORT).show();
                                            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                                Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                            } else if (exception instanceof FirebaseAuthException) {
                                                Toast.makeText(MainActivity.this, "Firebase authentication failed", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(MainActivity.this, "Login failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                } else {
                                    progressDialog.dismiss();
                                    String errorMessage = task.getException().getMessage();
                                    Toast.makeText(MainActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
//                biometricPrompt.authenticate(promptInfo);
            }
        });

    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            String userId = user.getUid();

                            DocumentReference userRef = FirebaseFirestore.getInstance()
                                    .collection("Registered Users")
                                    .document(userId);
                            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()){
                                        DocumentSnapshot documentSnapshot = task.getResult();
                                        if (documentSnapshot.exists()){
                                            UserDetails loggedInUser = documentSnapshot.toObject(UserDetails.class);
                                            //Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                            Toast.makeText(MainActivity.this, "Logged In successfully", Toast.LENGTH_SHORT).show();

                                        }else{
                                            Toast.makeText(MainActivity.this, "User document not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }else {
                                        String errorMessage = task.getException().getMessage();
                                        Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });
                        } else {
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(MainActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}