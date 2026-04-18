package com.example.myapplication;

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


public class SigninFragment extends Fragment {
    EditText etEmail,etPassword;
    Button btnLogin;
    SharedPreferences srPref;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        etEmail=view.findViewById(R.id.etEmail);
        etPassword=view.findViewById(R.id.etPassword);
        btnLogin=view.findViewById(R.id.btnLogin);
        TextView tvCreateAccount = view.findViewById(R.id.tvcreateAccount);
        srPref= requireActivity().getSharedPreferences("MyPref",MODE_PRIVATE);
        btnLogin.setOnClickListener((v->{
            String email,password;
            email=etEmail.getText().toString().trim();
            password=etPassword.getText().toString().trim(); //idk if supposed to be trimmed or not


            if(email.isEmpty()||password.isEmpty()){
                Toast.makeText(requireContext(),"All fields must be filled",Toast.LENGTH_SHORT).show();
                return;
            }
            if(!password.equals(srPref.getString("password",""))){
                Toast.makeText(requireContext(),"Invalid Username/Password",Toast.LENGTH_SHORT).show();
                return;
            }
            srPref.edit().putBoolean("is_loggedin",true).commit();
            Intent i =new Intent(requireContext(), MainActivity.class);
            startActivity(i);



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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signin, container, false);
    }
}