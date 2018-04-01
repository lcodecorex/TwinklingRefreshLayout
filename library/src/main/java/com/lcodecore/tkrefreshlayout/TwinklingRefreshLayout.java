package com.lcodecore.tkrefreshlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.lcodecore.tkrefreshlayout.footer.BallPulseView;
import com.lcodecore.tkrefreshlayout.header.GoogleDotView;
import com.lcodecore.tkrefreshlayout.processor.AnimProcessor;
import com.lcodecore.tkrefreshlayout.processor.IDecorator;
import com.lcodecore.tkrefreshlayout.processor.OverScrollDecorator;
import com.lcodecore.tkrefreshlayout.processor.RefreshProcessor;
import com.lcodecore.tkrefreshlayout.utils.DensityUtil;

import java.lang.reflect.Constructor;

import static android.support.v4.widget.ViewDragHelper.INVALID_POINTER;

/**
 * Created by lcodecore on 16/3/2.
 */
public class TwinklingRefreshLayout extends RelativeLayout implements PullListener, NestedScrollingChild {

    //波浪的高度,最大扩展高度
    protected float mMaxHeadHeight;
    protected float mMaxBottomHeight;

    //头部的高度
    protected float mHeadHeight;

    //允许的越界回弹的高度
    protected float mOverScrollHeight;

    //子控件
    private View mChildView;

    //头部layout
    protected FrameLayout mHeadLayout;

    //整个头部
    private FrameLayout mExtraHeadLayout;
    //附加顶部高度
    private int mExHeadHeight = 0;

    private IHeaderView mHeadView;
    private IBottomView mBottomView;

    //设置的默认的header/footer class的完整包名+类名
    private static String HEADER_CLASS_NAME = "";
    private static String FOOTER_CLASS_NAME = "";

    //底部高度
    private float mBottomHeight;

    //底部layout
    private FrameLayout mBottomLayout;


    //是否刷新视图可见
    protected boolean isRefreshVisible = false;

    //是否加载更多视图可见
    protected boolean isLoadingVisible = false;

    //是否处于刷新状态（和isRefreshVisible区别为是否开启了enableKeepHeadWhenRefresh）
    protected boolean isRefreshing = false;
    protected boolean isLoadingMore = false;

    //是否需要加载更多,默认需要
    protected boolean enableLoadmore = true;
    //是否需要下拉刷新,默认需要
    protected boolean enableRefresh = true;

    //是否在越界回弹的时候显示下拉图标
    protected boolean isOverScrollTopShow = true;
    //是否在越界回弹的时候显示上拉图标
    protected boolean isOverScrollBottomShow = true;

    //是否隐藏刷新控件,开启越界回弹模式(开启之后刷新控件将隐藏)
    protected boolean isPureScrollModeOn = false;

    //是否自动加载更多
    protected boolean autoLoadMore = false;

    //是否开启悬浮刷新模式
    protected boolean floatRefresh = false;

    //是否允许进入越界回弹模式
    protected boolean enableOverScroll = true;

    //是否在刷新或者加载更多后保持状态
    protected boolean enableKeepIView = true;

    //是否在越界且处于刷新时直接显示顶部
    protected boolean showRefreshingWhenOverScroll = true;

    //是否在越界且处于加载更多时直接显示底部
    protected boolean showLoadingWhenOverScroll = true;

    private CoContext cp;
    private final int mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    //设置手势动作的监听器
    private PullListener pullListener = this;

    private final NestedScrollingChildHelper mChildHelper;

