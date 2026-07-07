package com.example.voy.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.voy.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class SignupTabFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText emailEt = getView().findViewById(R.id.signUp_email);
        EditText passwordEt = getView().findViewById(R.id.signup_password);
        EditText confirmPasswordEt = getView().findViewById(R.id.signup_confirm);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        Button signupBtn = view.findViewById(R.id.signup_btn);

        signupBtn.setOnClickListener(v->{
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();
            String confirmPassword = confirmPasswordEt.getText().toString().trim();
            if(email.isEmpty()||password.isEmpty()||confirmPassword.isEmpty()){
                Toast.makeText(getContext(),"Fill all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!password.equals(confirmPassword)){
                Toast.makeText(getContext(),"Passwords must match!", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            startActivity(new Intent(getContext(),MainActivity.class));
                            requireActivity().finish();
                        }else{
                            Toast.makeText(getContext(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}