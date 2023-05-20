package se3.com.fingerprintlogin;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private static final int REQUEST_CODE = 101010;
    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextNumber;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private Button buttonSignUp;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        TextView txtSignIn = findViewById(R.id.txtSignIn);
        progressDialog = new ProgressDialog(SignUpActivity.this);
        progressDialog.setMessage("Signing up...");
        progressDialog.setCancelable(false);


        txtSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        editTextName = findViewById(R.id.edtSignUpName);
        editTextEmail = findViewById(R.id.edtSignUpEmail);
        editTextPassword = findViewById(R.id.edtSignUpPassword);
        editTextNumber = findViewById(R.id.edtSignUpMobile);
        firebaseAuth = FirebaseAuth.getInstance();
        buttonSignUp = findViewById(R.id.btnSignUp);





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
        biometricPrompt = new BiometricPrompt(SignUpActivity.this,
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
                Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
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

        // Prompt appears when user clicks "Log in".
        // Consider integrating with the keystore to unlock cryptographic operations,
        // if needed by your app.

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();

                //Obtain enetered data
                String textFullName = editTextName.getText().toString();
                String textEmail = editTextEmail.getText().toString();
                String textPassword = editTextPassword.getText().toString();
                String textNumber = editTextNumber.getText().toString();
                if(isValidDetails(textFullName, textEmail, textNumber, textPassword)){
                    //registerUser(textFullName, textEmail, textNumber, textPassword);
                    firebaseAuth.createUserWithEmailAndPassword(textEmail, textPassword).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(SignUpActivity.this, textFullName+" is successfully registered", Toast.LENGTH_LONG).show();
                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                UserDetails userDetails = new UserDetails(textFullName, textEmail, textNumber);
                                String userId = firebaseUser.getUid();
                                DocumentReference databaseReference = FirebaseFirestore.getInstance().collection("Registered Users").document(userId);
                                databaseReference.set(userDetails)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                biometricPrompt.authenticate(promptInfo);
                                                Toast.makeText(SignUpActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                                progressDialog.dismiss();

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                String errorMessage = e.getMessage();
                                                // Handle the error according to your requirements
                                                Toast.makeText(SignUpActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                                                progressDialog.dismiss();

                                            }
                                        });

                            }else
                            {
                                try {
                                    throw task.getException();
                                } catch (FirebaseAuthWeakPasswordException weakPasswordException) {
                                    // Handle weak password error
                                    String errorCode = weakPasswordException.getErrorCode();
                                    String errorMessage = weakPasswordException.getMessage();
                                    progressDialog.dismiss();
                                    Toast.makeText(SignUpActivity.this, errorCode+" "+errorMessage, Toast.LENGTH_SHORT).show();

                                    // Handle the error according to your requirements
                                } catch (FirebaseAuthInvalidCredentialsException invalidCredentialsException) {
                                    // Handle invalid email error
                                    String errorCode = invalidCredentialsException.getErrorCode();
                                    String errorMessage = invalidCredentialsException.getMessage();
                                    progressDialog.dismiss();
                                    Toast.makeText(SignUpActivity.this, errorCode+" "+errorMessage, Toast.LENGTH_SHORT).show();

                                    // Handle the error according to your requirements
                                } catch (FirebaseAuthUserCollisionException userCollisionException) {
                                    // Handle user collision error (e.g., email already exists)
                                    String errorCode = userCollisionException.getErrorCode();
                                    String errorMessage = userCollisionException.getMessage();
                                    progressDialog.dismiss();
                                    Toast.makeText(SignUpActivity.this, errorCode+" "+errorMessage, Toast.LENGTH_SHORT).show();

                                    // Handle the error according to your requirements
                                } catch (Exception e) {
                                    // Handle other errors
                                    String errorMessage = e.getMessage();
                                    // Handle the error according to your requirements
                                    progressDialog.dismiss();
                                    Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

                                }
                                progressDialog.dismiss();
                                Toast.makeText(SignUpActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                            }
//                    Toast.makeText(SignUpActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                        }
                    });
