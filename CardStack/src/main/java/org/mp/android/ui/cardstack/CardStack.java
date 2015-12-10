package org.mp.android.ui.cardstack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import java.util.ArrayList;

public class CardStack<T> extends RelativeLayout {
    private static final String LOG_TAG = "CardStack";
    private int mIndex = 0;
    private int mNumVisible = 4;
    private int mOriginalNumVisible = mNumVisible;
    private boolean mReDraw = false;
    private ArrayAdapter<?> mAdapter;
    private OnTouchListener mOnTouchListener;
    private CardAnimator mCardAnimator;

    private boolean mAnimateRotationCard;
    private boolean mStickNextCardToTop;
    private boolean mSimpleDismissAnimation;
    private boolean mRandomBackgroundColor;

    private boolean mRotateCardDeck;
    private boolean mRestartIndex = false;

    private int chainEnd = 0;
    private int chainLink = 0;
    private int chainDismissDirection;
    private boolean chainingDismiss = false;

    private CardEventListener mEventListener = new DefaultStackEventListener(300);
    private int mContentResource = 0;


    public interface CardEventListener {
        //section
        // 0 | 1
        //--------
        // 2 | 3
        // swipe distance, most likely be used with height and width of a view ;

        boolean swipeEnd(int section, float distance);

        boolean swipeStart(int section, float distance);

        boolean swipeContinue(int section, float distanceX, float distanceY);

        void beforeDiscarded(View cardView);

        void discarded(int mIndex, int direction);

        void topCardTapped();

        void longPressTopCard();
    }

    public boolean isRotateCardDeck() {
        return mRotateCardDeck;
    }

    public void setRotateCardDeck(boolean rotateCardDeck) {
        mRotateCardDeck = rotateCardDeck;
    }

    public boolean isSimpleDismissAnimation() {
        return mSimpleDismissAnimation;
    }

    public void setSimpleDismissAnimation(boolean simpleDismissAnimation) {
        mSimpleDismissAnimation = simpleDismissAnimation;
    }

    public boolean isAnimateRotationCard() {
        return mAnimateRotationCard;
    }

    public void setAnimateRotationCard(boolean animateRotationCard) {
        mAnimateRotationCard = animateRotationCard;
    }

    public boolean isStickNextCardToTop() {
        return mStickNextCardToTop;
    }

    public void setStickNextCardToTop(boolean stickNextCardToTop) {
        this.mStickNextCardToTop = stickNextCardToTop;
    }

    public boolean isRandomBackgroundColor() {
        return mRandomBackgroundColor;
    }

    public void setRandomBackgroundColor(boolean randomBackgroundColor) {
        mRandomBackgroundColor = randomBackgroundColor;
    }

    public void discardTop(final int direction) {
        discardTop(direction, null, 250);
    }

    public void discardTop(final int direction, long duration) {
        discardTop(direction, null, duration);
    }

