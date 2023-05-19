package se3.com.fingerprintlogin;

import java.io.Serializable;

public class UserDetails implements Serializable {
    public String fullName, email, number;
    public UserDetails() {
        // Empty constructor required by Firebase Firestore
        // You can leave it empty or initialize any variables here
    }
    public UserDetails(String fullName, String email, String number) {
        this.fullName = fullName;
        this.email = email;
        this.number = number;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
