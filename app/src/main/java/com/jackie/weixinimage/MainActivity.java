package com.jackie.weixinimage;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private GridView mGridView;
    private RelativeLayout mBottomLayout;
    private TextView mDirName;
    private TextView mDirCount;
    private DirPopupWindow mDirPopupWindow;
    private ImageAdapter mImgAdapter;
    private List<String> mImgs;
    private File currDir;
    private int currDirCount;
    private List<FolderBean> mFolderBeans = new ArrayList<>();
    private ProgressDialog mProgressDialog;
    private int mMaxCount;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x110) {
                mProgressDialog.dismiss();
                refreshViewData();
                initDirPopupWindow();
            }
        }
    };

    private void initDirPopupWindow() {
        mDirPopupWindow = new DirPopupWindow(MainActivity.this, mFolderBeans);
        mDirPopupWindow.setAnimationStyle(R.style.DirPopupWindowAnim);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //内容区域变亮
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.alpha = 1.0f;
                getWindow().setAttributes(layoutParams);

            }
        });
        mDirPopupWindow.setmOnDirSelectedListener(new DirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                currDir = new File(folderBean.getDirPath());
                mImgs = Arrays.asList(currDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".jpg")) {
                            return true;
                        }
                        return false;
                    }
                }));

                int count = mImgs.size();
                mImgAdapter = new ImageAdapter(MainActivity.this, mImgs, currDir.getAbsolutePath());
                mGridView.setAdapter(mImgAdapter);
                mDirCount.setText(count + "张");
                mDirName.setText(currDir.getName());
                mDirPopupWindow.dismiss();

            }
        });
    }

    private void refreshViewData() {
        if (currDir == null) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(currDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".jpg")) {
                    return true;
                }
                return false;
            }
        }));
        Log.d("jackie", "currDir.list()=====" + mImgs.size());
        mImgAdapter = new ImageAdapter(this, mImgs, currDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);
        mDirCount.setText(currDirCount + "张");
        mDirName.setText(currDir.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDatas();
        initEvent();
    }

    private void initEvent() {
        mBottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirPopupWindow.showAsDropDown(mBottomLayout, 0, 0);
                //内容区域变暗
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.alpha = 0.3f;
                getWindow().setAttributes(layoutParams);
            }
        });

    }

    /**
     * 利用ContentProvider扫描手机图片
     */
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");
        new Thread() {
            @Override
            public void run() {
                //1.安卓图片Uri,用于给使用
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                //2.声明ContentResolver
                ContentResolver contentResolver = MainActivity.this.getContentResolver();
                //3.查询,得到Cursor
                Cursor cursor = contentResolver.query(
                        mImgUri,
                        null,
                        MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED//按照修改日期排序
                );
                //用一个Set存储遍历过的文件夹
                Set<String> mDirSet = new HashSet<String>();

                //4.遍历游标获取文件夹
                while (cursor.moveToNext()) {
                    //1.得到图片路径(String类型)
                    String imgPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    Log.d("jackie", "得到图片路径(String类型)=====" + imgPath);
                    //2.得到图片所在目录(File类型)
                    File dirFile = new File(imgPath).getParentFile();
                    //如果目录不存在直接进入下一次循环
                    if (dirFile == null) continue;
                    //3.得到图片所在目录绝对路径(String类型)
                    String dirPath = dirFile.getAbsolutePath();
                    Log.d("jackie", "得到图片所在目录绝对路径(String类型)=====" + dirPath);
                    //4.封装FolderBean
                    FolderBean folderBean = null;
                    //如果此文件夹已经遍历过直接进入下一次循环
                    if (mDirSet.contains(dirPath)) continue;
                    mDirSet.add(dirPath);
                    folderBean = new FolderBean();
                    folderBean.setDirPath(dirPath);
                    folderBean.setFirstImgPath(imgPath);
                    //如果文件集合为空直接进入下一次循环
                    if (dirFile.listFiles() == null) continue;
                    //过滤并得到文件夹下图片的数量
                    int count = dirFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".jpg")) {
                                return true;
                            }
                            return false;
                        }
                    }).length;
                    Log.d("jackie", "过滤并得到文件夹下图片的数量=====" + count);
                    folderBean.setCount(count);
                    mFolderBeans.add(folderBean);
                    if (count > currDirCount) {
                        currDirCount = count;
                        currDir = dirFile;
                    }
                }
                //扫描结束释放资源
                cursor.close();
                mHandler.sendEmptyMessage(0x110);
            }
        }.start();
    }

    private void initView() {

        mGridView = (GridView) findViewById(R.id.gvMain);
        mBottomLayout = (RelativeLayout) findViewById(R.id.bottomLayout);
        mDirName = (TextView) findViewById(R.id.tvDirName);
        mDirCount = (TextView) findViewById(R.id.tvDirCount);

    }
}
