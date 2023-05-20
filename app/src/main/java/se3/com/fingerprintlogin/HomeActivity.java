package se3.com.fingerprintlogin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {
    private TextView textViewName, fullname;
    private TextView textViewEmail, emailB;
    private TextView textViewPhone;
    private Button btnLogout;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        textViewName = findViewById(R.id.fullname_profile);
        textViewEmail = findViewById(R.id.fullname_email);
        textViewPhone = findViewById(R.id.fullname_phone);
        fullname = findViewById(R.id.fullnameBig);
        emailB = findViewById(R.id.email);
        btnLogout = findViewById(R.id.btnSignOut);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null){
            String userId = currentUser.getUid();

            DocumentReference userRef = firebaseFirestore.collection("Registered Users").document(userId);
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()){
                            String email = document.getString("email");
                            String fullName = document.getString("fullName");
                            String phone = document.getString("number");

                            textViewName.setText(fullName);
                            fullname.setText(fullName);
                            textViewEmail.setText(email);
                            emailB.setText(email);

                        }
                    }
                }
            });
        }
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}