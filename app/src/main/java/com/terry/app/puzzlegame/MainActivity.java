package com.terry.app.puzzlegame;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.yancy.imageselector.ImageConfig;
import com.yancy.imageselector.ImageSelector;
import com.yancy.imageselector.ImageSelectorActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    /**
     * 利用二维数组创建小方块
     */
    private ImageView[][] iv_game_arr = new ImageView[3][5];

    private GridLayout gl_main_game;

    private ImageView iv_null_imageView;
    //手势判断
    private GestureDetector mDetector;
    //游戏是否开始
    private boolean isGameStart = false;
    //动画运行状态
    private boolean isAnimRun = false;

    private ImageView mImageView;

    private Toolbar mToolbar;

    public static final int CHOOSE_PHOTO = 1;

    private Bitmap bigBm;

    private TextView mCount;

    private static final int COUNT = 0x01;

    private ArrayList<String> path = new ArrayList<>();

    public static final int REQUEST_CODE = 1000;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COUNT) {
                count++;
                mCount.setText("" + count);
            }
            super.handleMessage(msg);
        }
    };

    private int count = 0;

    private Timer timer;

    TimerTask timerTask;


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCount = (TextView) findViewById(R.id.count);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        // 初始化游戏主界面，并添加若干个小方块
        gl_main_game = (GridLayout) findViewById(R.id.gl_main_game);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("拼图游戏");
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.choose) {
                    ImageConfig imageConfig
                            = new ImageConfig.Builder(
                            // GlideLoader 可用自己用的缓存库
                            new GlideLoader())
                            // 如果在 4.4 以上，则修改状态栏颜色 （默认黑色）
                            .steepToolBarColor(getResources().getColor(R.color.blue))
                            // 标题的背景颜色 （默认黑色）
                            .titleBgColor(getResources().getColor(R.color.blue))
                            // 提交按钮字体的颜色  （默认白色）
                            .titleSubmitTextColor(getResources().getColor(R.color.white))
                            // 标题颜色 （默认白色）
                            .titleTextColor(getResources().getColor(R.color.white))
                            // 开启多选   （默认为多选）  (单选 为 singleSelect)
                            .singleSelect()
                            .crop()
                            // 多选时的最大数量   （默认 9 张）
                            .mutiSelectMaxSize(9)
                            // 已选择的图片路径
                            .pathList(path)
                            // 拍照后存放的图片路径（默认 /temp/picture）
                            .filePath("/ImageSelector/Pictures")
                            // 开启拍照功能 （默认开启）
                            .showCamera()
                            .requestCode(REQUEST_CODE)
                            .build();

                    ImageSelector.open(MainActivity.this, imageConfig);   // 开启图片选择器
                    return true;
                }
                return false;
            }
        });


        mDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                int type = getDirByGes(e1.getX(), e1.getY(), e2.getX(), e2.getY());
