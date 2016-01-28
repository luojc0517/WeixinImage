package com.jackie.weixinimage;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Law on 2016/1/27.
 */
public class DirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mDatas;
    private OnDirSelectedListener mOnDirSelectedListener;

    public DirPopupWindow(Context context, List<FolderBean> mDatas) {
        super(context);
        this.mDatas = mDatas;
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;

                }
                return false;
            }
        });
        initViews(context);
        initEvent();
    }

    public OnDirSelectedListener getmOnDirSelectedListener() {
        return mOnDirSelectedListener;
    }

    public void setmOnDirSelectedListener(OnDirSelectedListener mOnDirSelectedListener) {
        this.mOnDirSelectedListener = mOnDirSelectedListener;
    }

    /**
     * 设置接口回调，与MainAcitivity解耦
     */
    public interface OnDirSelectedListener {
        void onSelected(FolderBean folderBean);
    }

    private void initEvent() {

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mOnDirSelectedListener != null) {
                    mOnDirSelectedListener.onSelected(mDatas.get(position));
                }
            }
        });


    }

    private void initViews(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.list_dir);
        mListView.setAdapter(new DirAdapter(context, mDatas));

    }

    /**
     *
     */
    private class DirAdapter extends ArrayAdapter<FolderBean> {
        private LayoutInflater mLayoutInflater;
        private List<FolderBean> mDatas;

        public DirAdapter(Context context, List<FolderBean> objects) {
            super(context, 0, objects);
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = mLayoutInflater.inflate(R.layout.item_popup_main, parent, false);
                viewHolder.mImg = (ImageView) convertView.findViewById(R.id.dir_item_img);
                viewHolder.mDirName = (TextView) convertView.findViewById(R.id.dir_item_name);
                viewHolder.mDirCount = (TextView) convertView.findViewById(R.id.dir_item_count);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            FolderBean folderBean = getItem(position);
            //换屏重置图片
            viewHolder.mImg.setImageResource(R.drawable.pictures_no);
            ImageLoader.getInstance(3, ImageLoader.TYPE.LIFO).loadImage(folderBean.getFirstImgPath(), viewHolder.mImg);
            viewHolder.mDirName.setText(folderBean.getDirName());
            viewHolder.mDirCount.setText(folderBean.getCount() + "张");//注意数字转文本

            return convertView;
        }

        private class ViewHolder {
            ImageView mImg;
            TextView mDirName;
            TextView mDirCount;

        }
    }

    //根据上下文计算Popupwindow高度
    private void calWidthAndHeight(Context context) {
        //获得屏幕宽高
        WindowManager wm = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        //以屏幕宽，屏幕高的百分之七十作为popupwindow宽高
        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);

    }
}
