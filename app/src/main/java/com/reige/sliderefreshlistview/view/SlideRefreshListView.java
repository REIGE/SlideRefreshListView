package com.reige.sliderefreshlistview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TimeFormatException;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.reige.sliderefreshlistview.R;

import java.text.SimpleDateFormat;

/**
 * Created by REIGE on 2017/3/15.
 */

public class SlideRefreshListView extends ListView implements AbsListView.OnScrollListener{

    private View header;
    private ImageView arrow;
    private ProgressBar progressBar;
    private TextView content;
    private TextView time;
    private float downY;
    private float moveY;
    private int mHeaderHeight;
    //箭头上滑动画
    private RotateAnimation rotateUpAnimation;
    //箭头下滑动画
    private RotateAnimation rotateDowmAnimation;
    //下拉刷新
    public static final int PULL_TO_REFRESH = 0;
    //释放刷新
    public static final int RELEASE_REFRESH = 1;
    //正在刷新中
    public static final int REFRESHING = 2;
    //当前刷新模式
    private int currentState = PULL_TO_REFRESH;
    //刷新监听用于接收具体实现
    private OnRefreshListener onRefreshListener;
    //是否在加载更多 默认没有加载更多
    boolean isLoadMore = false;
    private View footer;
    private int mFooterHeight;

    //刷新完成状态
    public static final int REFRESH = 0;
    //加载更多完成状态
    public static final int LOAD_MORE = 1;

    public SlideRefreshListView(Context context) {
        super(context);
        init();
    }

    public SlideRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SlideRefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    //初始化
    private void init() {
        initHeader();
        initFooter();
        initAnim();
        setOnScrollListener(this);
    }

    //初始化头布局
    private void initHeader() {
        header = View.inflate(getContext(), R.layout.layout_header, null);

        arrow = (ImageView) header.findViewById(R.id.arrow);
        progressBar = (ProgressBar) header.findViewById(R.id.progressBar);
        content = (TextView) header.findViewById(R.id.tv_content);
        time = (TextView) header.findViewById(R.id.tv_time);

        header.measure(0, 0);
        mHeaderHeight = header.getMeasuredHeight();
        //初始化隐藏头布局
        header.setPadding(0, -mHeaderHeight, 0, 0);
        addHeaderView(header);
    }

    //初始化脚布局
    private void initFooter() {

        footer = View.inflate(getContext(), R.layout.layout_footer, null);
        //先测量再拿高度
        footer.measure(0, 0);
        mFooterHeight = footer.getMeasuredHeight();
        //初始化隐藏脚布局
        footer.setPadding(0, -mFooterHeight, 0, 0);
        addFooterView(footer);
    }

    //初始化动画 箭头旋转
    private void initAnim() {
        //逆时针 0->-180
        rotateUpAnimation = new RotateAnimation(0, -180f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        //持续时间
        rotateUpAnimation.setDuration(200);
        //动画结束后保持位置
        rotateUpAnimation.setFillAfter(true);
        rotateDowmAnimation = new RotateAnimation(-180, -360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateDowmAnimation.setDuration(200);
        rotateDowmAnimation.setFillAfter(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = ev.getY();

                //如果当前已经在刷新 则将事件交由父亲去处理
                if (currentState == REFRESHING) {
                    return super.onTouchEvent(ev);
                }

                float dy = moveY - downY; // 移动的偏移量
                // 当dy>0 且头布局可见时才改变头布局的padding
                if (dy > 0 && getFirstVisiblePosition() == 0) {
                    int paddingTop = (int) (-mHeaderHeight + 0.4 * dy);

                    header.setPadding(0, paddingTop, 0, 0);

                    //布局完全显示
                    if (paddingTop >= 0 && currentState != RELEASE_REFRESH) {
                        currentState = RELEASE_REFRESH;
                        updateHeader();
                    } else if (paddingTop < 0 && currentState != PULL_TO_REFRESH) {
                        //布局不完全显示
                        currentState = PULL_TO_REFRESH;
                        updateHeader();
                    }

                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                //还没滑到位 松开隐藏头布局
                if (currentState == PULL_TO_REFRESH) {
                    header.setPadding(0, -mHeaderHeight, 0, 0);
                } else if (currentState == RELEASE_REFRESH) {
                    //设置当前状态为REFRESHING 避免重复进入
                    currentState = REFRESHING;
                    //滑到位了 全部显示头布局
                    header.setPadding(0, 0, 0, 0);
                    updateHeader();
                }

                break;
        }

        return super.onTouchEvent(ev);

    }

    //根据currentState更新头布局
    private void updateHeader() {
        switch (currentState) {
            case PULL_TO_REFRESH:
                arrow.startAnimation(rotateUpAnimation);
                content.setText("下拉刷新...");
                break;
            case RELEASE_REFRESH:
                arrow.startAnimation(rotateDowmAnimation);
                content.setText("释放刷新...");

                break;
            case REFRESHING:

                //只有清除绑定的动画 才能设置不可见
                arrow.clearAnimation();
                arrow.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                content.setText("正在刷新...");

                if(onRefreshListener != null) {
                    onRefreshListener.onRefresh();
                }

                break;
            default:
                break;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {

        if(isLoadMore){
            return;
        }
        // 最新状态是空闲状态, 并且当前界面显示了所有数据的最后一条. 加载更多
        if(scrollState == SCROLL_STATE_IDLE && getLastVisiblePosition() >= (getCount() - 1)){
            isLoadMore = true;
            footer.setPadding(0, 0, 0, 0);

            setSelection(getCount()); // 跳转到最后一条, 使其显示出加载更多.

            if(onRefreshListener != null){
                onRefreshListener.onLoadMore();
            }
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {

    }

    // 获取当前时间
    private String getRefreshTime(){
        long current = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        return sdf.format(current);
    }


    public void RefreshComplete(int state){
        switch (state){
            case REFRESH:
                currentState = PULL_TO_REFRESH;
                content.setText("下拉刷新...");
                header.setPadding(0,-mHeaderHeight,0,0);
                arrow.setVisibility(VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                String ctime = getRefreshTime();
                time.setText("上次刷新时间: " + time);

                break;
            case LOAD_MORE:
                footer.setPadding(0, -mFooterHeight, 0, 0);
                isLoadMore = false;
                break;
        }
    }

    /**
     * 刷新回调接口
     */
    public interface OnRefreshListener{
        void onRefresh();
        void onLoadMore();
    }

    /**
     * 设置监听
     * @param orl
     */
    public void setOnRefreshListener(OnRefreshListener orl){
        this.onRefreshListener = orl;
    }
}
