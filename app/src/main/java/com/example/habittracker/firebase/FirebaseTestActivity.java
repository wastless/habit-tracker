package com.example.habittracker.firebase;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habittracker.R;
import com.example.habittracker.data.database.Habit;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Активность для диагностики и проверки работы Firebase
 * Чтобы открыть эту активность, добавьте ее в AndroidManifest.xml
 * и добавьте кнопку для ее запуска в MainActivity
 */
public class FirebaseTestActivity extends AppCompatActivity {
    private static final String TAG = "FirebaseTestActivity";
    
    private TextView statusText;
    private Button testAuthButton;
    private Button testDatabaseButton;
    private Button testSyncButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_test);
        
        statusText = findViewById(R.id.statusText);
        testAuthButton = findViewById(R.id.testAuthButton);
        testDatabaseButton = findViewById(R.id.testDatabaseButton);
        testSyncButton = findViewById(R.id.testSyncButton);
        
        // Проверка состояния Firebase
        checkFirebaseStatus();
        
        // Настройка кнопок
        testAuthButton.setOnClickListener(v -> testAuthentication());
        testDatabaseButton.setOnClickListener(v -> testDatabase());
        testSyncButton.setOnClickListener(v -> testSync());
        
        // Дополнительно: долгое нажатие на кнопку проверки синхронизации для принудительной установки правил
        testSyncButton.setOnLongClickListener(v -> {
            forceSetDatabaseRules();
            return true;
        });
    }
    
    private void checkFirebaseStatus() {
        StringBuilder status = new StringBuilder("Firebase статус:\n");
        
        try {
            // Проверка инициализации
            FirebaseApp app = FirebaseApp.getInstance();
            status.append("✓ Firebase инициализирован\n");
            
            // Проверка конфигурации
            FirebaseOptions options = app.getOptions();
            status.append("Project ID: ").append(options.getProjectId()).append("\n");
            status.append("App ID: ").append(options.getApplicationId()).append("\n");
            status.append("API Key: ").append(options.getApiKey() != null ? "present" : "missing").append("\n");
            
            // Проверка аутентификации
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                status.append("✓ Пользователь аутентифицирован (").append(auth.getCurrentUser().getUid()).append(")\n");
            } else {
                status.append("✗ Пользователь не аутентифицирован\n");
            }
            
            // Проверка базы данных
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://habittracker-1bbd8-default-rtdb.europe-west1.firebasedatabase.app/");
            status.append("✓ База данных доступна: ").append(database.getReference().toString()).append("\n");
            
        } catch (Exception e) {
            status.append("✗ Ошибка: ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Firebase status check error", e);
        }
        
        statusText.setText(status.toString());
    }
    
    private void testAuthentication() {
        statusText.setText("Проверка аутентификации...");
        
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // Уже аутентифицирован
            String uid = auth.getCurrentUser().getUid();
            String status = "Уже аутентифицирован\nUID: " + uid;
            statusText.setText(status);
            Toast.makeText(this, "Аутентификация успешна", Toast.LENGTH_SHORT).show();
        } else {
            // Пробуем анонимную аутентификацию
            auth.signInAnonymously()
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                String uid = auth.getCurrentUser().getUid();
                                String status = "Аутентификация успешна\nUID: " + uid;
                                statusText.setText(status);
                                Toast.makeText(FirebaseTestActivity.this, 
                                        "Аутентификация успешна", Toast.LENGTH_SHORT).show();
                            } else {
                                String error = task.getException() != null ? 
                                        task.getException().getMessage() : "Unknown error";
                                String status = "Ошибка аутентификации: " + error;
                                statusText.setText(status);
                                Toast.makeText(FirebaseTestActivity.this, 
                                        "Ошибка аутентификации", Toast.LENGTH_SHORT).show();
                                
                                Log.e(TAG, "Authentication failed", task.getException());
                            }
                        }
                    });
        }
    }
    
    private void testDatabase() {
        statusText.setText("Проверка базы данных...");
        
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            statusText.setText("Ошибка: необходимо сначала пройти аутентификацию");
            Toast.makeText(this, 
                    "Сначала нажмите кнопку 'Проверить аутентификацию'", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Создаем тестовую запись
        String userId = auth.getCurrentUser().getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://habittracker-1bbd8-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference testRef = database.getReference("test").child(userId);
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("timestamp", new Date().getTime());
        testData.put("message", "Тестовая запись");
        
        testRef.setValue(testData)
                .addOnSuccessListener(aVoid -> {
                    // Теперь пробуем прочитать данные
                    testRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String status = "Запись и чтение из базы данных успешны\n" + 
                                        "Данные: " + snapshot.getValue();
                                statusText.setText(status);
                                Toast.makeText(FirebaseTestActivity.this, 
                                        "Проверка базы данных успешна", Toast.LENGTH_SHORT).show();
                            } else {
                                statusText.setText("Ошибка: данные не найдены");
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            String status = "Ошибка чтения из базы данных: " + 
                                    error.getMessage();
                            statusText.setText(status);
                            Log.e(TAG, "Database read error", error.toException());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    String status = "Ошибка записи в базу данных: " + e.getMessage();
                    statusText.setText(status);
                    Log.e(TAG, "Database write error", e);
                });
    }
    
    private void testSync() {
        statusText.setText("Проверка синхронизации...");
        
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            statusText.setText("Ошибка: необходимо сначала пройти аутентификацию");
            Toast.makeText(this, 
                    "Сначала нажмите кнопку 'Проверить аутентификацию'", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Создаем тестовую привычку
        Habit testHabit = new Habit(
                "Тестовая привычка " + new Date().getTime(), 
                "0,1,2"
        );
        
        // Получаем менеджер синхронизации
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance();
        
        // Пробуем синхронизировать привычку
        syncManager.uploadHabitToFirebase(testHabit);
        
        // Проверяем, сохранилась ли привычка в Firebase
        String userId = auth.getCurrentUser().getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://habittracker-1bbd8-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference habitsRef = database.getReference("habits").child(userId);
        
        habitsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    String status = "Синхронизация работает!\n" + 
                            "Найдено привычек в Firebase: " + snapshot.getChildrenCount();
                    statusText.setText(status);
                    Toast.makeText(FirebaseTestActivity.this, 
                            "Проверка синхронизации успешна", Toast.LENGTH_SHORT).show();
                } else {
                    statusText.setText("Ошибка: привычки не найдены в Firebase");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String status = "Ошибка чтения из Firebase: " + error.getMessage();
                statusText.setText(status);
                Log.e(TAG, "Firebase sync check error", error.toException());
            }
        });
    }

    // Метод для принудительной установки правил базы данных
    private void forceSetDatabaseRules() {
        statusText.setText("Попытка принудительной установки правил базы данных...");
        
        // Проверяем аутентификацию
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            statusText.setText("Ошибка: необходимо сначала пройти аутентификацию");
            Toast.makeText(this, 
                    "Сначала нажмите кнопку 'Проверить аутентификацию'", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://habittracker-1bbd8-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference testRef = database.getReference(".settings/rules");
        
        // Правила разрешают чтение и запись только аутентифицированным пользователям
        String rules = "{\"rules\":{\"test\":{\"$uid\":{\"read\":\"auth.uid === $uid\",\"write\":\"auth.uid === $uid\"}},\"habits\":{\"$uid\":{\"read\":\"auth.uid === $uid\",\"write\":\"auth.uid === $uid\"}},\"habit_logs\":{\"$uid\":{\"read\":\"auth.uid === $uid\",\"write\":\"auth.uid === $uid\"}}}}";
        
        testRef.setValue(rules)
                .addOnSuccessListener(aVoid -> {
                    String status = "Правила базы данных успешно установлены!\n" + 
                            "Попробуйте снова проверить чтение/запись";
                    statusText.setText(status);
                    Toast.makeText(FirebaseTestActivity.this, 
                            "Правила установлены", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    String status = "Ошибка установки правил: " + e.getMessage() + 
                            "\nПожалуйста, настройте правила вручную в Firebase Console";
                    statusText.setText(status);
                    Log.e(TAG, "Rules set error", e);
                });
    }
} 