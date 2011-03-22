package com.okolabo.android.teidennotify;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.okolabo.android.teidennotify.DatabaseHelper.InputHistories;

public class InputHistory extends ListActivity {

    private DatabaseHelper mDBHelper;
    
    private Cursor mCursor;
    
    private SimpleCursorAdapter mAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_history);
        
        mDBHelper = new DatabaseHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCursor = mDBHelper.getAllInputHistories();
        startManagingCursor(mCursor);
        if (mCursor.getCount() == 0) {
            Toast.makeText(this, R.string.no_input_history, Toast.LENGTH_SHORT).show();
            finish();
        }
        String[] from = {InputHistories.PREF, InputHistories.ADDRESS};
        int[] to = {android.R.id.text1,
                    android.R.id.text2};
        mAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.two_line_list_item,
                mCursor,
                from,
                to
                );
        setListAdapter(mAdapter);
    }
    
    @Override
    protected void onDestroy() {
        mDBHelper.close();
        super.onDestroy();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String pref = ((TextView)v.findViewById(android.R.id.text1)).getText().toString();
        String address = ((TextView)v.findViewById(android.R.id.text2)).getText().toString();
        Intent intent = new Intent();
        intent.putExtra(InputHistories.PREF, pref);
        intent.putExtra(InputHistories.ADDRESS, address);
        setResult(RESULT_OK, intent);
        finish();
    }
    
    /**
     * 画面回転対策
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
