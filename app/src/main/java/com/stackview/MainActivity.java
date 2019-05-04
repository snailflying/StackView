package com.stackview;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import com.stackview.wiget.StackView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private StackItemAdapter mAdapter;

    StackView mStackView;
    private ArrayList<Integer> mViewIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStackView = (StackView) findViewById(R.id.stackview);


        mViewIds.add(R.drawable.s1);
        mViewIds.add(R.drawable.s2);
        mViewIds.add(R.drawable.s3);
        mAdapter = new StackItemAdapter();
        mStackView.setAdapter(mAdapter);

        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStackView.startLoop();
            }
        });
        findViewById(R.id.second_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,SecondActivity.class));
          }
        });

    }

    private class StackItemAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mViewIds.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.stack_item, parent, false);
                holder = new ViewHolder();
                holder.imgIv = (ImageView) convertView.findViewById(R.id.imgIv);
                holder.positionTv = convertView.findViewById(R.id.positionTv);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.imgIv.setImageResource(mViewIds.get(position));
            holder.positionTv.setText(String.format("NO.%d", position+1));
            return convertView;
        }
    }

    public static class ViewHolder {
        public ImageView imgIv;
        public TextView positionTv;
    }
}
