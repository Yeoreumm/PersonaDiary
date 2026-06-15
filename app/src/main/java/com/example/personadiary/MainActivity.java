package com.example.personadiary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    View screenHome, screenDiary, screenGrowth, screenWrite, screenDetail;

    LinearLayout listDiary;
    //LinearLayout bottomTab;
    BottomNavigationView bottomTab;

    AppDatabase db;

    Diary selectedDiary;        // 지금 보고 있는 일기 기억 변수

    boolean calendarVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this);                        *******(지워도됨!!!)
        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "diary-db"
        ).allowMainThreadQueries().build();
        // ** ㄴ  allowMainThreadQueries() 원래 권장되지 않음. 기말 프로젝트에서는 간단하고 편해서 많이 사용

        // 화면 찾기
        screenHome = findViewById(R.id.screen_home);
        screenDiary = findViewById(R.id.screen_diary);
        screenGrowth = findViewById(R.id.screen_growth);
        screenWrite = findViewById(R.id.screen_write);
        screenDetail = findViewById(R.id.screen_detail);

        // 하단 탭 버튼
        bottomTab = findViewById(R.id.bottom_tab);

        // 성장 화면 탭 눌릴 때마다 갱신
        bottomTab.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showScreen(0);
            } else if (id == R.id.nav_diary) {
                showScreen(1);
            } else if (id == R.id.nav_growth) {
                updateGrowthScreen();       // 성장 화면 데이터 채우기
                showScreen(2);
            }
            return true;
        });

        // 홈 화면 오늘 날짜 자동 표시
        TextView homeDate = findViewById(R.id.home_date);
        String today = new SimpleDateFormat(
                "yyyy년 MM월 dd일 EEEE", Locale.KOREAN).format(new Date());
        homeDate.setText(today);

        //===================================================
        // 홈 화면 , 말풍선 AI 연결
        updateHomeBubble();

        // 홈 화면 "오늘 일기 쓰기 버튼"
        Button btnGoWrite = findViewById(R.id.btn_go_write);
        btnGoWrite.setOnClickListener(v -> showScreen(3));

        // 일기 목록
        listDiary = findViewById(R.id.list_diary);
        TextView btnAdd = findViewById(R.id.btn_add);

        // + 버튼 : 일기 쓰기 화면 전환
        btnAdd.setOnClickListener(v -> showScreen(3));

        android.widget.ImageView btnCalendar = findViewById(R.id.btn_calendar);
        if (btnCalendar != null) {
            btnCalendar.setOnClickListener(v -> setupCalendar());
        }

        // 앱 시작 시 기존 일기 불러오기
        List<Diary> diaryList = db.diaryDao().getAll();
        for (Diary diary : diaryList) {
            addDiaryCard(diary);     // false : 불러오기
        }

        // 쓰기 화면 저장 버튼
        EditText editDiary = findViewById(R.id.edit_diary);
        EditText editTitle = findViewById(R.id.edit_title);
        EditText editEmotion = findViewById(R.id.edit_emotion);
        Button btnSave = findViewById(R.id.btn_save);
        // 뒤로가기 버튼
        TextView btnWriteBack = findViewById(R.id.btn_write_back);
        btnWriteBack.setOnClickListener(v -> showScreen(1));

        // 저장 : 저장 시 목록에 추가, 일기 화면으로 돌아감
        btnSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            String emotion = editEmotion.getText().toString();
            String content = editDiary.getText().toString();

            // 빈 칸 저장 방지
            if (title.isEmpty() || emotion.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "빈칸을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            String date = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            Diary diary = new Diary( title, emotion, content, date);
            db.diaryDao().insert(diary);
            // addDiaryCard(diary);        // 카드로 추가
            listDiary.removeAllViews();
            List<Diary> refreshed = db.diaryDao().getAll();
            for (Diary d : refreshed) {
                addDiaryCard(d);
            }

            editTitle.setText("");
            editEmotion.setText("");
            editDiary.setText("");
            updateHomeBubble();
            showScreen(1);
        });

        // 전문 보기 화면 버튼
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> showScreen(1));

        btnDelete.setOnClickListener(v -> {
            if (selectedDiary != null) {
                db.diaryDao().delete(selectedDiary);    // DB에서 삭제
                listDiary.removeAllViews();             // 목록에서 초기화
                List<Diary> updated = db.diaryDao().getAll();
                for (Diary d : updated) {
                    addDiaryCard(d);                    // 목록 다시 그리기
                }
                selectedDiary = null;
                showScreen(1);
            }
        });
    }

    // 셋 중 하나만 보여주고 나머지 숨기는 함수
    void showScreen(int which) {
        screenHome.setVisibility  (which == 0 ? View.VISIBLE : View.GONE);
        screenDiary.setVisibility (which == 1 ? View.VISIBLE : View.GONE);
        screenGrowth.setVisibility(which == 2 ? View.VISIBLE : View.GONE);
        screenWrite.setVisibility (which == 3 ? View.VISIBLE : View.GONE);
        screenDetail.setVisibility(which == 4 ? View.VISIBLE : View.GONE);

        // 쓰기(3), 전문보기(4) 에서 하단 탭 숨기기
        bottomTab.setVisibility( (which == 3 || which == 4) ? View.GONE : View.VISIBLE);
    }

    void addDiaryCard(Diary diary) {
        View card = getLayoutInflater().inflate(R.layout.item_diary, listDiary, false);

        TextView itemDate    = card.findViewById(R.id.item_date);
        TextView itemEmotion = card.findViewById(R.id.item_emotion);
        TextView itemTitle   = card.findViewById(R.id.item_title);
        // TextView content = card.findViewById(R.id.item_content);     // 목록에서 내용 보여주기 할거면 이거

        // 앞 10글자만 출력 => yyyy-MM-dd 만
        itemDate.setText(diary.date.substring(0, 10));
        itemEmotion.setText(diary.emotion);
        itemTitle.setText(diary.title);
        //content.setText(diary.content);

        // 감정별 칩 색 사용
        setEmotionChipColor(itemEmotion, diary.emotion);

        card.setOnClickListener(v -> openDetail(diary));
        listDiary.addView(card);              // 불러오기 -> 항상 아래로 쌓기
    }

    void openDetail(Diary diary) {
        selectedDiary = diary;      // 어떤 일기인지 기억

        TextView detailDate = findViewById(R.id.detail_date);
        TextView detailEmotion = findViewById(R.id.detail_emotion);
        TextView detailTitle = findViewById(R.id.detail_title);
        TextView detailContent = findViewById(R.id.detail_content);

        detailDate.setText(diary.date);
        detailEmotion.setText(diary.emotion);
        detailTitle.setText(diary.title);
        detailContent.setText(diary.content);

        showScreen(4);      // 전문 보기 화면으로
    }

    void updateGrowthScreen() {
        List<Diary> all = db.diaryDao().getAll();
        int total = all.size();

        // 일기 수 -> 레벨 계산
        int level = (total / 5) + 1;
        TextView growthLevel = findViewById(R.id.growth_level);
        TextView growthCount = findViewById(R.id.growth_count);
        growthLevel.setText("Lv. " + level);
        growthCount.setText("일기 " + total + "편 작성");

        // =====================
        // 연속 작성일 계산
        TextView growthStreak = findViewById(R.id.growth_streak);
        java.util.Set<String> dateset = new java.util.HashSet<>();
        for (Diary d : all) {
            dateset.add(d.date.substring(0, 10));   // yyyy-MM-dd만 추출
        }

        int streak = 0;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

        while(true) {
            String dateStr = sdf.format(cal.getTime());
            if (dateset.contains(dateStr)) {
                streak++;
                cal.add(Calendar.DAY_OF_MONTH, -1); // 하루씩 거슬러 올라가기
            } else {
                break;
            }
        }

        growthStreak.setText("🔥 " + streak + "일 연속 작성 중!");

        // =====================
        // 이번 달 요약 AI
        TextView growthSummary = findViewById(R.id.growth_summary);
        growthSummary.setText("분석 중...");

        // 이번 달 일기만 필터링
        String thisMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        StringBuilder monthDiaries = new StringBuilder();
        for (Diary d : all) {
            if (d.date.startsWith(thisMonth)) {
                monthDiaries.append(d.date.substring(0, 10))
                        .append(": ")
                        .append(d.content)
                        .append("\n");
            }
        }

        if (monthDiaries.length() == 0) {
            growthSummary.setText("아직 이번 달 일기가 없어요.");
        } else {
            String summaryPrompt = "아래는 사용자가 이번 달에 쓴 일기야:\n" + monthDiaries + "\n이번 달 이 사람에게 어떤 일들이 있었는지, " + "감정 흐름은 어땠는지 따뜻하게 2~3문장으로 요약해줘. 반말로.";

            GeminiClient.ask(summaryPrompt, new GeminiClient.GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    growthSummary.setText(result);
                }

                @Override
                public void onFailure(String error) {
                    growthSummary.setText("요약을 불러오지 못했어요.");
                }
            });
        }

        // =====================
        // 감정별 카운트
        java.util.Map<String, Integer> emotionMap = new java.util.LinkedHashMap<>();
        for (Diary d : all) {
            String e = d.emotion == null || d.emotion.isEmpty() ? "기타" : d.emotion;
            emotionMap.put(e, emotionMap.getOrDefault(e, 0) + 1);
        }

        // 막대 4개에 순서대로 채우기
        int[] labels = {R.id.stat_label_1, R.id.stat_label_2, R.id.stat_label_3, R.id.stat_label_4, R.id.stat_label_5};

        int[] bars = {R.id.stat_bar_1, R.id.stat_bar_2, R.id.stat_bar_3, R.id.stat_bar_4, R.id.stat_bar_5};

        int[] counts = {R.id.stat_count_1, R.id.stat_count_2, R.id.stat_count_3, R.id.stat_count_4, R.id.stat_count_5};

        int[] rows = {R.id.stat_row_1, R.id.stat_row_2, R.id.stat_row_3, R.id.stat_row_4, R.id.stat_row_5};

        // 전체 최대값 (막대 길이 비율 계산용)
        int max = 1;
        for (int v : emotionMap.values()) if (v > max) max = v;

        int i = 0;
        for (java.util.Map.Entry<String, Integer> entry : emotionMap.entrySet()) {
            if (i >= 5) break;
            findViewById(rows[i]).setVisibility(View.VISIBLE);
            ((TextView) findViewById(labels[i])).setText(entry.getKey());
            ((TextView) findViewById(counts[i])).setText(entry.getValue() + "회");

            // 막대 길이 = 최대 200 dp 기준 비율 계산
            int widthDp = (int)(200f * entry.getValue() / max);
            int widthPx = (int)(widthDp * getResources().getDisplayMetrics().density);
            findViewById(bars[i]).getLayoutParams().width = widthPx;
            findViewById(bars[i]).requestLayout();
            i++;
        }

        // 남은 행 숨기기
        for (; i < 5; i++) {
            findViewById(rows[i]).setVisibility(View.GONE);
        }
    }

    void setEmotionChipColor(TextView chip, String emotion) {
        if (emotion == null) return;
        switch (emotion.trim()) {       // trim() : 공백 제거
            case "기쁨" :
                chip.setBackgroundResource(R.drawable.chip_pink);       // 연한 핑크 : 코튼캔디
                chip.setTextColor(0xFFA64D79);                          // 0xFF8E5C7A 이거랑 고민
                break;
            case "슬픔" :
                chip.setBackgroundResource(R.drawable.chip_blue);       // 연한 파랑
                chip.setTextColor(0xFF0C447C);
                break;
            case "분노" :
                chip.setBackgroundResource(R.drawable.chip_red);;       // 연한 빨강
                chip.setTextColor(0xFFA32D2D);
                break;
            case "지침" :
                chip.setBackgroundResource(R.drawable.chip_yellow);     // 연한 노랑
                chip.setTextColor(0xFF633806);
                break;
            case "평온" :
                chip.setBackgroundResource(R.drawable.chip_green);      // 연한 초록
                chip.setTextColor(0xFF085041);
                break;
            default:
                chip.setBackgroundResource(R.drawable.chip_gray);       // 회색 (기타)
                chip.setTextColor(0xFF444441);
                break;
        }
    }

    void setupCalendar() {
        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(this);
        dialog.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            String selected = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            listDiary.removeAllViews();
            List<Diary> all = db.diaryDao().getAll();
            boolean found = false;
            for (Diary d : all) {
                if (d.date.startsWith(selected)) {
                    addDiaryCard(d);
                    found = true;
                }
            }
            if (!found) {
                Toast.makeText(this, selected + "에 작성한 일기가 없어요", Toast.LENGTH_SHORT).show();
            }
            calendarVisible = false;
        });
        dialog.show();
    }

    // 말풍선 자동 갱신
    void updateHomeBubble() {
        TextView homeBubble = findViewById(R.id.home_bubble);
        if (homeBubble == null) return;

        homeBubble.setText("잠깐만...");

        List<Diary> recent = db.diaryDao().getAll();
        int total = recent.size();
        int level = (total / 5) + 1;

        StringBuilder diaryText = new StringBuilder();
        int count = 0;
        for (Diary d : recent) {
            if (count >= 5) break;
            diaryText.append("일기 내용 : ").append(d.content).append("\n");
            count++;
        }

        String prompt;
        if (diaryText.length() == 0) {
            // 일기 없으면 기본 멘트
            prompt = "처음 만난 사용자에게 일기 앱 캐릭터로서 친근하게 오늘 안부를 묻는 짧은 한 마디를 한국어로 만들어줘. 반말로.";
        } else {
            prompt = "아래는 사용자가 쓴 일기야:\n" + diaryText +
                    "\n이 사람의 말투와 감정을 분석해서, 이 사람처럼 말하는 친근한 캐릭터가 " +
                    "오늘 안부를 묻는 짧은 한 마디를 한국어로 만들어줘. " +
                    "사용자의 말투를 흉내내서 반말로. 30자 이내로.";
        }

        GeminiClient.ask(prompt, new GeminiClient.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                homeBubble.setText(result);
            }

            @Override
            public void onFailure(String error) {
                homeBubble.setText("오늘 하루 어땠어? 나한테 말해줘 ㅎㅎ");
            }
        });
    }
}