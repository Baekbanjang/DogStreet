package com.dog_street;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import org.checkerframework.checker.nullness.qual.NonNull;


public class activity_login extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogIn;
    private Button buttonSignUp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        firebaseAuth = FirebaseAuth.getInstance(); // Firebase 인증 객체 초기화

        editTextEmail = (EditText) findViewById(R.id.edittext_email);
        editTextPassword = (EditText) findViewById(R.id.edittext_password);
        buttonSignUp = (Button) findViewById(R.id.btn_signup);

        //회원가입 버튼 클릭 시 메소드
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 회원가입 버튼 클릭 시 회원가입 화면으로 이동
                Intent intent = new Intent(activity_login.this, report_edit.class);
                startActivity(intent);
            }
        });

        buttonLogIn = (Button) findViewById(R.id.btn_login);
        buttonLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //이메일과 비밀번호가 입력된 경우 loginUser 메소드 호출
                if (!editTextEmail.getText().toString().equals("") && !editTextPassword.getText().toString().equals("")) {
                    loginUser(editTextEmail.getText().toString(), editTextPassword.getText().toString());
                } else {
                    //이메일 또는 비밀번호가 누락 시 아래 메시지 표시
                    Toast.makeText(activity_login.this, "계정과 비밀번호를 입력하세요.", Toast.LENGTH_LONG).show();
                }
            }
        });


        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // 현재 로그인된 사용자의 정보를 가져옴
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //사용자 로그인 경우 운전자/보행자 선택 화면 이동 예정(다음 화면 저장 시 수정 예정)
                    Intent intent = new Intent(activity_login.this,mode_choice.class);
                    startActivity(intent);
                    finish();
                } else {
                }
            }
        };
    }

    //사용자 로그인 처리할 때 메시지 알림 메소드
    public void loginUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //로그인 성공 시 아래 메시지 표시
                            Toast.makeText(activity_login.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                            firebaseAuth.addAuthStateListener(firebaseAuthListener);
                        } else {
                            //이메일 또는 비밀번호 실패 시 아래 메시지 표시
                            Toast.makeText(activity_login.this, "이메일 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}