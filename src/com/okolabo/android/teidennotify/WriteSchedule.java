
package com.okolabo.android.teidennotify;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class WriteSchedule extends Activity {

    private Spinner mSpnAlarmTime;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);
        ArrayList<String> groups = getIntent().getStringArrayListExtra("groups");
        StringBuilder builder = new StringBuilder();
        String strDai = getString(R.string.dai);
        String strGroup = getString(R.string.group);
        for (String group : groups) {
            builder.append(strDai).append(group).append(strGroup).append("\n");
        }
        TextView txtGroups = (TextView) findViewById(R.id.groups);
        txtGroups.setText(builder.toString());
        
        mSpnAlarmTime = (Spinner)findViewById(R.id.alarmTime);
        ArrayAdapter<String> aa = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.default_alarm_time));
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnAlarmTime.setAdapter(aa);
        // デフォルトのアラーム時間を設定
        String alarmTime = Prefs.getDefaultAlarmTime(this);
        String[] alarmTimes = getResources().getStringArray(R.array.default_alarm_time_values);
        for (int i = 0; i < alarmTimes.length; i++) {
            if (alarmTimes[i].equals(alarmTime)) {
                mSpnAlarmTime.setSelection(i);
                break;
            }
        }
    }

    public void cancel(View v) {
        finish();
    }

    public void regist(View v) {

        DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
        final int year = datePicker.getYear();
        final int monthOfYear = datePicker.getMonth();
        final int dayOfMonth = datePicker.getDayOfMonth();
        CheckBox chkBox = (CheckBox) findViewById(R.id.setAlarm);
        boolean setAlarm = chkBox.isChecked();
        String[] alarmTimes = getResources().getStringArray(R.array.default_alarm_time_values);
        final int alarmBefore = Integer.valueOf(
                alarmTimes[mSpnAlarmTime.getSelectedItemPosition()]
                );

        int version = Build.VERSION.SDK_INT;
        final String calendarProvUri, eventProvUri, reminderProvUri;
        if (version >= 8) {
            calendarProvUri = "content://com.android.calendar/calendars";
            eventProvUri = "content://com.android.calendar/events";
            reminderProvUri = "content://com.android.calendar/reminders";
        } else {
            calendarProvUri = "content://calendar/calendars";
            eventProvUri = "content://calendar/events";
            reminderProvUri = "content://calendar/reminders";
        }

        ContentResolver contentResolver = getContentResolver();
        Date date = new Date(year - 1900, monthOfYear, dayOfMonth);
        SimpleDateFormat fmt = new SimpleDateFormat("MM月dd日(E)");
        // 指定された日付
        String day = fmt.format(date);
        // 指定されているグループを取得
        // String subGroup = mSubGroups[mSpnGroup.getSelectedItemPosition()];
        try {
            Intent intent = getIntent();
            ArrayList<String> groups = intent.getStringArrayListExtra("groups");
            String json = intent.getStringExtra("schedule");
            JSONObject schedule = new JSONObject(json);

            // カレンダーを取得する
            String[] projection = new String[] {
                    "_id", "name"
            };// idはプライマリーキー、nameはカレンダー名
            String selection = "access_level" + "=?";
            String[] selectionArgs = new String[] {
                "700"
            };
            Cursor managedCursor = managedQuery(Uri.parse(calendarProvUri), projection, selection,
                    selectionArgs, null);
            int[] mCalIds;
            String[] mCalNames;
            if (managedCursor.moveToFirst()) {
                int len = managedCursor.getCount();
                mCalIds = new int[len];
                mCalNames = new String[len];

                int idColumnIndex = managedCursor.getColumnIndex("_id");
                int nameColumnIndex = managedCursor.getColumnIndex("name");

                int i = 0;
                do {
                    mCalIds[i] = managedCursor.getInt(idColumnIndex);
                    mCalNames[i] = managedCursor.getString(nameColumnIndex);
                    i++;
                } while (managedCursor.moveToNext());

                for (String subGroup : groups) {
                    JSONArray teidens = schedule.getJSONObject("date").getJSONObject(day)
                            .getJSONArray(subGroup);
                    for (int j = 0; j < teidens.length(); j++) {
                        JSONObject teiden = teidens.getJSONObject(j);

                        // 停電開始時刻を取得(ミリ秒で)
                        String[] start = teiden.getString("start").split(":");
                        date.setHours(Integer.valueOf(start[0]));
                        date.setMinutes(Integer.valueOf(start[1]));
                        long startDate = date.getTime();

                        // 停電終了時刻を取得(ミリ秒で)
                        String[] end = teiden.getString("end").split(":");
                        date.setHours(Integer.valueOf(end[0]));
                        date.setMinutes(Integer.valueOf(end[1]));
                        long endDate = date.getTime();

                        // 備考を取得(実施 or 中止)
                        String note = teiden.getString("note");

                        // 計画停電をカレンダーに登録する
                        ContentValues cv = new ContentValues();
                        cv.put("calendar_id", mCalIds[0]);
                        cv.put("title", getString(R.string.plan_teiden) + getString(R.string.dai)
                                + subGroup + getString(R.string.group) + note);
                        // cv.put("description",
                        // (String)mSpnGroup.getSelectedItem());
                        // cv.put("eventLocation", "住所");
                        cv.put("dtstart", startDate);// ミリ秒で指定
                        cv.put("dtend", endDate);// ミリ秒で指定

                        // 通知機能を使用する場合は1を設定する。使用しない場合は省略して良い
                        if (setAlarm) {
                            // アラームをセット
                            if (!note.equals("(中止)")) {
                                // 実行される可能性があるので、alarmBefore分前に通知
                                cv.put("hasAlarm", 1);
                                Uri eventUri = contentResolver.insert(Uri.parse(eventProvUri), cv);
                                long rowId = Long.parseLong(eventUri.getLastPathSegment());
                                cv = new ContentValues();
                                cv.put("event_id", rowId);
                                cv.put("method", 1);// 通知方法を指定する
                                cv.put("minutes", alarmBefore);// 分単位で指定する
                                contentResolver.insert(Uri.parse(reminderProvUri), cv);
                            } else {
                                // 中止なので、登録はするが、通知はしない
                                contentResolver.insert(Uri.parse(eventProvUri), cv);
                            }
                        } else {
                            // アラームなし
                            contentResolver.insert(Uri.parse(eventProvUri), cv);
                        }
                        Toast.makeText(this, R.string.toast_regist_calendar, Toast.LENGTH_LONG)
                                .show();
                        finish();
                    }
                }
            }
        } catch (JSONException e) {
            // e.printStackTrace();
            Toast.makeText(this, R.string.cannot_get_schedule, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
