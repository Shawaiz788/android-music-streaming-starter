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


public class SigninFragment extends Fragment {
    EditText etEmail,etPassword;
    Button btnLogin;

    FirebaseAuth auth;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        auth=FirebaseAuth.getInstance();
        etEmail=view.findViewById(R.id.etEmail);
        etPassword=view.findViewById(R.id.etPassword);
        btnLogin=view.findViewById(R.id.btnLogin);
        TextView tvCreateAccount = view.findViewById(R.id.tvcreateAccount);

        btnLogin.setOnClickListener((v->{
            String email,password;
            email=etEmail.getText().toString().trim();
            password=etPassword.getText().toString().trim();


            if(email.isEmpty()||password.isEmpty()){
                Toast.makeText(requireContext(), R.string.all_fields_filled, Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email,password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        MyApplication.initHandlers(firebaseUser.getUid());
                    }
                    Toast.makeText(getContext(), R.string.signin_successful, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getActivity(), MainActivity.class));
                    requireActivity().finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(requireContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }));

        tvCreateAccount.setOnClickListener(v1 -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SignUpFragment())
                    .addToBackStack(null)
                    .commit();
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signin, container, false);
    }
}
