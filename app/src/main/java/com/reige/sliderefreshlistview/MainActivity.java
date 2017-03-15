package com.reige.sliderefreshlistview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.reige.sliderefreshlistview.view.SlideRefreshListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> list ;
    private LVAdapter lvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final SlideRefreshListView listView = (SlideRefreshListView) findViewById(R.id.srListView);
        listView.setOnRefreshListener(new SlideRefreshListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // TODO: 2017/3/15 刷新逻辑

            }

            @Override
            public void onLoadMore() {
                // TODO: 2017/3/15 加载更多
                new Thread(){
                    @Override
                    public void run() {

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        list.add("加载更多出来的数据****");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listView.RefreshComplete(SlideRefreshListView.LOAD_MORE);
                                lvAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }.start();
            }
        });

        list = new ArrayList<>();
        for(int i=0;i<30;i++)
            list.add(i,"这是假数据"+i);
        lvAdapter = new LVAdapter();
        listView.setAdapter(lvAdapter);
    }
    class LVAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            TextView textView = new TextView(parent.getContext());
            textView.setTextSize(18f);
            textView.setText(list.get(position));

            return textView;
        }
    }

}
