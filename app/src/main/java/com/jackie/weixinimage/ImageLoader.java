package com.jackie.weixinimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by Law on 2015/12/15.
 */
public class ImageLoader {
    //单例
    private static ImageLoader mImageLoader;
    //缓存池，缓存Url-Bitmap
    private LruCache<String, Bitmap> mBitmapLruCache;
    //线程池
    private ExecutorService mThreadPool;

    //枚举调度方式
    public enum TYPE {
        FIFO, LIFO
    }

    //调度方式
    private static TYPE mType = TYPE.LIFO;
    //线程池中线程数量
    private static int DEFAUT_THREAD_COUNT = 1;
    //任务队列
    private LinkedList<Runnable> mTaskQueue;
    /*
        轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    //信号量，防止mPoolThreadHandler还没初始化addTask就使用它
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    //根据线程数量初始化信号量
    private Semaphore mSemaphoreThreadPool;
    /*
        UI线程
     */
    private Handler mUIHandler;

    private ImageLoader(int threadCount, TYPE type) {
        init(threadCount, type);
    }

    /**
     * 初始化操作
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, TYPE type) {
        /*
            初始化轮询线程
         */
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //从线程池取出任务执行
                        mThreadPool.execute(getTask());
                        //acquire一下信号量看是否有空位
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //mPoolThreadHandler初始化完以后释放信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
        /*
            初始化图片缓存
         */
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mBitmapLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        /*
            创建线程池
         */
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        /*
            创建任务队列
         */
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;
        mSemaphoreThreadPool = new Semaphore(threadCount);

    }


    /**
     * 获取单例
     *
     * @return mImageLoader
     */
    public static ImageLoader getInstance(int threadCount, TYPE type) {
        if (mImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (mImageLoader == null) {
                    mImageLoader = new ImageLoader(threadCount, type);
                }
            }
        }
        return mImageLoader;
    }

    /**
     * 加载图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImageHolder imageHolder = (ImageHolder) msg.obj;
                    ImageView imageView = imageHolder.imageView;
                    Bitmap bitmap = imageHolder.bitmap;
                    String path = imageHolder.path;
                    if (imageView.getTag().equals(path)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }

        Bitmap bitmap = getBitmapFromCache(path);
        if (bitmap != null) {//缓存中若存在此图片则直接UI刷新
            Message msg = Message.obtain();
            ImageHolder imageHolder = new ImageHolder();
            imageHolder.bitmap = bitmap;
            imageHolder.path = path;
            imageHolder.imageView = imageView;
            msg.obj = imageHolder;
            mUIHandler.sendMessage(msg);
        } else {//否则增加一个任务到任务队列
            addTask(new Runnable() {
                /**
                 * 获取并压缩图片
                 */
                @Override
                public void run() {
                    //压缩图片
                    //1、获得图片显示的大小
                    ImageSize imageSize = getImageSize(imageView);
                    //2、压缩图片
                    Bitmap bitmap = decodeSampleFromPath(path, imageSize.width, imageSize.height);
                    //3、将图片加入缓存
                    addBitmapToCache(path, bitmap);
                    //4、通知UI线程刷新图片
                    Message msg = Message.obtain();
                    ImageHolder imageHolder = new ImageHolder();
                    imageHolder.bitmap = bitmap;
                    imageHolder.path = path;
                    imageHolder.imageView = imageView;
                    msg.obj = imageHolder;
                    mUIHandler.sendMessage(msg);
                    //5、释放信号量，让其他正在阻塞的线程进入线程池的执行队列
                    mSemaphoreThreadPool.release();

                }
            });
        }
    }

    /**
     * 压缩图片，利用Options
     *
     * @param path
     * @param width
     * @param height
     * @return bitmap
     */
    private Bitmap decodeSampleFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //获取图片宽高，但是不加载图片到内存中去
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        //加载到内存
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * 计算samplesize
     *
     * @param options   实际宽高
     * @param reqWidth  需求宽
     * @param reqHeight 需求高
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    /**
     * 根据imageView获取适当的宽高
     *
     * @param imageView
     * @return imagesize
     */
    private ImageSize getImageSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        int width = imageView.getWidth();
        if (width <= 0) {//imageView还没有显示出来
            width = layoutParams.width;//imageView在布局中的声明宽度，但是wrap_content时宽为0
        }
        if (width <= 0) {//imageView的声明是wrap content
            width = imageView.getMaxWidth();
        }
        if (width <= 0) {//最坏情况获取屏幕宽度
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();
        if (width <= 0) {//imageView还没有显示出来
            width = layoutParams.height;//imageView在布局中的声明宽度，但是wrap_content时宽为0
        }
        if (width <= 0) {//imageView的声明是wrap content
            width = imageView.getMaxHeight();
        }
        if (width <= 0) {//最坏情况获取屏幕宽度
            width = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /**
     * 给任务队列添加任务,并通知线程池执行任务
     *
     * @param runnable
     */
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        if (mPoolThreadHandler == null) {
            try {
                //如果信号量还没有被释放(即handler还没有完成初始化),那么代码将阻塞,知道信号量被释放
                mSemaphorePoolThreadHandler.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 从任务队列取出任务
     *
     * @return 最后一个放进去的任务
     */
    private Runnable getTask() {
        Runnable runnable = null;
        if (mType == TYPE.LIFO) {
            runnable = mTaskQueue.removeLast();
        } else {
            runnable = mTaskQueue.removeFirst();
        }
        return runnable;
    }

    /**
     * 从缓存中获取图片
     *
     * @param path
     * @return bitmap
     */
    public Bitmap getBitmapFromCache(String path) {
        Bitmap bitmap = null;
        if (!path.isEmpty() && !path.equals("")) {
            bitmap = mBitmapLruCache.get(path);
        }
        return bitmap;

    }

    /**
     * 将图片放入缓存
     *
     * @param path
     * @param bitmap
     */
    public void addBitmapToCache(String path, Bitmap bitmap) {
        if (getBitmapFromCache(path) == null) {
            if (bitmap != null) {
                mBitmapLruCache.put(path, bitmap);
            }
        }
    }

    /**
     * 图片holder
     */
    private class ImageHolder {
        String path;
        ImageView imageView;
        Bitmap bitmap;
    }

    /**
     * 实际图片大小，包含宽高
     */
    private class ImageSize {
        int width;
        int height;
    }
}
