package com.example.habittracker.firebase;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Класс для управления аутентификацией Firebase
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static FirebaseAuthManager instance;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private AuthStateListener authStateListener;

    public interface AuthStateListener {
        void onUserAuthenticated(FirebaseUser user);
        void onUserNotAuthenticated();
    }

    private FirebaseAuthManager() {
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        // Добавляем логирование статуса аутентификации
        Log.d(TAG, "Initialization: Current user is " + (currentUser != null ? "authenticated" : "not authenticated"));
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    public void setAuthStateListener(AuthStateListener listener) {
        this.authStateListener = listener;
        if (currentUser != null) {
            Log.d(TAG, "Auth state listener set, user is authenticated");
            listener.onUserAuthenticated(currentUser);
        } else {
            Log.d(TAG, "Auth state listener set, user is not authenticated");
            listener.onUserNotAuthenticated();
        }
    }

    /**
     * Регистрация анонимного пользователя
     */
    public void signInAnonymously(Context context) {
        Log.d(TAG, "Attempting anonymous sign-in...");
        
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser = auth.getCurrentUser();
                        Log.d(TAG, "Anonymous sign-in successful. User ID: " + currentUser.getUid());
                        
                        if (authStateListener != null) {
                            authStateListener.onUserAuthenticated(currentUser);
                        }
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Anonymous sign-in failed: " + errorMessage, task.getException());
                        
                        // Показываем более детальное сообщение об ошибке
                        Toast.makeText(context, 
                                "Ошибка аутентификации: " + errorMessage, 
                                Toast.LENGTH_LONG).show();
                        
                        if (authStateListener != null) {
                            authStateListener.onUserNotAuthenticated();
                        }
                    }
                });
    }

    /**
     * Проверка, аутентифицирован ли пользователь
     */
    public boolean isUserAuthenticated() {
        boolean isAuth = currentUser != null;
        Log.d(TAG, "isUserAuthenticated() called, result: " + isAuth);
        return isAuth;
    }

    /**
     * Получение ID текущего пользователя
     */
    public String getCurrentUserId() {
        String uid = currentUser != null ? currentUser.getUid() : null;
        Log.d(TAG, "getCurrentUserId() called, result: " + uid);
        return uid;
    }

    /**
     * Выход из аккаунта
     */
    public void signOut() {
        Log.d(TAG, "signOut() called");
        auth.signOut();
        currentUser = null;
        if (authStateListener != null) {
            authStateListener.onUserNotAuthenticated();
        }
    }
} 