//                Toast.makeText(MainActivity.this, type + "", Toast.LENGTH_SHORT).show();
                changeByDir(type);
                return false;
            }
        });

        //初始化若干个小方块
        bigBm = ((BitmapDrawable) getResources().getDrawable(R.mipmap.main_pic)).getBitmap();//获取大图资源
        inintView(bigBm);
    }

    private void inintView(Bitmap bigBm) {
//        bigBm = BitmapFactory.decodeResource(getResources(), R.mipmap.timg);
        gl_main_game.removeAllViews();
        //小方块宽度应该为整个屏幕宽度/5
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int ivWidth = metrics.widthPixels / 5;
//        int ivWidth = getWindowManager().getDefaultDisplay().getWidth() / 5;
        int width = bigBm.getWidth() / 5;//小方块宽高
        for (int i = 0; i < iv_game_arr.length; i++) {
            for (int j = 0; j < iv_game_arr[0].length; j++) {
                Bitmap bm = Bitmap.createBitmap(bigBm, j * width, i * width, width, width);
                iv_game_arr[i][j] = new ImageView(this);
                iv_game_arr[i][j].setImageBitmap(bm);
                //设置小方块之间的间距
                iv_game_arr[i][j].setPadding(2, 2, 2, 2);
                iv_game_arr[i][j].setTag(new GameData(i, j, bm));
                iv_game_arr[i][j].setLayoutParams(new RelativeLayout.LayoutParams(ivWidth, ivWidth));
                iv_game_arr[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean flag = isHasByNullImageView((ImageView) v);
//                        Toast.makeText(MainActivity.this, "位置关系是否存在：" + flag, Toast.LENGTH_SHORT).show();
                        if (flag) {
                            changeDataByImageView((ImageView) v);
                        }
                    }
                });
            }
        }

        for (int i = 0; i < iv_game_arr.length; i++) {
            for (int j = 0; j < iv_game_arr[0].length; j++) {
                gl_main_game.addView(iv_game_arr[i][j]);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(bigBm, 0, 0, 5 * width, 3 * width);
        mImageView = (ImageView) findViewById(R.id.iv_pic);
        mImageView.setImageBitmap(bitmap);
        //设置空方块
        setNullImageView(iv_game_arr[2][4]);
        //随机打乱顺序
        randomMove();
//        //游戏开始
        isGameStart = true;
        if (isGameStart) {
            if (timerTask != null) {
                timerTask.cancel();
            }
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message msg = new Message();
                    msg.what = COUNT;
                    handler.sendMessage(msg);
                }
            };
            timer = new Timer(true);
            timer.schedule(timerTask, 1000, 1000);
        }
    }

    public void changeDataByImageView(final ImageView mImageView) {
        changeDataByImageView(mImageView, true);
    }

    /**
     * 利用动画结束后，交换两个方块的数据
     *
     * @param mImageView 点击的方块
     * @param isAnim     true 播放动画 false 不播放动画
     */
    public void changeDataByImageView(final ImageView mImageView, boolean isAnim) {
        if (isAnimRun) {
            return;
        }
        if (!isAnim) {
            GameData mGameData = (GameData) mImageView.getTag();
            iv_null_imageView.setImageBitmap(mGameData.bm);
            GameData mNullGameData = (GameData) iv_null_imageView.getTag();
            mNullGameData.bm = mGameData.bm;
            mNullGameData.p_x = mGameData.p_x;
            mNullGameData.p_y = mGameData.p_y;
            //设置当前点击的方块为空方块
            setNullImageView(mImageView);
            if (isGameStart) {
                isGameOver();//拼图完成弹出框提示
            }
            return;
        }
        //创建一个动画，设置好方向，移动距离
        TranslateAnimation translateAnimation = null;
        if (mImageView.getX() > iv_null_imageView.getX()) {//往上移动
            translateAnimation = new TranslateAnimation(0.1f, -mImageView.getWidth(), 0.1f, 0.1f);
        } else if (mImageView.getX() < iv_null_imageView.getX()) {//下移
            translateAnimation = new TranslateAnimation(0.1f, mImageView.getWidth(), 0.1f, 0.1f);
        } else if (mImageView.getY() > iv_null_imageView.getY()) {//左移
            translateAnimation = new TranslateAnimation(0.1f, 0.1f, 0.1f, -mImageView.getWidth());
        } else if (mImageView.getY() < iv_null_imageView.getY()) {//右移
            translateAnimation = new TranslateAnimation(0.1f, 0.1f, 0.1f, mImageView.getWidth());
        }
        //设置动画时长
        translateAnimation.setDuration(70);
        //设置动画结束之后是否停留
        translateAnimation.setFillAfter(true);
        //设置动画结束之后交换数据
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimRun = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimRun = false;
                mImageView.clearAnimation();
                GameData mGameData = (GameData) mImageView.getTag();
                iv_null_imageView.setImageBitmap(mGameData.bm);
                GameData mNullGameData = (GameData) iv_null_imageView.getTag();
                mNullGameData.bm = mGameData.bm;
                mNullGameData.p_x = mGameData.p_x;
                mNullGameData.p_y = mGameData.p_y;
                //设置当前点击的方块为空方块
                setNullImageView(mImageView);
                if (isGameStart) {
                    isGameOver();//拼图完成弹出框提示
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mImageView.startAnimation(translateAnimation);
    }

    public void changeByDir(int type) {
        changeByDir(type, true);
    }


    /**
     * 根据手势方向，获取方块相应的位置如果存在方块，进行数据交换
     *
     * @param type   1.上，2.下，3.左，4.右
     * @param isAnim 是否有动画
     */
    public void changeByDir(int type, boolean isAnim) {
        //获取当前空方块的位置
        GameData mNullGameData = (GameData) iv_null_imageView.getTag();
        //根据方向设置相应的相邻位置的坐标
        int new_x = mNullGameData.x;
        int new_y = mNullGameData.y;
        if (type == 1) {//要移动的方块在空方块的下方
            new_x++;
        } else if (type == 2) {
            new_x--;
        } else if (type == 3) {
            new_y++;
        } else if (type == 4) {
            new_y--;
        }
        //判断这个新坐标是否存在,存在就移动在x0~2,y0~4之间判断
        if (new_x >= 0 && new_x < iv_game_arr.length && new_y >= 0 && new_y < iv_game_arr[0].length) {
            if (isAnim) {
                changeDataByImageView(iv_game_arr[new_x][new_y]);
            } else {//无动画直接交换数据
                changeDataByImageView(iv_game_arr[new_x][new_y], isAnim);
            }
        } else {
            //不存在不做移动
        }
    }

    @Override
    protected void onStop() {
//        timer.cancel();
        super.onStop();
    }

    //判断游戏是否结束
    public void isGameOver() {
        boolean isGameOver = true;
        //遍历每个游戏小方块
        for (int i = 0; i < iv_game_arr.length; i++) {
            for (int j = 0; j < iv_game_arr[0].length; j++) {
                if (iv_game_arr[i][j] == iv_null_imageView) {
                    continue;
                }
                GameData mGameData = (GameData) iv_game_arr[i][j].getTag();
                if (!mGameData.isTrue()) {
                    isGameOver = false;
                    break;
                }
            }
        }
        if (isGameOver) {
            timer.cancel();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("恭喜你完成拼图").setMessage("用时" + count + "秒").setCancelable(false).setPositiveButton("好的", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        }
    }

    /**
     * 手势判断，左滑还是右滑
     *
     * @param start_x 手势起始点x
     * @param start_y 手势起始点y
     * @param end_x   手势终止点x
     * @param end_y   手势终止点y
     * @return 1.上，2.下，3.左，4.右
     */
    public int getDirByGes(float start_x, float start_y, float end_x, float end_y) {
        boolean isLeftOrRight = (Math.abs(start_x - end_x) > Math.abs(start_y - end_y)) ? true : false;
        if (isLeftOrRight) {//左右
            boolean isLeft = start_x - end_x > 0 ? true : false;
            if (isLeft) {
                return 3;
            } else {
                return 4;
            }
        } else {//上下
            boolean isUp = start_y - end_y > 0 ? true : false;
            if (isUp) {
                return 1;
            } else {
                return 2;
            }
        }
    }

    //随机打乱顺序
    public void randomMove() {
        //打乱次数
        for (int i = 0; i < 50; i++) {
            int type = (int) (Math.random() * 4) + 1;//获取1-4之间的随机数
            changeByDir(type, false);
        }
    }

    /**
     * 设置某个方块为空
     */
    public void setNullImageView(ImageView mImageView) {
        mImageView.setImageBitmap(null);
        iv_null_imageView = mImageView;
    }

    /**
     * 判断当前点击的方块，是否与空方块的位置是相邻关系
     *
     * @param mImageView
     * @return true 相邻 false 不相邻
     */
    public boolean isHasByNullImageView(ImageView mImageView) {
        GameData mNullGameData = (GameData) iv_null_imageView.getTag();
        GameData mGameData = (GameData) mImageView.getTag();
        if (mNullGameData.y == mGameData.y && mGameData.x + 1 == mNullGameData.x) {//点击的小方块在空方块上方
            return true;
        } else if (mNullGameData.y == mGameData.y && mGameData.x - 1 == mNullGameData.x) {//点击的小方块在空方块下方
            return true;
        } else if (mNullGameData.y == mGameData.y + 1 && mGameData.x == mNullGameData.x) {//点击的小方块在空方块左方
            return true;
        } else if (mNullGameData.y == mGameData.y - 1 && mGameData.x == mNullGameData.x) {//点击的小方块在空方块右方
            return true;
        }
        return false;
    }

    class GameData {
        public int x = 0;//小方块的实际位置x
        public int y = 0;//小方块的实际位置y
        public Bitmap bm;//每个小方块图片
        public int p_x = 0;//每个小方块的位置x
        public int p_y = 0;//每个小方块的位置y

        public GameData(int x, int y, Bitmap bm) {
            this.x = x;
            this.y = y;
            this.bm = bm;
            this.p_x = x;
            this.p_y = y;
        }

        public boolean isTrue() {
            if (x == p_x && y == p_y) return true;
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            List<String> pathList = data.getStringArrayListExtra(ImageSelectorActivity.EXTRA_RESULT);

            for (String path : pathList) {
                Log.i("ImagePathList", path);
            }

            path.clear();
            path.addAll(pathList);
        }
        displayImage(path.get(path.size() - 1));
    }

    //    private void handleImageBeforeKitkat(Intent data) {
//        Uri uri = data.getData();
//        String imagePath = getImagePath(uri, null);
//        displayImage(imagePath);
//    }
//
    public void displayImage(String imagePath) {
        isGameStart = false;
        count = 0;
        timer.purge();
        timerTask.run();
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        inintView(bitmap);
    }
//
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    private void handleImageOnKitKat(Intent data) {
//        String imagePath = null;
//        Uri uri = data.getData();
//        if (DocumentsContract.isDocumentUri(this, uri)) {
//            String docId = DocumentsContract.getDocumentId(uri);
//            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
//                String id = docId.split(":")[1];
//                String selection = MediaStore.Images.Media._ID + "=" + id;
//                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
//            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
//                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
//                imagePath = getImagePath(contentUri, null);
//            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
//                imagePath = getImagePath(uri, null);
//            }
//        }
//        displayImage(imagePath);
//    }
//
//
//    private String getImagePath(Uri externalContentUri, String selection) {
//        String path = null;
//        Cursor cursor = getContentResolver().query(externalContentUri, null, selection, null, null);
//        if (cursor != null) {
//            if (cursor.moveToFirst()) {
//                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
//            }
//            cursor.close();
//        }
//        return path;
//    }

    //获取菜单点击更换图片
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.choose_pic, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
