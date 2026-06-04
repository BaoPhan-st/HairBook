package com.haircut.app.model;
public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT  = 1;
    public String text;
    public int type;
    public long timestamp;
    public ChatMessage(String text, int type) {
        this.text = text; this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}
