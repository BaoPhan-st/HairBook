package com.haircut.app.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.ChatMessage;
import com.haircut.app.model.ChatRequest;
import com.haircut.app.model.ChatResponse;
import com.haircut.app.util.FaceShapeAnalyzer;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AIChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnScanFace;
    private LinearLayout layoutTyping;

    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ApiService apiService;

    private FaceDetector faceDetector;
    private ActivityResultLauncher<Void> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        apiService = ApiClient.getService(this);
        setupFaceDetector();
        setupViews();
        showWelcomeMessage();
    }

    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) analyzeFace(bitmap);
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) takePictureLauncher.launch(null);
                    else Toast.makeText(this, "Cần quyền camera để quét khuôn mặt", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupViews() {
        rvMessages   = findViewById(R.id.rv_messages);
        etMessage    = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        btnScanFace  = findViewById(R.id.btn_scan_face);
        layoutTyping = findViewById(R.id.layout_typing);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnScanFace.setOnClickListener(v -> onScanFaceClicked());

        adapter = new ChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> etMessage.post(this::sendMessage));
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                // post() để đợi IME commit xong composing text (dấu tiếng Việt) trước khi đọc getText()
                etMessage.post(this::sendMessage);
                return true;
            }
            return false;
        });
    }

    private void showWelcomeMessage() {
        addBotMessage("Xin chào! ✂️ Tôi là trợ lý AI của HairBook.\n\n" +
                "Tôi có thể tư vấn:\n• Kiểu tóc phù hợp khuôn mặt\n" +
                "• Thông tin dịch vụ & giá\n• Giới thiệu thợ cắt tóc\n\n" +
                "Bạn cần tư vấn gì ạ? 😊");
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        addUserMessage(text);
        etMessage.setText("");
        sendToAI(text);
    }

    /** Gửi thẳng một message tới Gemini (dùng chung cho gõ tay và kết quả quét khuôn mặt). */
    private void sendToAI(String text) {
        setTyping(true);
        apiService.sendChatMessage(new ChatRequest(text)).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                setTyping(false);
                addBotMessage(response.isSuccessful() && response.body() != null
                        ? response.body().reply : "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại!");
            }
            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                setTyping(false);
                addBotMessage("Không kết nối được server. Kiểm tra lại mạng nhé!");
            }
        });
    }

    // ---- Quét khuôn mặt ----

    private void onScanFaceClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void analyzeFace(Bitmap bitmap) {
        setTyping(true);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    setTyping(false);
                    if (faces.isEmpty()) {
                        addBotMessage("Mình chưa nhận diện được khuôn mặt trong ảnh 😅. " +
                                "Bạn chụp lại rõ mặt hơn, đủ sáng và nhìn thẳng vào camera nhé!");
                        return;
                    }
                    Face face = faces.get(0);
                    String shape = FaceShapeAnalyzer.classify(face);
                    addUserMessage("📷 Đã quét khuôn mặt — dạng: " + shape);

                    String prompt = "Tôi vừa quét khuôn mặt và kết quả cho thấy khuôn mặt tôi thuộc dạng: "
                            + shape + ". Hãy tư vấn 2-3 kiểu tóc phù hợp nhất với dạng khuôn mặt này.";
                    sendToAI(prompt);
                })
                .addOnFailureListener(e -> {
                    setTyping(false);
                    addBotMessage("Có lỗi khi phân tích khuôn mặt, vui lòng thử lại!");
                });
    }

    private void addUserMessage(String text) {
        messages.add(new ChatMessage(text, ChatMessage.TYPE_USER));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        messages.add(new ChatMessage(text, ChatMessage.TYPE_BOT));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        rvMessages.postDelayed(() ->
                rvMessages.smoothScrollToPosition(messages.size() - 1), 100);
    }

    private void setTyping(boolean typing) {
        layoutTyping.setVisibility(typing ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!typing);
        btnScanFace.setEnabled(!typing);
    }

    // ---- Chat Adapter ----
    static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<ChatMessage> messages;
        ChatAdapter(List<ChatMessage> messages) { this.messages = messages; }

        @Override public int getItemViewType(int pos) { return messages.get(pos).type; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == ChatMessage.TYPE_USER)
                return new VH(inf.inflate(R.layout.item_chat_user, parent, false));
            return new VH(inf.inflate(R.layout.item_chat_bot, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((VH) holder).tvMessage.setText(messages.get(position).text);
        }

        @Override public int getItemCount() { return messages.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvMessage;
            VH(View v) { super(v); tvMessage = v.findViewById(R.id.tv_message); }
        }
    }
}