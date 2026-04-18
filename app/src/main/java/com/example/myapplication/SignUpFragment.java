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


public class SignUpFragment extends Fragment {

    EditText etEmail,etPassword,etCpassword;
    Button btnSignup;
    SharedPreferences srPref;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        etEmail=view.findViewById(R.id.etEmail);
        etPassword=view.findViewById(R.id.etPassword);
        etCpassword=view.findViewById(R.id.etCpassword);
        btnSignup=view.findViewById(R.id.btnSignUp);
        TextView tvSignin = view.findViewById(R.id.tvSignin);
        srPref= requireActivity().getSharedPreferences("MyPref",MODE_PRIVATE);
        btnSignup.setOnClickListener((v->{
            String email,password,cpassword;
            email=etEmail.getText().toString().trim();
            password=etPassword.getText().toString().trim(); //idk if supposed to be trimmed or not
            cpassword=etCpassword.getText().toString().trim();//idk if supposed to be trimmed or not

            if(email.isEmpty()||password.isEmpty()||cpassword.isEmpty()){
                Toast.makeText(requireContext(),"All fields must be filled",Toast.LENGTH_SHORT).show();
                return;
            }
            if(!password.equals(cpassword)){
                Toast.makeText(requireContext(),"Password and Verified Password are not equal",Toast.LENGTH_SHORT).show();
                return;
            }
            srPref.edit().putString("email",email).putString("password",password).putBoolean("is_loggedin",true).commit();
            Intent i =new Intent(requireContext(), MainActivity.class);
            startActivity(i);


        }));

        tvSignin.setOnClickListener(v1 -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SigninFragment())
                    .addToBackStack(null)
                    .commit();
        });

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }
}