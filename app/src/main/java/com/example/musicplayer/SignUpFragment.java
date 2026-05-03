package com.example.musicplayer;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class SignUpFragment extends Fragment {

    EditText etEmail,etPassword,etCpassword;
    Button btnSignup;
    SharedPreferences srPref;
    FirebaseAuth auth;
    TextView tvSignin;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        etEmail=view.findViewById(R.id.etEmail);
        etPassword=view.findViewById(R.id.etPassword);
        etCpassword=view.findViewById(R.id.etCpassword);
        btnSignup=view.findViewById(R.id.btnSignUp);
        auth= FirebaseAuth.getInstance();
        tvSignin = view.findViewById(R.id.tvSignin);
        srPref= requireActivity().getSharedPreferences("MyPref",MODE_PRIVATE);

        btnSignup.setOnClickListener((v->{
            String email,password,cpassword;
            email=etEmail.getText().toString().trim();
            password=etPassword.getText().toString().trim();
            cpassword=etCpassword.getText().toString().trim();

            if(email.isEmpty()||password.isEmpty()||cpassword.isEmpty()){
                Toast.makeText(requireContext(), R.string.all_fields_filled, Toast.LENGTH_SHORT).show();
                return;
           }
            if(password.equals(cpassword)){
                auth.createUserWithEmailAndPassword(email,password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            String userEmail = firebaseUser.getEmail();
                            MyApplication.initHandlers(uid);
                            String name = userEmail != null ? userEmail.split("@")[0] : "User";
                            User newUser = new User(uid, name, userEmail, "");
                            if (MyApplication.userHandler != null) {
                                MyApplication.userHandler.saveUser(newUser);
                            }
                        }
                        sendEmailVerification(authResult);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                Toast.makeText(requireContext(), R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
                return;
            }
        }));

        tvSignin.setOnClickListener(v1 -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SigninFragment())
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void sendEmailVerification(AuthResult authResult) {
        if (authResult.getUser() != null) {
            authResult.getUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new VerifyEmailFragment())
                            .addToBackStack(null)
                            .commit();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }
}
