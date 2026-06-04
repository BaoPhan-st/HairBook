package com.haircut.app.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.ChatMessage;
import com.haircut.app.model.ChatRequest;
import com.haircut.app.model.ChatResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AIChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private LinearLayout layoutTyping;

    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        apiService = ApiClient.getService(this);
        setupViews();
        showWelcomeMessage();
    }

    private void setupViews() {
        rvMessages   = findViewById(R.id.rv_messages);
        etMessage    = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        layoutTyping = findViewById(R.id.layout_typing);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        adapter = new ChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
        etMessage.setOnEditorActionListener((v, actionId, event) -> { sendMessage(); return true; });
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
