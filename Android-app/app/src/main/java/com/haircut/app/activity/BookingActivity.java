package com.haircut.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.adapter.BarberAdapter;
import com.haircut.app.adapter.ServiceAdapter;
import com.haircut.app.adapter.TimeSlotAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.BarberModel;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.BookingRequest;
import com.haircut.app.model.RescheduleRequest;
import com.haircut.app.model.ServiceModel;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình đặt lịch — hỗ trợ 3 chế độ:
 *
 *  MODE_CREATE     — đặt lịch mới bình thường.
 *  MODE_REBOOK     — đặt lại dựa trên 1 booking cũ (COMPLETED / đã huỷ / NO_SHOW).
 *                    Tự điền sẵn dịch vụ, thợ, ghi chú từ booking cũ, nhưng bắt
 *                    buộc chọn lại ngày/giờ mới. Tạo booking MỚI, không đổi booking cũ.
 *  MODE_RESCHEDULE — đổi giờ 1 booking đang PENDING/CONFIRMED. Giữ nguyên dịch vụ
 *                    và thợ (không cho đổi), chỉ chọn lại ngày/giờ. Cập nhật booking
 *                    hiện tại, không tạo booking mới.
 */
public class BookingActivity extends AppCompatActivity {

    public static final String EXTRA_MODE        = "mode";
    public static final int MODE_CREATE     = 0;
    public static final int MODE_REBOOK     = 1;
    public static final int MODE_RESCHEDULE = 2;

    public static final String EXTRA_BOOKING_ID   = "booking_id";
    public static final String EXTRA_BARBER_ID    = "barber_id";
    public static final String EXTRA_BARBER_NAME  = "barber_name";
    public static final String EXTRA_SERVICE_ID   = "service_id";
    public static final String EXTRA_SERVICE_NAME = "service_name";
    public static final String EXTRA_NOTE         = "note";

    private RecyclerView rvServices, rvBarbers, rvTimeSlots;
    private CalendarView calendarView;
    private Button btnConfirm, btnBack;
    private ProgressBar progressBar;
    private TextView tvToolbarTitle, tvLockedSummary;
    private View layoutStepService, layoutStepBarber, layoutStepNote;
    private EditText etNote;

    private ApiService apiService;
    private Long selectedServiceId;
    private String selectedServiceName;
    private Long selectedBarberId;
    private String selectedBarberName;
    private String selectedDate; // yyyy-MM-dd
    private String selectedSlot;

    private int mode = MODE_CREATE;
    private Long rescheduleBookingId;

