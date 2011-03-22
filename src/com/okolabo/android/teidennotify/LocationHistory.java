package com.okolabo.android.teidennotify;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

import com.okolabo.android.teidennotify.DatabaseHelper.LocationHistories;

public class LocationHistory extends ListActivity {

    private DatabaseHelper mDBHelper;
    
    private Cursor mCursor;
    
    private SimpleCursorAdapter mAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_history);
        
        mDBHelper = new DatabaseHelper(this);
        
        ListView listView = getListView();
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

            public boolean onItemLongClick(final AdapterView<?> parent, final View v, int position, long id) {
                // TODO AlertDialogを表示して、タイトルの変更、履歴の削除、キャンセルなどを実装
                mCursor.moveToPosition(position);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        LocationHistory.this,
                        android.R.layout.select_dialog_item,
                        getResources().getStringArray(R.array.dialog_menu_location_history)
                );
                AlertDialog dialog = new AlertDialog.Builder(LocationHistory.this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(R.string.please_select)
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        // 選択された住所で検索
                                        String address = ((TextView)v.findViewById(android.R.id.text2)).getText().toString();
                                        Intent intent = new Intent();
                                        intent.putExtra(LocationHistories.ADDRESS, address);
                                        setResult(RESULT_OK, intent);
                                        dialog.dismiss();
                                        finish();
                                        break;
                                        
                                    case 1:
                                        // タイトルの変更
                                        LayoutInflater inflator = LocationHistory.this.getLayoutInflater();
                                        final View dv = inflator.inflate(R.layout.title_location_history, null);
                                        String title = mCursor.getString(mCursor.getColumnIndex(LocationHistories.TITLE));
                                        ((EditText)dv.findViewById(R.id.title_location_history)).setText(title);
                                        AlertDialog editDialog = new AlertDialog.Builder(LocationHistory.this)
                                                .setIcon(android.R.drawable.ic_dialog_info)
                                                .setTitle(R.string.edit_title)
                                                .setView(dv)
                                                .setPositiveButton(R.string.save, new OnClickListener() {
                                                    
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        long id = mCursor.getLong(mCursor.getColumnIndex(LocationHistories.ID));
                                                        String title = ((EditText)dv.findViewById(R.id.title_location_history)).getText().toString();
                                                        mDBHelper.updateLocationTitle(id, title);
                                                        mCursor = mDBHelper.getAllLocationHistories();
                                                        mAdapter.changeCursor(mCursor);
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .setNegativeButton(R.string.cancel, new OnClickListener() {
                                                    
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .create();
                                        editDialog.show();
                                        break;
                                        
                                    case 2:
                                        // 履歴の削除
                                        long id = mCursor.getLong(mCursor.getColumnIndex(LocationHistories.ID));
                                        mDBHelper.deleteLocationHistory(id);
                                        mCursor = mDBHelper.getAllLocationHistories();
                                        mAdapter.changeCursor(mCursor);
                                        Toast.makeText(LocationHistory.this, R.string.delete_history, Toast.LENGTH_SHORT).show();
                                        break;
                                        
                                    case 3:
                                        // キャンセル
                                        dialog.dismiss();
                                        break;
                                }
                            }
                        })
                        .create();
                dialog.show();
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCursor = mDBHelper.getAllLocationHistories();
        startManagingCursor(mCursor);
        if (mCursor.getCount() == 0) {
            Toast.makeText(this, R.string.no_location_history, Toast.LENGTH_SHORT).show();
            finish();
        }
        String[] from = {LocationHistories.TITLE, LocationHistories.ADDRESS};
        int[] to = {android.R.id.text1,
                    android.R.id.text2};
        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.row_location_history,
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
        String address = ((TextView)v.findViewById(android.R.id.text2)).getText().toString();
        Intent intent = new Intent();
        intent.putExtra(LocationHistories.ADDRESS, address);
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
