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
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;


public class LoginFragment extends Fragment {
    TextInputEditText tietemail,tietpassword;
    Button btnLogin;
    SharedPreferences srPref;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tietemail=view.findViewById(R.id.tietemail);
        tietpassword=view.findViewById(R.id.tietpassword);
        btnLogin=view.findViewById(R.id.btnLogin);
        srPref= requireActivity().getSharedPreferences("MyPref",MODE_PRIVATE);
        btnLogin.setOnClickListener((v->{
            String email,password,cpassword;
            email=tietemail.getText().toString().trim();
            password=tietpassword.getText().toString().trim(); //idk if supposed to be trimmed or not


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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }
}