    public TwinklingRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public TwinklingRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwinklingRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TwinklingRefreshLayout, defStyleAttr, 0);
        try {
            mMaxHeadHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_max_head_height, (int) DensityUtil.dp2px(context, 120));
            mHeadHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_head_height, (int) DensityUtil.dp2px(context, 80));
            mMaxBottomHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_max_bottom_height, (int) DensityUtil.dp2px(context, 120));
            mBottomHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_bottom_height, (int) DensityUtil.dp2px(context, 60));
            mOverScrollHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_overscroll_height, (int) mHeadHeight);
            enableRefresh = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_enable_refresh, true);
            enableLoadmore = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_enable_loadmore, true);
            isPureScrollModeOn = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_pureScrollMode_on, false);
            isOverScrollTopShow = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_overscroll_top_show, true);
            isOverScrollBottomShow = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_overscroll_bottom_show, true);
            enableOverScroll = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_enable_overscroll, true);
            floatRefresh = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_floatRefresh, false);
            autoLoadMore = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_autoLoadMore, false);
            enableKeepIView = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_enable_keepIView, true);
            showRefreshingWhenOverScroll = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_showRefreshingWhenOverScroll, true);
            showLoadingWhenOverScroll = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_showLoadingWhenOverScroll, true);
        } finally {
            a.recycle();
        }

        cp = new CoContext();

        addHeader();
        addFooter();

        setFloatRefresh(floatRefresh);
        setAutoLoadMore(autoLoadMore);
        setEnableRefresh(enableRefresh);
        setEnableLoadmore(enableLoadmore);

        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    private void addHeader() {
        FrameLayout headViewLayout = new FrameLayout(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        layoutParams.addRule(ALIGN_PARENT_TOP);

        FrameLayout extraHeadLayout = new FrameLayout(getContext());
        extraHeadLayout.setId(R.id.ex_header);
        LayoutParams layoutParams2 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        this.addView(extraHeadLayout, layoutParams2);
        this.addView(headViewLayout, layoutParams);

        mExtraHeadLayout = extraHeadLayout;
        mHeadLayout = headViewLayout;

        if (mHeadView == null) {
            if (!TextUtils.isEmpty(HEADER_CLASS_NAME)) {
                try {
                    Class headClazz = Class.forName(HEADER_CLASS_NAME);
                    Constructor ct = headClazz.getDeclaredConstructor(Context.class);
                    setHeaderView((IHeaderView) ct.newInstance(getContext()));
                } catch (Exception e) {
                    Log.e("TwinklingRefreshLayout:", "setDefaultHeader classname=" + e.getMessage());
                    setHeaderView(new GoogleDotView(getContext()));
                }
            } else {
                setHeaderView(new GoogleDotView(getContext()));
            }
        }
    }

    private void addFooter() {
        FrameLayout bottomViewLayout = new FrameLayout(getContext());
        LayoutParams layoutParams2 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        layoutParams2.addRule(ALIGN_PARENT_BOTTOM);
        bottomViewLayout.setLayoutParams(layoutParams2);

        mBottomLayout = bottomViewLayout;
        this.addView(mBottomLayout);

        if (mBottomView == null) {
            if (!TextUtils.isEmpty(FOOTER_CLASS_NAME)) {
                try {
                    Class clazz = Class.forName(FOOTER_CLASS_NAME);
                    Constructor ct = clazz.getDeclaredConstructor(Context.class);
                    setBottomView((IBottomView) ct.newInstance(getContext()));
                } catch (Exception e) {
                    Log.e("TwinklingRefreshLayout:", "setDefaultFooter classname=" + e.getMessage());
                    setBottomView(new BallPulseView(getContext()));
                }
            } else {
                setBottomView(new BallPulseView(getContext()));
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //获得子控件
        //onAttachedToWindow方法中mChildView始终是第0个child，把header、footer放到构造函数中，mChildView最后被inflate
        mChildView = getChildAt(3);

        cp.init();
        decorator = new OverScrollDecorator(cp, new RefreshProcessor(cp));
        initGestureDetector();
    }

    private IDecorator decorator;
    private OnGestureListener listener;

    private void initGestureDetector() {
        listener = new OnGestureListener() {
            @Override
            public void onDown(MotionEvent ev) {
                decorator.onFingerDown(ev);
            }

            @Override
            public void onScroll(MotionEvent downEvent, MotionEvent currentEvent, float distanceX, float distanceY) {
                decorator.onFingerScroll(downEvent, currentEvent, distanceX, distanceY, vx, vy);
            }

            @Override
            public void onUp(MotionEvent ev, boolean isFling) {
                decorator.onFingerUp(ev, isFling);
            }

            @Override
            public void onFling(MotionEvent downEvent, MotionEvent upEvent, float velocityX, float velocityY) {
                decorator.onFingerFling(downEvent, upEvent, velocityX, velocityY);
            }
        };
    }

    //VelocityX,VelocityY
    private float vx, vy;

    private VelocityTracker mVelocityTracker;
    private float mLastFocusX;
    private float mLastFocusY;
    private float mDownFocusX;
    private float mDownFocusY;
    private int mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
    private int mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
    private MotionEvent mCurrentDownEvent;
    private boolean mAlwaysInTapRegion;
    private int mTouchSlopSquare = mTouchSlop * mTouchSlop;

    private void detectGesture(MotionEvent ev, OnGestureListener listener) {
        final int action = ev.getAction();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final boolean pointerUp =
                (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? ev.getActionIndex() : -1;

        // Determine focal point
        float sumX = 0, sumY = 0;
        final int count = ev.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) continue;
            sumX += ev.getX(i);
            sumY += ev.getY(i);
        }
        final int div = pointerUp ? count - 1 : count;
        final float focusX = sumX / div;
        final float focusY = sumY / div;

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;

                // Check the dot product of current velocities.
                // If the pointer that left was opposing another velocity vector, clear.
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                final int upIndex = ev.getActionIndex();
                final int id1 = ev.getPointerId(upIndex);
                final float x1 = mVelocityTracker.getXVelocity(id1);
                final float y1 = mVelocityTracker.getYVelocity(id1);
                for (int i = 0; i < count; i++) {
                    if (i == upIndex) continue;

                    final int id2 = ev.getPointerId(i);
                    final float x = x1 * mVelocityTracker.getXVelocity(id2);
                    final float y = y1 * mVelocityTracker.getYVelocity(id2);

                    final float dot = x + y;
                    if (dot < 0) {
                        mVelocityTracker.clear();
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mDownFocusX = mLastFocusX = focusX;
                mDownFocusY = mLastFocusY = focusY;
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(ev);
                mAlwaysInTapRegion = true;
                listener.onDown(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                final float scrollX = mLastFocusX - focusX;
                final float scrollY = mLastFocusY - focusY;
                if (mAlwaysInTapRegion) {
                    final int deltaX = (int) (focusX - mDownFocusX);
                    final int deltaY = (int) (focusY - mDownFocusY);
                    int distance = (deltaX * deltaX) + (deltaY * deltaY);
                    if (distance > mTouchSlopSquare) {
                        listener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                        mLastFocusX = focusX;
                        mLastFocusY = focusY;
                        mAlwaysInTapRegion = false;
                    }
                } else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    listener.onScroll(mCurrentDownEvent, ev, scrollX, scrollY);
                    mLastFocusX = focusX;
                    mLastFocusY = focusY;
                }
                break;
            case MotionEvent.ACTION_UP:
                final int pointerId = ev.getPointerId(0);
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                vy = mVelocityTracker.getYVelocity(pointerId);
                vx = mVelocityTracker.getXVelocity(pointerId);

                boolean isFling = false;
                if ((Math.abs(vy) > mMinimumFlingVelocity)
                        || (Math.abs(vx) > mMinimumFlingVelocity)) {
                    listener.onFling(mCurrentDownEvent, ev, vx, vy);
                    isFling = true;
                }

                listener.onUp(ev, isFling);

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mAlwaysInTapRegion = false;
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
    }

    /************************************* 触摸事件处理 *****************************************/
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //1.监听fling动作 2.获取手指滚动速度（存在滚动但非fling的状态）3.分发事件
        boolean consume = decorator.dispatchTouchEvent(ev);
        detectGesture(ev, listener);
        detectNestedScroll(ev);
        return consume;
    }

    /**
     * 拦截事件
     *
     * @return return true时,ViewGroup的事件有效,执行onTouchEvent事件
     * return false时,事件向下传递,onTouchEvent无效
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = decorator.interceptTouchEvent(ev);
        return intercept || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean consume = decorator.dealTouchEvent(e);
        return consume || super.onTouchEvent(e);
    }

    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];
    private int mActivePointerId = INVALID_POINTER;
    private int mLastTouchX;
    private int mLastTouchY;
    private boolean mIsBeingDragged;

    private boolean detectNestedScroll(MotionEvent e) {
        final MotionEvent vtev = MotionEvent.obtain(e);
        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = e.getPointerId(0);
                mLastTouchX = (int) e.getX();
                mLastTouchY = (int) e.getY();
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN:
                mActivePointerId = e.getPointerId(actionIndex);
                mLastTouchX = (int) e.getX(actionIndex);
                mLastTouchY = (int) e.getY(actionIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                final int index = e.findPointerIndex(mActivePointerId);
                if (index < 0) {
                    Log.e("TwinklingRefreshLayout", "Error processing scroll; pointer index for id " +
                            mActivePointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) e.getX(index);
                final int y = (int) e.getY(index);

                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset)) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    // Updated the nested offsets
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                if (!mIsBeingDragged && Math.abs(dy) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (dy > 0) {
                        dy -= mTouchSlop;
                    } else {
                        dy += mTouchSlop;
                    }
                }

                if (mIsBeingDragged) {
                    mLastTouchY = y - mScrollOffset[1];

                    final int scrolledDeltaY = 0;
                    final int unconsumedY = dy - scrolledDeltaY;
                    if (dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset)) {
                        mLastTouchX -= mScrollOffset[0];
                        mLastTouchY -= mScrollOffset[1];
                        vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                        mNestedOffsets[0] += mScrollOffset[0];
                        mNestedOffsets[1] += mScrollOffset[1];
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopNestedScroll();
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }
        vtev.recycle();
        return true;
    }

    //NestedScroll
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    /************************************** 开放api区  *****************************************/
    //设置默认的header class 名
    public static void setDefaultHeader(String className) {
        HEADER_CLASS_NAME = className;
    }

    //设置默认的footer class 名
    public static void setDefaultFooter(String className) {
        FOOTER_CLASS_NAME = className;
    }

    //主动刷新
    public void startRefresh() {
        cp.startRefresh();
    }

    //主动加载跟多
    public void startLoadMore() {
        cp.startLoadMore();
    }

    /**
     * 结束刷新
     */
    public void finishRefreshing() {
        if (cp.isRefreshing()) {
            cp.finishRefreshing();
        }
    }

    /**
     * 结束加载更多
     */
    public void finishLoadmore() {
        if (cp.isLoadingMore()) {
            cp.finishLoadmore();
        }
    }

    /**
     * 同时结束刷新和加载更多
     */
    public void finishAll(){
        finishRefreshing();
        finishLoadmore();
    }

    /**
     * 手动设置刷新View
     */
    public void setTargetView(View targetView) {
        if (targetView != null) mChildView = targetView;
    }

    /**
     * 手动设置RefreshLayout的装饰器
     */
    public void setDecorator(IDecorator decorator1) {
        if (decorator1 != null) decorator = decorator1;
    }

    /**
     * 设置头部刷新View
     */
    public void setHeaderView(final IHeaderView headerView) {
        if (headerView != null) {
            mHeadLayout.removeAllViewsInLayout();
            mHeadLayout.addView(headerView.getView());
            mHeadView = headerView;
        }
    }

    /**
     * 设置固定在顶部的header
     */
    @Deprecated
    public void addFixedExHeader(final View view) {
        if (view != null && mExtraHeadLayout != null) {
            mExtraHeadLayout.addView(view);
            mExtraHeadLayout.bringToFront();
            if (floatRefresh) mHeadLayout.bringToFront();
            cp.onAddExHead();
            cp.setExHeadFixed();
        }
    }

    /**
     * 获取额外附加的头部
     */
    public View getExtraHeaderView() {
        return mExtraHeadLayout;
    }

    /**
     * 设置底部View
     */
    public void setBottomView(final IBottomView bottomView) {
        if (bottomView != null) {
            mBottomLayout.removeAllViewsInLayout();
            mBottomLayout.addView(bottomView.getView());
            mBottomView = bottomView;
        }
    }

    public void setFloatRefresh(boolean ifOpenFloatRefreshMode) {
        floatRefresh = ifOpenFloatRefreshMode;
        if (!floatRefresh) return;
        post(new Runnable() {
            @Override
            public void run() {
                if (mHeadLayout != null) mHeadLayout.bringToFront();
            }
        });
    }

    /**
     * 设置wave的下拉高度
     */
    public void setMaxHeadHeight(float maxHeightDp) {
        this.mMaxHeadHeight = DensityUtil.dp2px(getContext(), maxHeightDp);
    }

    /**
     * 设置下拉头的高度
     */
    public void setHeaderHeight(float headHeightDp) {
        this.mHeadHeight = DensityUtil.dp2px(getContext(), headHeightDp);
    }

    public void setMaxBottomHeight(float maxBottomHeight) {
        mMaxBottomHeight = DensityUtil.dp2px(getContext(), maxBottomHeight);
    }

    /**
     * 设置底部高度
     */
    public void setBottomHeight(float bottomHeightDp) {
        this.mBottomHeight = DensityUtil.dp2px(getContext(), bottomHeightDp);
    }

    /**
     * 是否允许加载更多
     */
    public void setEnableLoadmore(boolean enableLoadmore1) {
        enableLoadmore = enableLoadmore1;
        if (mBottomView != null) {
            if (enableLoadmore) mBottomView.getView().setVisibility(VISIBLE);
            else mBottomView.getView().setVisibility(GONE);
        }
    }

    /**
     * 是否允许下拉刷新
     */
    public void setEnableRefresh(boolean enableRefresh1) {
        this.enableRefresh = enableRefresh1;
        if (mHeadView != null) {
            if (enableRefresh) mHeadView.getView().setVisibility(VISIBLE);
            else mHeadView.getView().setVisibility(GONE);
        }
    }

    /**
     * 是否允许越界时显示头部刷新控件
     */
    public void setOverScrollTopShow(boolean isOverScrollTopShow) {
        this.isOverScrollTopShow = isOverScrollTopShow;
    }

    /**
     * 是否允许越界时显示底部刷新控件
     */
    public void setOverScrollBottomShow(boolean isOverScrollBottomShow) {
        this.isOverScrollBottomShow = isOverScrollBottomShow;
    }

    /**
     * 是否允许越界时显示刷新控件（setOverScrollTopShow,setOverScrollBottomShow统一设置方法）
     */
    public void setOverScrollRefreshShow(boolean isOverScrollRefreshShow) {
        this.isOverScrollTopShow = isOverScrollRefreshShow;
        this.isOverScrollBottomShow = isOverScrollRefreshShow;
    }

    /**
     * 是否允许开启越界回弹模式
     */
    public void setEnableOverScroll(boolean enableOverScroll1) {
        this.enableOverScroll = enableOverScroll1;
    }

    /**
     * 是否开启纯净的越界回弹模式,开启时刷新和加载更多控件不显示
     */
    public void setPureScrollModeOn() {
        isPureScrollModeOn = true;

        isOverScrollTopShow = false;
        isOverScrollBottomShow = false;
        setMaxHeadHeight(mOverScrollHeight);
        setHeaderHeight(mOverScrollHeight);
        setMaxBottomHeight(mOverScrollHeight);
        setBottomHeight(mOverScrollHeight);
    }

    /**
     * 设置越界高度
     */
    public void setOverScrollHeight(float overScrollHeightDp) {
        this.mOverScrollHeight = DensityUtil.dp2px(getContext(), overScrollHeightDp);
    }

    /**
     * 设置OverScroll时自动加载更多
     *
     * @param ifAutoLoadMore 为true表示底部越界时主动进入加载跟多模式，否则直接回弹
     */
    public void setAutoLoadMore(boolean ifAutoLoadMore) {
        autoLoadMore = ifAutoLoadMore;
        if (!autoLoadMore) return;
        setEnableLoadmore(true);
    }

    public void showRefreshingWhenOverScroll(boolean ifShow) {
        showRefreshingWhenOverScroll = ifShow;
    }

    public void showLoadingWhenOverScroll(boolean ifShow) {
        showLoadingWhenOverScroll = ifShow;
    }

    public void setEnableKeepIView(boolean ifKeep) {
        enableKeepIView = ifKeep;
    }

    /**
     * 设置刷新控件监听器
     */
    private RefreshListenerAdapter refreshListener;

    public void setOnRefreshListener(RefreshListenerAdapter refreshListener) {
        if (refreshListener != null) {
            this.refreshListener = refreshListener;
        }
    }

    @Override
    public void onPullingDown(TwinklingRefreshLayout refreshLayout, float fraction) {
        mHeadView.onPullingDown(fraction, mMaxHeadHeight, mHeadHeight);
        if (!enableRefresh) return;
        if (refreshListener != null) refreshListener.onPullingDown(refreshLayout, fraction);
    }

    @Override
    public void onPullingUp(TwinklingRefreshLayout refreshLayout, float fraction) {
        mBottomView.onPullingUp(fraction, mMaxHeadHeight, mHeadHeight);
        if (!enableLoadmore) return;
        if (refreshListener != null) refreshListener.onPullingUp(refreshLayout, fraction);
    }

    @Override
    public void onPullDownReleasing(TwinklingRefreshLayout refreshLayout, float fraction) {
        mHeadView.onPullReleasing(fraction, mMaxHeadHeight, mHeadHeight);
        if (!enableRefresh) return;
        if (refreshListener != null)
            refreshListener.onPullDownReleasing(refreshLayout, fraction);
    }

    @Override
    public void onPullUpReleasing(TwinklingRefreshLayout refreshLayout, float fraction) {
        mBottomView.onPullReleasing(fraction, mMaxBottomHeight, mBottomHeight);
        if (!enableLoadmore) return;
        if (refreshListener != null) refreshListener.onPullUpReleasing(refreshLayout, fraction);
    }

    @Override
    public void onRefresh(TwinklingRefreshLayout refreshLayout) {
        mHeadView.startAnim(mMaxHeadHeight, mHeadHeight);
        if (refreshListener != null) refreshListener.onRefresh(refreshLayout);
    }

    @Override
    public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
        mBottomView.startAnim(mMaxBottomHeight, mBottomHeight);
        if (refreshListener != null) refreshListener.onLoadMore(refreshLayout);
    }

    @Override
    public void onFinishRefresh() {
        if (refreshListener != null) {
            refreshListener.onFinishRefresh();
        }
        if (!cp.isEnableKeepIView() && !cp.isRefreshing()) return;
        mHeadView.onFinish(new OnAnimEndListener() {
            @Override
            public void onAnimEnd() {
                cp.finishRefreshAfterAnim();
            }
        });
    }

    @Override
    public void onFinishLoadMore() {
        if (refreshListener != null) {
            refreshListener.onFinishLoadMore();
        }
        if (!cp.isEnableKeepIView() && !cp.isLoadingMore()) return;
        mBottomView.onFinish();
    }

    @Override
    public void onRefreshCanceled() {
        if (refreshListener != null) refreshListener.onRefreshCanceled();
    }

    @Override
    public void onLoadmoreCanceled() {
        if (refreshListener != null) refreshListener.onLoadmoreCanceled();
    }


    public class CoContext {
        private AnimProcessor animProcessor;

        private final static int PULLING_TOP_DOWN = 0;
        private final static int PULLING_BOTTOM_UP = 1;
        private int state = PULLING_TOP_DOWN;

        private static final int EX_MODE_NORMAL = 0;
        private static final int EX_MODE_FIXED = 1;
        private int exHeadMode = EX_MODE_NORMAL;

        public CoContext() {
            animProcessor = new AnimProcessor(this);
        }

        public void init() {
            if (isPureScrollModeOn) {
                setOverScrollTopShow(false);
                setOverScrollBottomShow(false);
                if (mHeadLayout != null) mHeadLayout.setVisibility(GONE);
                if (mBottomLayout != null) mBottomLayout.setVisibility(GONE);
            }
        }

        public AnimProcessor getAnimProcessor() {
            return animProcessor;
        }

        public boolean isEnableKeepIView() {
            return enableKeepIView;
        }

        public boolean showRefreshingWhenOverScroll() {
            return showRefreshingWhenOverScroll;
        }

        public boolean showLoadingWhenOverScroll() {
            return showLoadingWhenOverScroll;
        }

        public float getMaxHeadHeight() {
            return mMaxHeadHeight;
        }

        public int getHeadHeight() {
            return (int) mHeadHeight;
        }

        public int getExtraHeadHeight() {
            return mExtraHeadLayout.getHeight();
        }

        public int getMaxBottomHeight() {
            return (int) mMaxBottomHeight;
        }

        public int getBottomHeight() {
            return (int) mBottomHeight;
        }

        public int getOsHeight() {
            return (int) mOverScrollHeight;
        }

        public View getTargetView() {
            return mChildView;
        }

        public View getHeader() {
            return mHeadLayout;
        }

        public View getFooter() {
            return mBottomLayout;
        }

        public int getTouchSlop() {
            return mTouchSlop;
        }

        public void resetHeaderView() {
            if (mHeadView != null) mHeadView.reset();
        }

        public void resetBottomView() {
            if (mBottomView != null) mBottomView.reset();
        }

        public View getExHead() {
            return mExtraHeadLayout;
        }

        public void setExHeadNormal() {
            exHeadMode = EX_MODE_NORMAL;
        }

        public void setExHeadFixed() {
            exHeadMode = EX_MODE_FIXED;
        }

        public boolean isExHeadNormal() {
            return exHeadMode == EX_MODE_NORMAL;
        }

        public boolean isExHeadFixed() {
            return exHeadMode == EX_MODE_FIXED;
        }

        /**
         * 在添加附加Header前锁住，阻止一些额外的位移动画
         */
        private boolean isExHeadLocked = true;

        public boolean isExHeadLocked() {
            return isExHeadLocked;
        }

        //添加了额外头部时触发
        public void onAddExHead() {
            isExHeadLocked = false;
            LayoutParams params = (LayoutParams) mChildView.getLayoutParams();
            params.addRule(BELOW, mExtraHeadLayout.getId());
            mChildView.setLayoutParams(params);
            requestLayout();
        }


        /**
         * 主动刷新、加载更多、结束
         */
        public void startRefresh() {
            post(new Runnable() {
                @Override
                public void run() {
                    setStatePTD();
                    if (!isPureScrollModeOn && mChildView != null) {
                        setRefreshing(true);
                        animProcessor.animHeadToRefresh();
                    }
                }
            });
        }

        public void startLoadMore() {
            post(new Runnable() {
                @Override
                public void run() {
                    setStatePBU();
                    if (!isPureScrollModeOn && mChildView != null) {
                        setLoadingMore(true);
                        animProcessor.animBottomToLoad();
                    }
                }
            });
        }

        public void finishRefreshing() {
            onFinishRefresh();
        }

        public void finishRefreshAfterAnim() {
            if (mChildView != null) {
                animProcessor.animHeadBack(true);
            }
        }

        public void finishLoadmore() {
            onFinishLoadMore();
            if (mChildView != null) {
                animProcessor.animBottomBack(true);
            }
        }

        public boolean enableOverScroll() {
            return enableOverScroll;
        }

        public boolean allowPullDown() {
            return enableRefresh || enableOverScroll;
        }

        public boolean allowPullUp() {
            return enableLoadmore || enableOverScroll;
        }

        public boolean enableRefresh() {
            return enableRefresh;
        }

        public boolean enableLoadmore() {
            return enableLoadmore;
        }

        public boolean allowOverScroll() {
            return (!isRefreshVisible && !isLoadingVisible);
        }

        public boolean isRefreshVisible() {
            return isRefreshVisible;
        }

        public boolean isLoadingVisible() {
            return isLoadingVisible;
        }

        public void setRefreshVisible(boolean visible) {
            isRefreshVisible = visible;
        }

        public void setLoadVisible(boolean visible) {
            isLoadingVisible = visible;
        }

        public void setRefreshing(boolean refreshing) {
            isRefreshing = refreshing;
        }

        public boolean isRefreshing() {
            return isRefreshing;
        }

        public boolean isLoadingMore() {
            return isLoadingMore;
        }

        public void setLoadingMore(boolean loadingMore) {
            isLoadingMore = loadingMore;
        }

        public boolean isOpenFloatRefresh() {
            return floatRefresh;
        }

        public boolean autoLoadMore() {
            return autoLoadMore;
        }

        public boolean isPureScrollModeOn() {
            return isPureScrollModeOn;
        }

        public boolean isOverScrollTopShow() {
            return isOverScrollTopShow;
        }

        public boolean isOverScrollBottomShow() {
            return isOverScrollBottomShow;
        }

        public void onPullingDown(float offsetY) {
            pullListener.onPullingDown(TwinklingRefreshLayout.this, offsetY / mHeadHeight);
        }

        public void onPullingUp(float offsetY) {
            pullListener.onPullingUp(TwinklingRefreshLayout.this, offsetY / mBottomHeight);
        }

        public void onRefresh() {
            pullListener.onRefresh(TwinklingRefreshLayout.this);
        }

        public void onLoadMore() {
            pullListener.onLoadMore(TwinklingRefreshLayout.this);
        }

        public void onFinishRefresh() {
            pullListener.onFinishRefresh();
        }

        public void onFinishLoadMore() {
            pullListener.onFinishLoadMore();
        }

        public void onPullDownReleasing(float offsetY) {
            pullListener.onPullDownReleasing(TwinklingRefreshLayout.this, offsetY / mHeadHeight);
        }

        public void onPullUpReleasing(float offsetY) {
            pullListener.onPullUpReleasing(TwinklingRefreshLayout.this, offsetY / mBottomHeight);
        }

        public boolean dispatchTouchEventSuper(MotionEvent ev) {
            return TwinklingRefreshLayout.super.dispatchTouchEvent(ev);
        }

        public void onRefreshCanceled() {
            pullListener.onRefreshCanceled();
        }

        public void onLoadmoreCanceled() {
            pullListener.onLoadmoreCanceled();
        }

        public void setStatePTD() {
            state = PULLING_TOP_DOWN;
        }

        public void setStatePBU() {
            state = PULLING_BOTTOM_UP;
        }

        public boolean isStatePTD() {
            return PULLING_TOP_DOWN == state;
        }

        public boolean isStatePBU() {
            return PULLING_BOTTOM_UP == state;
        }

        private boolean prepareFinishRefresh = false;
        private boolean prepareFinishLoadMore = false;

        public boolean isPrepareFinishRefresh() {
            return prepareFinishRefresh;
        }

        public boolean isPrepareFinishLoadMore() {
            return prepareFinishLoadMore;
        }

        public void setPrepareFinishRefresh(boolean prepareFinishRefresh) {
            this.prepareFinishRefresh = prepareFinishRefresh;
        }

        public void setPrepareFinishLoadMore(boolean prepareFinishLoadMore) {
            this.prepareFinishLoadMore = prepareFinishLoadMore;
        }
    }
}
