package com.example.firebase;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class activity_signup extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextName;
    private EditText editTextPhoneNumber;
    private Button buttonJoin;
    private Button buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        firebaseAuth = FirebaseAuth.getInstance();

        editTextEmail = (EditText) findViewById(R.id.editText_email);
        editTextPassword = (EditText) findViewById(R.id.editText_passWord);
        editTextName = (EditText) findViewById(R.id.editText_name);
        editTextPhoneNumber = findViewById(R.id.editText_phone);

        buttonJoin = (Button) findViewById(R.id.btn_join);
        buttonBack = (Button) findViewById(R.id.btn_back);

        //  Join 버튼의 OnClickListener
        buttonJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();
                String name = editTextName.getText().toString();
                String phoneNumber = editTextPhoneNumber.getText().toString();

                if (!editTextEmail.getText().toString().equals("") && !editTextPassword.getText().toString().equals("")) {
                    //이메일과 비밀번호가 제공된 경우 createUser 메소드 호출
                    createUser(email, password, name, phoneNumber);
                    //다음 화면 넘기기 위한 코드
                    Intent intent = new Intent(activity_signup.this, activity_login.class);
                    startActivity(intent);
                } else {

                    // 이메일 또는 비밀번호 작성 안할 시 아래 메시지 호출
                    Toast.makeText(activity_signup.this, "계정과 비밀번호를 입력하세요.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Back 버튼의 OnClickListener
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // "Back 버튼 클릭 시 로그인 화면 전환
                Intent intent = new Intent(activity_signup.this, activity_login.class);
                startActivity(intent);
            }
        });
    }

    // 이메일과 비밀번호를 사용하여 새 사용자 생성
    private void createUser(String email, String password, String name, String phoneNumber) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            //생성 성공 시 아래 메시지 표시
                            Toast.makeText(activity_signup.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {

                            //생성 실패 시 아래 메시지 표시
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(activity_signup.this, "회원가입 실패: " + errorMessage, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }
}