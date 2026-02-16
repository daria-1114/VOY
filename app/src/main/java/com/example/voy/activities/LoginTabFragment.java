package com.example.voy.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.voy.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;


public class LoginTabFragment extends Fragment {

    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login_tab, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        EditText emailEt = getView().findViewById(R.id.LogIn_email);
        EditText passwordEt = getView().findViewById(R.id.Login_password);

        auth = FirebaseAuth.getInstance();

        Button loginBtn = view.findViewById(R.id.login_btn);


        loginBtn.setOnClickListener(v->{
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(getContext(), "Fill all fields!", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task ->{
                        if(task.isSuccessful()){
                            startActivity(new Intent(getContext(), MainActivity.class));
                            requireActivity().finish();
                        }else{
                            Toast.makeText(getContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        });
    }
}