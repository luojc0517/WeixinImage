package com.jackie.weixinimage;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Law on 2015/12/17.
 */
public class ImageAdapter extends BaseAdapter {
    private Context context;
    private List<String> Imgs;
    private String dirPath;
    private LayoutInflater layoutInflater;
    //保存已经选中的img
    private static Set<String> mSelectedImgs = new HashSet<String>();

    public ImageAdapter(Context context, List<String> imgs, String dirPath) {
        this.context = context;
        this.Imgs = imgs;
        this.dirPath = dirPath;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return Imgs.size();
    }

    @Override
    public Object getItem(int position) {
        return Imgs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position,  View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.item_img, null);
            viewHolder.ivImg = (ImageView) convertView.findViewById(R.id.ivImg);
            viewHolder.btnImg = (ImageButton) convertView.findViewById(R.id.btnImg);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        //重置状态
        viewHolder.ivImg.setImageResource(R.drawable.pictures_no);
        viewHolder.btnImg.setImageResource(R.drawable.picture_unselected);
        viewHolder.ivImg.setColorFilter(null);
        //用ImageLoader加载图片
        ImageLoader.getInstance(3, ImageLoader.TYPE.LIFO).loadImage(
                dirPath + "/" + Imgs.get(position),
                viewHolder.ivImg
        );
        final String imgTag = dirPath + "/" + Imgs.get(position);
        //为image添加点击事件
        viewHolder.ivImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedImgs.contains(imgTag)) {//如果已经是选中状态那么点击就要取消选中
                    mSelectedImgs.remove(imgTag);
                    viewHolder.btnImg.setImageResource(R.drawable.picture_unselected);
                    viewHolder.ivImg.setColorFilter(null);
                } else {
                    mSelectedImgs.add(imgTag);
                    //图片覆盖上透明灰色
                    viewHolder.ivImg.setColorFilter(Color.parseColor("#77000000"));
                    //按钮变成选中状态
                    viewHolder.btnImg.setImageResource(R.drawable.pictures_selected);

                }
                //这里使用notifyDataSetChanged();会造成闪屏

            }
        });
        if (mSelectedImgs.contains(imgTag)) {
            //图片覆盖上透明灰色
            viewHolder.ivImg.setColorFilter(Color.parseColor("#77000000"));
            //按钮变成选中状态
            viewHolder.btnImg.setImageResource(R.drawable.pictures_selected);
        }

        return convertView;
    }

    private class ViewHolder {
        ImageView ivImg;
        ImageButton btnImg;
    }
}