//                    biometricPrompt.authenticate(promptInfo);
                }
            }
        });

    }

    private boolean isValidDetails(String name, String email, String number, String password) {

        if (TextUtils.isEmpty(name)){
            progressDialog.dismiss();

            editTextName.setError("Full name required!");
            editTextName.requestFocus();
            return false;
        }else if(TextUtils.isEmpty(email)){
            progressDialog.dismiss();

            editTextEmail.setError("Email required!");
            editTextEmail.requestFocus();
            return false;

        }else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            progressDialog.dismiss();

            editTextEmail.setError("Valid email required!");
            editTextEmail.requestFocus();
            return false;

        }else if(TextUtils.isEmpty(number)){
            progressDialog.dismiss();

            editTextNumber.setError("Phone number required!");
            editTextNumber.requestFocus();
            return false;

        }else if(number.length() !=9 ){
            progressDialog.dismiss();

            editTextNumber.setError("Valid Phone number required!");
            editTextNumber.requestFocus();
            return false;

        }else if(TextUtils.isEmpty(password)){
            progressDialog.dismiss();

            editTextPassword.setError("Password required!");
            editTextPassword.requestFocus();
            return false;

        }
        return true;

    }

    private void registerUser(String name, String email, String number, String password){

//        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
//            @Override
//            public void onComplete(@NonNull Task<AuthResult> task) {
//                if (task.isSuccessful()){
//                    Toast.makeText(SignUpActivity.this, name+" successfully registered", Toast.LENGTH_LONG).show();
//                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
////                    UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
////                    firebaseUser.updateProfile(profileChangeRequest);
//                    UserDetails userDetails = new UserDetails(name, email, number);
//                    String userId = firebaseUser.getUid();
//                    DocumentReference databaseReference = FirebaseFirestore.getInstance().collection("Registered Users").document(userId);
//                    databaseReference.set(userDetails)
//                            .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                @Override
//                                public void onSuccess(Void unused) {
//                                    Toast.makeText(SignUpActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
//                                }
//                            }).addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception e) {
//                                    String errorMessage = e.getMessage();
//                                    // Handle the error according to your requirements
//                                    Toast.makeText(SignUpActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
//
//                                }
//                            });
//
//                }else
//                {
//                    try {
//                        throw task.getException();
//                    } catch (FirebaseAuthWeakPasswordException weakPasswordException) {
//                        // Handle weak password error
//                        String errorCode = weakPasswordException.getErrorCode();
//                        String errorMessage = weakPasswordException.getMessage();
//                        Toast.makeText(SignUpActivity.this, errorCode+" "+errorMessage, Toast.LENGTH_SHORT).show();
//
//                        // Handle the error according to your requirements
//                    } catch (FirebaseAuthInvalidCredentialsException invalidCredentialsException) {
//                        // Handle invalid email error
//                        String errorCode = invalidCredentialsException.getErrorCode();
//                        String errorMessage = invalidCredentialsException.getMessage();
//                        Toast.makeText(SignUpActivity.this, errorCode+" "+errorMessage, Toast.LENGTH_SHORT).show();
//
//                        // Handle the error according to your requirements
//                    } catch (FirebaseAuthUserCollisionException userCollisionException) {
//                        // Handle user collision error (e.g., email already exists)
//                        String errorCode = userCollisionException.getErrorCode();
//                        String errorMessage = userCollisionException.getMessage();
//                        Toast.makeText(SignUpActivity.this, errorCode+" "+errorMessage, Toast.LENGTH_SHORT).show();
//
//                        // Handle the error according to your requirements
//                    } catch (Exception e) {
//                        // Handle other errors
//                        String errorMessage = e.getMessage();
//                        // Handle the error according to your requirements
//                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
//
//                    }
//
//                    Toast.makeText(SignUpActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
//                }
////                    Toast.makeText(SignUpActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
//                }
//        });
    }

}