    // Giới hạn ngày đặt lịch tối đa kể từ hôm nay
    private static final int MAX_DAYS_AHEAD = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        apiService = ApiClient.getService(this);

        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_CREATE);

        initViews();
        applyMode();
        setupCalendar();

        if (mode == MODE_RESCHEDULE) {
            // Dịch vụ/thợ đã cố định từ intent, chỉ cần tải giờ trống.
            loadTimeSlots();
        } else {
            loadServices();
            loadBarbers();
        }
    }

    private void initViews() {
        rvServices        = findViewById(R.id.rv_services);
        rvBarbers         = findViewById(R.id.rv_barbers);
        rvTimeSlots       = findViewById(R.id.rv_time_slots);
        calendarView      = findViewById(R.id.calendar_view);
        btnConfirm        = findViewById(R.id.btn_confirm_booking);
        progressBar       = findViewById(R.id.progress_bar);
        tvToolbarTitle    = findViewById(R.id.tv_toolbar_title);
        tvLockedSummary   = findViewById(R.id.tv_locked_summary);
        layoutStepService = findViewById(R.id.layout_step_service);
        layoutStepBarber  = findViewById(R.id.layout_step_barber);
        layoutStepNote    = findViewById(R.id.layout_step_note);
        etNote            = findViewById(R.id.et_note);

        rvServices.setLayoutManager(new GridLayoutManager(this, 2));
        rvBarbers.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 4));

        // Mặc định hôm nay
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        btnConfirm.setOnClickListener(v -> attemptBooking());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void applyMode() {
        Intent intent = getIntent();

        switch (mode) {
            case MODE_REBOOK:
                tvToolbarTitle.setText("Đặt lại lịch hẹn");
                btnConfirm.setText("Xác nhận đặt lại");
                // Prefill nhưng vẫn cho phép người dùng đổi dịch vụ/thợ nếu muốn.
                selectedServiceId   = intent.getLongExtra(EXTRA_SERVICE_ID, -1) == -1
                        ? null : intent.getLongExtra(EXTRA_SERVICE_ID, -1);
                selectedServiceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
                selectedBarberId    = intent.getLongExtra(EXTRA_BARBER_ID, -1) == -1
                        ? null : intent.getLongExtra(EXTRA_BARBER_ID, -1);
                selectedBarberName  = intent.getStringExtra(EXTRA_BARBER_NAME);
                String oldNote = intent.getStringExtra(EXTRA_NOTE);
                if (oldNote != null) etNote.setText(oldNote);
                if (selectedServiceName != null || selectedBarberName != null) {
                    Toast.makeText(this,
                            "Đã điền lại: " +
                                    (selectedServiceName != null ? selectedServiceName : "") +
                                    (selectedBarberName != null ? " - " + selectedBarberName : "") +
                                    ". Vui lòng chọn ngày giờ mới.",
                            Toast.LENGTH_LONG).show();
                }
                break;

            case MODE_RESCHEDULE:
                tvToolbarTitle.setText("Đổi lịch hẹn");
                btnConfirm.setText("Xác nhận đổi lịch");
                rescheduleBookingId = intent.getLongExtra(EXTRA_BOOKING_ID, -1);
                selectedServiceId   = intent.getLongExtra(EXTRA_SERVICE_ID, -1) == -1
                        ? null : intent.getLongExtra(EXTRA_SERVICE_ID, -1);
                selectedServiceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
                selectedBarberId    = intent.getLongExtra(EXTRA_BARBER_ID, -1) == -1
                        ? null : intent.getLongExtra(EXTRA_BARBER_ID, -1);
                selectedBarberName  = intent.getStringExtra(EXTRA_BARBER_NAME);

                // Đổi lịch KHÔNG cho đổi dịch vụ/thợ/ghi chú — chỉ đổi ngày giờ.
                layoutStepService.setVisibility(View.GONE);
                layoutStepBarber.setVisibility(View.GONE);
                layoutStepNote.setVisibility(View.GONE);
                tvLockedSummary.setVisibility(View.VISIBLE);
                tvLockedSummary.setText("Giữ nguyên: " +
                        (selectedServiceName != null ? selectedServiceName : "dịch vụ đã chọn") +
                        " — " +
                        (selectedBarberName != null ? selectedBarberName : "thợ đã chọn") +
                        ". Chỉ chọn lại ngày/giờ mới.");
                break;

            default: // MODE_CREATE
                tvToolbarTitle.setText("Đặt lịch cắt tóc");
                btnConfirm.setText("Xác nhận đặt lịch");
                break;
        }
    }

    private void loadServices() {
        apiService.getAllServices().enqueue(new Callback<List<ServiceModel>>() {
            @Override public void onResponse(Call<List<ServiceModel>> call, Response<List<ServiceModel>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    rvServices.setAdapter(new ServiceAdapter(resp.body(), s -> {
                        selectedServiceId = s.id;
                        selectedServiceName = s.name;
                        selectedSlot = null; // đổi dịch vụ -> duration đổi -> phải chọn lại giờ
                        String duration = s.durationMinutes != null ? (s.durationMinutes + " phút") : "";
                        String price = s.price != null ? String.format(Locale.getDefault(), "%,.0fđ", s.price) : "";
                        Toast.makeText(BookingActivity.this,
                                "Đã chọn: " + s.name + " - " + price + " - " + duration,
                                Toast.LENGTH_SHORT).show();
                        loadTimeSlots();
                    }));
                } else {
                    Toast.makeText(BookingActivity.this, "Không tải được danh sách dịch vụ", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<ServiceModel>> call, Throwable t) {
                Toast.makeText(BookingActivity.this, "Lỗi kết nối khi tải dịch vụ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBarbers() {
        apiService.getAllBarbers().enqueue(new Callback<List<BarberModel>>() {
            @Override public void onResponse(Call<List<BarberModel>> call, Response<List<BarberModel>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    rvBarbers.setAdapter(new BarberAdapter(resp.body(), b -> {
                        if (Boolean.FALSE.equals(b.available)) {
                            Toast.makeText(BookingActivity.this, "Thợ này hiện không nhận lịch", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedBarberId = b.id;
                        selectedBarberName = b.name;
                        selectedSlot = null;
                        Toast.makeText(BookingActivity.this, "Đã chọn thợ: " + b.name, Toast.LENGTH_SHORT).show();
                        loadTimeSlots();
                    }));
                } else {
                    Toast.makeText(BookingActivity.this, "Không tải được danh sách thợ", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<BarberModel>> call, Throwable t) {
                Toast.makeText(BookingActivity.this, "Lỗi kết nối khi tải danh sách thợ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCalendar() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        calendarView.setMinDate(today.getTimeInMillis());

        Calendar maxDate = (Calendar) today.clone();
        maxDate.add(Calendar.DAY_OF_YEAR, MAX_DAYS_AHEAD);
        calendarView.setMaxDate(maxDate.getTimeInMillis());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            selectedSlot = null;
            loadTimeSlots();
        });
    }

    private void loadTimeSlots() {
        if (selectedBarberId == null || selectedDate == null) return;
        if (selectedServiceId == null) {
            // Chưa chọn dịch vụ thì chưa biết thời lượng -> chưa thể tính slot chính xác
            return;
        }
        apiService.getAvailableSlots(selectedBarberId, selectedDate, selectedServiceId)
                .enqueue(new Callback<List<String>>() {
                    @Override public void onResponse(Call<List<String>> call, Response<List<String>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            if (resp.body().isEmpty()) {
                                Toast.makeText(BookingActivity.this, "Thợ đã kín lịch trong ngày này", Toast.LENGTH_SHORT).show();
                            }
                            rvTimeSlots.setAdapter(new TimeSlotAdapter(resp.body(), slot -> {
                                selectedSlot = slot;
                            }));
                        } else {
                            Toast.makeText(BookingActivity.this, parseError(resp), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<List<String>> call, Throwable t) {
                        Toast.makeText(BookingActivity.this, "Lỗi kết nối khi tải giờ trống", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void attemptBooking() {
        if (selectedServiceId == null) { Toast.makeText(this, "Vui lòng chọn dịch vụ", Toast.LENGTH_SHORT).show(); return; }
        if (selectedBarberId == null)  { Toast.makeText(this, "Vui lòng chọn thợ cắt", Toast.LENGTH_SHORT).show(); return; }
        if (selectedDate == null)      { Toast.makeText(this, "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show(); return; }
        if (selectedSlot == null)      { Toast.makeText(this, "Vui lòng chọn giờ", Toast.LENGTH_SHORT).show(); return; }

        // Chặn thêm lần nữa ở client (backend vẫn là nguồn kiểm tra cuối cùng/đáng tin cậy)
        if (!isSlotStillValid()) {
            Toast.makeText(this, "Khung giờ đã chọn không còn hợp lệ, vui lòng chọn lại", Toast.LENGTH_SHORT).show();
            selectedSlot = null;
            loadTimeSlots();
            return;
        }

        String bookingTimeIso = selectedDate + "T" + selectedSlot + ":00";

        progressBar.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        if (mode == MODE_RESCHEDULE) {
            doReschedule(bookingTimeIso);
        } else {
            doCreateBooking(bookingTimeIso);
        }
    }

    private void doCreateBooking(String bookingTimeIso) {
        BookingRequest req = new BookingRequest();
        req.serviceId   = selectedServiceId;
        req.barberId    = selectedBarberId;
        req.bookingTime = bookingTimeIso;
        req.note        = etNote.getText().toString().trim();

        apiService.createBooking(req).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> call, Response<BookingModel> resp) {
                progressBar.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                if (resp.isSuccessful()) {
                    Toast.makeText(BookingActivity.this,
                            mode == MODE_REBOOK ? "Đặt lại lịch thành công! ✓" : "Đặt lịch thành công! ✓",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else if (resp.code() == 409) {
                    Toast.makeText(BookingActivity.this, "Khung giờ vừa bị người khác đặt, vui lòng chọn giờ khác", Toast.LENGTH_LONG).show();
                    selectedSlot = null;
                    loadTimeSlots();
                } else {
                    Toast.makeText(BookingActivity.this, parseError(resp), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                Toast.makeText(BookingActivity.this, "Lỗi kết nối server, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doReschedule(String bookingTimeIso) {
        if (rescheduleBookingId == null || rescheduleBookingId == -1) {
            progressBar.setVisibility(View.GONE);
            btnConfirm.setEnabled(true);
            Toast.makeText(this, "Không xác định được lịch hẹn cần đổi", Toast.LENGTH_SHORT).show();
            return;
        }
        RescheduleRequest req = new RescheduleRequest(bookingTimeIso);
        apiService.rescheduleBooking(rescheduleBookingId, req).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> call, Response<BookingModel> resp) {
                progressBar.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                if (resp.isSuccessful()) {
                    Toast.makeText(BookingActivity.this, "Đổi lịch thành công! ✓", Toast.LENGTH_LONG).show();
                    finish();
                } else if (resp.code() == 409) {
                    Toast.makeText(BookingActivity.this, "Khung giờ vừa bị người khác đặt, vui lòng chọn giờ khác", Toast.LENGTH_LONG).show();
                    selectedSlot = null;
                    loadTimeSlots();
                } else {
                    Toast.makeText(BookingActivity.this, parseError(resp), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                Toast.makeText(BookingActivity.this, "Lỗi kết nối server, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Nếu ngày đặt là hôm nay, slot phải còn ở tương lai tại thời điểm bấm xác nhận. */
    private boolean isSlotStillValid() {
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            if (!selectedDate.equals(today)) return true;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date slotTime = sdf.parse(selectedDate + " " + selectedSlot);
            return slotTime != null && slotTime.after(new Date());
        } catch (Exception e) {
            return true; // không chắc chắn -> để backend quyết định, tránh chặn nhầm
        }
    }

    private String parseError(Response<?> resp) {
        try {
            if (resp.errorBody() != null) {
                String raw = resp.errorBody().string();
                JSONObject obj = new JSONObject(raw);
                if (obj.has("error")) return obj.getString("error");
            }
        } catch (Exception ignored) { }
        return "Thao tác thất bại, vui lòng thử lại";
    }
}