    public void discardTop(final int direction, final CardStackCallback callback, long duration) {
        mCardAnimator.discard(direction, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator arg0) {
                mCardAnimator.initLayout();
                mIndex++;
                loadLast();

                //Para rotar el arreglo...
                if (mRestartIndex && mIndex > (mAdapter.getCount() - 1)) {
                    mIndex = 0;
                    mRestartIndex = false;
                }

                //Don't call until end animation...
                if (!chainingDismiss) {
                    mEventListener.discarded(mIndex, direction);
                }
                viewCollection.get(0).setOnTouchListener(null);
                viewCollection.get(viewCollection.size() - 1).setOnTouchListener(mOnTouchListener);
                if (null != callback) {
                    callback.onDismissCard();
                }
            }
        }, duration, mEventListener);
    }

    public int getCurrIndex() {
        //sync?
        return mIndex;
    }

    public View findCardViewByItem(T item) {
        View cardView = null;
        int viewIndex = mNumVisible - 1;
        int itemIndex = mIndex;
        for (int i = 0; i < mNumVisible; i++) {
            if (itemIndex >= mAdapter.getCount()) {
                itemIndex = itemIndex - mAdapter.getCount();
            }
            T itemAdapter = (T)mAdapter.getItem(itemIndex);
            if (itemAdapter.equals(item)) {
                cardView = viewCollection.get(viewIndex);
                break;
            }
            itemIndex++;
            viewIndex--;
        }
        return cardView;
    }

    private void chainDiscardTop() {
        discardTop(chainDismissDirection, new CardStackCallback() {
            @Override
            public void onDismissCard() {
                if (chainLink < chainEnd) {
                    chainLink++;
                    chainDismissDirection = chainDismissDirection == CardAnimator.DISMISS_DOWN_RIGHT ? CardAnimator.DISMISS_DOWN_LEFT : CardAnimator.DISMISS_DOWN_RIGHT;
                    if (chainLink == chainEnd) {
                        chainingDismiss = false;
                    }
                    chainDiscardTop();

                }
            }
        }, 200);
    }

    public synchronized void gotoCard(int index, boolean animate) {
        if (index >= 0 && index <= mAdapter.getCount()) {
            if (animate && mRotateCardDeck) {
                int cardsToDismiss = index - mIndex;
                if (cardsToDismiss < 0) {
                    cardsToDismiss = mAdapter.getCount() - mIndex + index;
                }
                if (cardsToDismiss > 0) {
                    chainLink = 1;
                    chainEnd = cardsToDismiss;
                    chainDismissDirection = CardAnimator.DISMISS_DOWN_RIGHT;
                    chainingDismiss = true;
                    chainDiscardTop();
                }
            } else {
                mIndex = index;
                reset(false);
            }
        } else {
            Log.w(LOG_TAG, "Index card to go is out of bounds of deck.");
        }

    }

    //only necessary when I need the attrs from xml, this will be used when inflating layout
    public CardStack(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CardStack);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.CardStack_animateRotationCard:
                    mAnimateRotationCard = a.getBoolean(attr, true);
                    break;
                case R.styleable.CardStack_stickNextCardToTop:
                    mStickNextCardToTop = a.getBoolean(attr, false);
                    break;
                case R.styleable.CardStack_simpleDismissAnimation:
                    mSimpleDismissAnimation = a.getBoolean(attr, false);
                    break;
                case R.styleable.CardStack_rotateCardDeck:
                    mRotateCardDeck = a.getBoolean(attr, false);
                    break;
                case R.styleable.CardStack_randomBackgroundColor:
                    mRandomBackgroundColor = a.getBoolean(attr, false);
                    break;
                default:
                    Log.d("TAG", "Unknown attribute for " + getClass().toString() + ": " + attr);
                    break;
            }
        }
        a.recycle();

        for (int i = 0; i < mNumVisible; i++) {
            addContainerViews();
        }
        setupAnimation();
    }

    private void addContainerViews() {
        FrameLayout v = new FrameLayout(getContext());
        viewCollection.add(v);
        addView(v);
    }

    public void setStackMargin(int margin) {
        mCardAnimator.setStackMargin(margin);
        mCardAnimator.initLayout();
    }

    public void setContentResource(int res) {
        mContentResource = res;
    }

    public void reset(boolean resetIndex) {
        if (resetIndex) mIndex = 0;
        removeAllViews();
        viewCollection.clear();

        for (int i = 0; i < mNumVisible; i++) {
            addContainerViews();
        }
        setupAnimation();
        loadData();
    }

    public void setVisibleCards(int visibleCards) {
        mNumVisible = visibleCards;
        mOriginalNumVisible = mNumVisible;
        if (mAdapter != null) {
            checkRedraw();
            reset(false);
        } else {
            Log.w(LOG_TAG, "Set visible cards after set adapter...");
        }
    }

    public void setThreshold(int t) {
        mEventListener = new DefaultStackEventListener(t);
    }

    public void setListener(CardEventListener cel) {
        mEventListener = cel;
    }

    private void setupAnimation() {
        boolean addDragListener = true;
        final View cardView = viewCollection.get(viewCollection.size() - 1);
        mCardAnimator = new CardAnimator(viewCollection, mAnimateRotationCard, mStickNextCardToTop, mSimpleDismissAnimation, mRandomBackgroundColor);
        mCardAnimator.initLayout();

        if (mRotateCardDeck) {
            if (viewCollection.size() <= 1) {
                addDragListener = false;
            }
        }
        if (addDragListener) {
            final DragGestureDetector dd = new DragGestureDetector(CardStack.this.getContext(), new DragGestureDetector.DragListener() {

                @Override
                public boolean onDragStart(MotionEvent e1, MotionEvent e2,
                                           float distanceX, float distanceY) {
                    mCardAnimator.drag(e1, e2, distanceX, distanceY);
                    return true;
                }

                @Override
                public boolean onDragContinue(MotionEvent e1, MotionEvent e2,
                                              float distanceX, float distanceY) {
                    float x1 = e1.getRawX();
                    float y1 = e1.getRawY();
                    float x2 = e2.getRawX();
                    float y2 = e2.getRawY();
                    //float distance = CardUtils.distance(x1,y1,x2,y2);
                    final int direction = CardUtils.direction(x1, y1, x2, y2);
                    mCardAnimator.drag(e1, e2, distanceX, distanceY);
                    mEventListener.swipeContinue(direction, Math.abs(x2 - x1), Math.abs(y2 - y1));
                    return true;
                }

                @Override
                public boolean onDragEnd(MotionEvent e1, MotionEvent e2) {
                    //reverse(e1,e2);
                    float x1 = e1.getRawX();
                    float y1 = e1.getRawY();
                    float x2 = e2.getRawX();
                    float y2 = e2.getRawY();
                    float distance = CardUtils.distance(x1, y1, x2, y2);
                    final int direction = CardUtils.direction(x1, y1, x2, y2);

                    boolean discard = mEventListener.swipeEnd(direction, distance);
                    if (discard) {
                        discardTop(direction);
                    } else {
                        mCardAnimator.reverse(e1, e2);
                    }
                    return true;
                }

                @Override
                public boolean onTapUp() {
                    mEventListener.topCardTapped();
                    return true;
                }

                @Override
                public void onLongPress() {
                    mEventListener.longPressTopCard();
                }
            }
            );

            mOnTouchListener = new OnTouchListener() {
                private static final String DEBUG_TAG = "MotionEvents";

                @Override
                public boolean onTouch(View arg0, MotionEvent event) {
                    dd.onTouchEvent(event);
                    return true;
                }
            };
            cardView.setOnTouchListener(mOnTouchListener);
        }

    }

    private DataSetObserver mOb = new DataSetObserver() {
        @Override
        public void onChanged() {
            checkRedraw();
            reset(false);
        }
    };

    private void checkRedraw() {
        if (mAdapter.getCount() <= mOriginalNumVisible) {
            mNumVisible = mAdapter.getCount();
            mReDraw = true;
        } else {
            mNumVisible = mOriginalNumVisible;
            mReDraw = true;
        }
    }


    ArrayList<View> viewCollection = new ArrayList<View>();

    public CardStack(Context context) {
        super(context);
    }

    public void setAdapter(final ArrayAdapter<?> adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mOb);
        }
        mAdapter = adapter;
        adapter.registerDataSetObserver(mOb);

        checkRedraw();

        loadData();
    }

    private void loadData() {
        boolean addView = true;

        if (mAdapter.getCount() > 0) {
            if (mReDraw) {
                mReDraw = false;
                removeAllViews();
                viewCollection.clear();

                for (int i = 0; i < mNumVisible; i++) {
                    addContainerViews();
                }
                setupAnimation();
            }
            for (int i = mNumVisible - 1; i >= 0; i--) {
                ViewGroup parent = (ViewGroup) viewCollection.get(i);
                int index = (mIndex + mNumVisible - 1) - i;
                if (index > mAdapter.getCount() - 1) {
                    if (mRotateCardDeck) {
                        //Start from the beginning of the deck
                        index = index - mAdapter.getCount();
                    } else {
                        parent.setVisibility(View.GONE);
                        addView = false;
                    }
                }
                if (addView) {
                    View child = mAdapter.getView(index, getContentView(), this);
                    parent.addView(child);
                    parent.setVisibility(View.VISIBLE);
                }
            }
        }
    }



    private View getContentView() {
        View contentView = null;
        if (mContentResource != 0) {
            LayoutInflater lf = LayoutInflater.from(getContext());
            contentView = lf.inflate(mContentResource, null);
        }
        return contentView;

    }

    private void loadLast() {
        ViewGroup parent = (ViewGroup) viewCollection.get(0);

        int lastIndex = (mNumVisible - 1) + mIndex;
        if (lastIndex > mAdapter.getCount() - 1) {
            if (mRotateCardDeck) {
                lastIndex = lastIndex - mAdapter.getCount();
                mRestartIndex = true;
            } else {
                parent.setVisibility(View.GONE);
                return;
            }
        }

        View child = mAdapter.getView(lastIndex, getContentView(), parent);
        parent.removeAllViews();
        parent.addView(child);
    }

    public int getStackSize() {
        return mNumVisible;
    }

    private interface CardStackCallback {
        void onDismissCard();
    }
}


