package com.forfan.bigbang.component.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.forfan.bigbang.R;
import com.forfan.bigbang.baseCard.DividerItemDecoration;
import com.forfan.bigbang.component.activity.share.ShareAppManagerActivity;
import com.forfan.bigbang.component.base.BaseActivity;
import com.forfan.bigbang.util.ConstantUtil;
import com.forfan.bigbang.util.UrlCountUtil;
import com.forfan.bigbang.util.ViewUtil;
import com.forfan.bigbang.view.BigBangLayout;
import com.forfan.bigbang.view.BigBangLayoutWrapper;
import com.shang.commonjar.contentProvider.SPHelper;
import com.shang.utils.StatusBarCompat;

/**
 * Created by penglu on 2016/11/9.
 */

public class SettingBigBangActivity extends BaseActivity {
    private static final int BIGBANG_BACKGROUND_COLOR_ARRAY_RES=R.array.bigbang_background_color;

    private static final int MIN_TEXT_SIZE = 8;
    private static final int MAX_TEXT_SIZE = 25;

    private static final int MIN_LINE_MARGIN = (int) ViewUtil.dp2px(0);
    private static final int MAX_LINE_MARGIN = (int) ViewUtil.dp2px(25);


    private static final int MIN_ITEM_MARGIN = (int) ViewUtil.dp2px(0);
    private static final int MAX_ITEM_MARGIN = (int) ViewUtil.dp2px(20);


    private static final int MIN_ITEM_PADDING = (int) ViewUtil.dp2px(2);
    private static final int MAX_ITEM_PADDING = (int) ViewUtil.dp2px(25);

    private CardView cardView;
    private BigBangLayoutWrapper mBigBangLayoutWrap;
    private BigBangLayout mBigBangLayout;
    private SeekBar mTextSizeSeekBar;
    private SeekBar mLineMarginSeekBar;
    private SeekBar mItemMarginSeekBar;
    private SeekBar mItemPaddingSeekBar;

    private TextView textSize, lineMargin, itemMargin,itemPadding;
    private TextView bigbangAlpha;
    private SeekBar mBigbangAlphaSeekBar;

    private CheckBox isFullScreen;
    private CheckBox isStickHeader;
    private CheckBox isDefaultLocal;
    private CheckBox autoAddBlanks;
    private CheckBox isBlankSymbol;

    private RecyclerView backgroundRV;
    private int[] bigbangBackgroungColors;
    private int lastPickedColor;//??????rgb
    private int alpha;//??????alpha???0-100
    private CheckBox isStickSharebar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_bigbang);

        StatusBarCompat.setupStatusBarView(this, (ViewGroup) getWindow().getDecorView(), true, R.color.colorPrimary);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.setting_bigbang);


        mBigBangLayout = (BigBangLayout) findViewById(R.id.bigbang);
        mBigBangLayoutWrap = (BigBangLayoutWrapper) findViewById(R.id.bigbang_wrap);
        cardView = (CardView) findViewById(R.id.bigbang_wraper);

        mTextSizeSeekBar = (SeekBar) findViewById(R.id.set_text_size);
        mLineMarginSeekBar = (SeekBar) findViewById(R.id.set_line_margin);
        mItemMarginSeekBar = (SeekBar) findViewById(R.id.set_item_margin);
        mItemPaddingSeekBar = (SeekBar) findViewById(R.id.set_item_padding);
        mBigbangAlphaSeekBar = (SeekBar) findViewById(R.id.set_bigbang_alpha);

        textSize = (TextView) findViewById(R.id.text_size);
        lineMargin = (TextView) findViewById(R.id.line_margin);
        itemMargin = (TextView) findViewById(R.id.item_margin);
        itemPadding = (TextView) findViewById(R.id.item_padding);
        bigbangAlpha = (TextView) findViewById(R.id.bigbang_alpha);

        backgroundRV= (RecyclerView) findViewById(R.id.bigbang_background);


        isFullScreen= (CheckBox) findViewById(R.id.is_full_screen);
        isStickHeader= (CheckBox) findViewById(R.id.is_stick_header);
        isStickSharebar= (CheckBox) findViewById(R.id.is_stick_sharebar);
        isDefaultLocal = (CheckBox) findViewById(R.id.is_default_local);
        autoAddBlanks = (CheckBox) findViewById(R.id.auto_add_blanks);
        isBlankSymbol = (CheckBox) findViewById(R.id.is_blank_symbol);


        mTextSizeSeekBar.setMax(MAX_TEXT_SIZE - MIN_TEXT_SIZE);
        mLineMarginSeekBar.setMax(MAX_LINE_MARGIN - MIN_LINE_MARGIN);
        mItemMarginSeekBar.setMax(MAX_ITEM_MARGIN - MIN_ITEM_MARGIN);
        mItemPaddingSeekBar.setMax(MAX_ITEM_PADDING - MIN_ITEM_PADDING);
        mItemMarginSeekBar.setMax(100);
        findViewById(R.id.setting_share_apps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SettingBigBangActivity.this, ShareAppManagerActivity.class);
                startActivity(intent);
            }
        });

        mTextSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (int) (MIN_TEXT_SIZE + progress);
                mBigBangLayout.setTextSize(value);
                textSize.setText(getString(R.string.setting_text_size) + value);
                SPHelper.save(ConstantUtil.TEXT_SIZE, value);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_TEXT_SIZE,value+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mLineMarginSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (int) (MIN_LINE_MARGIN + progress);
                mBigBangLayout.setLineSpace(value);
                lineMargin.setText(getString(R.string.setting_line_margin) + value);
                SPHelper.save(ConstantUtil.LINE_MARGIN, value);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_LINE_MARGIN,value+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mItemMarginSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (int) (MIN_ITEM_MARGIN + progress);
                mBigBangLayout.setItemSpace(value);
                itemMargin.setText(getString(R.string.setting_item_margin) + value);
                SPHelper.save(ConstantUtil.ITEM_MARGIN, value);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_ITEM_MARGIN,value+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        mItemPaddingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (int) (MIN_ITEM_PADDING + progress);
                mBigBangLayout.setTextPadding(value);
                itemPadding.setText(getString(R.string.setting_item_padding) + value);
                SPHelper.save(ConstantUtil.ITEM_PADDING, value);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_ITEM_PADDING,value+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mBigbangAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (int) (  progress);
//                mBigBangLayoutWrap.setBackgroundColorWithAlpha(lastPickedColor,value);
                cardView.setCardBackgroundColor(Color.argb((int) ((alpha / 100.0f) * 255), Color.red(lastPickedColor), Color.green(lastPickedColor), Color.blue(lastPickedColor)));
                bigbangAlpha.setText(getString(R.string.setting_alpha_percent) + value +"%");
                SPHelper.save(ConstantUtil.BIGBANG_ALPHA, value);
                alpha=value;
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_ALPHA,value+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        isFullScreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPHelper.save(ConstantUtil.IS_FULL_SCREEN,isChecked);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_FULL_SCREEN,isChecked+"");
            }
        });

        isStickHeader.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPHelper.save(ConstantUtil.IS_STICK_HEADER,isChecked);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_STICK_HEAD,isChecked+"");
                mBigBangLayoutWrap.setStickHeader(isChecked);
            }
        });

        isDefaultLocal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPHelper.save(ConstantUtil.DEFAULT_LOCAL,isChecked);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_DEFAULT_LOCAL,isChecked+"");
            }
        });
        autoAddBlanks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPHelper.save(ConstantUtil.AUTO_ADD_BLANKS,isChecked);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_ADD_BLANKS,isChecked+"");
            }
        });
        isBlankSymbol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPHelper.save(ConstantUtil.TREAT_BLANKS_AS_SYMBOL,isChecked);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_BLANKS_IS_SYMBOL,isChecked+"");
            }
        });
        isStickSharebar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SPHelper.save(ConstantUtil.IS_STICK_SHAREBAR,isChecked);
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_STICK_SHAREBAR,isChecked+"");
            }
        });




        int text = SPHelper.getInt(ConstantUtil.TEXT_SIZE, ConstantUtil.DEFAULT_TEXT_SIZE);
        int line = SPHelper.getInt(ConstantUtil.LINE_MARGIN, ConstantUtil.DEFAULT_LINE_MARGIN);
        int item = SPHelper.getInt(ConstantUtil.ITEM_MARGIN, ConstantUtil.DEFAULT_ITEM_MARGIN);
        int padding = SPHelper.getInt(ConstantUtil.ITEM_PADDING, (int) ViewUtil.dp2px(ConstantUtil.DEFAULT_ITEM_PADDING));
        alpha = SPHelper.getInt(ConstantUtil.BIGBANG_ALPHA, 100);
        lastPickedColor = SPHelper.getInt(ConstantUtil.BIGBANG_DIY_BG_COLOR, Color.parseColor("#94a4bb"));
        boolean fullScreen=SPHelper.getBoolean(ConstantUtil.IS_FULL_SCREEN,false);
        isFullScreen.setChecked(fullScreen);
        boolean stickHeader=SPHelper.getBoolean(ConstantUtil.IS_STICK_HEADER,false);
        isStickHeader.setChecked(stickHeader);
        boolean stickSharebar=SPHelper.getBoolean(ConstantUtil.IS_STICK_SHAREBAR,true);
        isStickSharebar.setChecked(stickSharebar);
        boolean remainSymbol = SPHelper.getBoolean(ConstantUtil.REMAIN_SYMBOL, true);


        boolean defaultLocal = SPHelper.getBoolean(ConstantUtil.DEFAULT_LOCAL, false);
        boolean addBlanks = SPHelper.getBoolean(ConstantUtil.AUTO_ADD_BLANKS, false);
        boolean blankIsSymbol = SPHelper.getBoolean(ConstantUtil.TREAT_BLANKS_AS_SYMBOL, true);


        mTextSizeSeekBar.setProgress((int) ((MIN_TEXT_SIZE)));
        mLineMarginSeekBar.setProgress((int) ((MIN_LINE_MARGIN)));
        mItemMarginSeekBar.setProgress((int) ((MIN_ITEM_MARGIN)));
        mItemPaddingSeekBar.setProgress((int) ((MIN_ITEM_PADDING)));

        mTextSizeSeekBar.setProgress((int) ((MAX_TEXT_SIZE)));
        mLineMarginSeekBar.setProgress((int) ((MAX_LINE_MARGIN)));
        mItemMarginSeekBar.setProgress((int) ((MAX_ITEM_MARGIN)));
        mItemPaddingSeekBar.setProgress((int) ((MIN_ITEM_PADDING)));

        mTextSizeSeekBar.setProgress((int) ((text - MIN_TEXT_SIZE)));
        mLineMarginSeekBar.setProgress((int) ((line - MIN_LINE_MARGIN)));
        mItemMarginSeekBar.setProgress((int) ((item - MIN_ITEM_MARGIN)));
        mItemPaddingSeekBar.setProgress((int) ((padding-MIN_ITEM_PADDING)));

        bigbangAlpha.setText(getString(R.string.setting_alpha_percent) + alpha +"%");

//        mBigBangLayoutWrap.setBackgroundColorWithAlpha(lastPickedColor,alpha);
        cardView.setCardBackgroundColor(Color.argb((int) ((alpha / 100.0f) * 255), Color.red(lastPickedColor), Color.green(lastPickedColor), Color.blue(lastPickedColor)));
        mBigbangAlphaSeekBar.setProgress(alpha);

        mBigBangLayoutWrap.setStickHeader(stickHeader);
        mBigBangLayoutWrap.setShowSymbol(remainSymbol);
        mBigBangLayoutWrap.setShowSection(SPHelper.getBoolean(ConstantUtil.REMAIN_SECTION, false));
        mBigBangLayoutWrap.setActionListener(bigBangActionListener);

        String[] txts = new String[]{"BigBang", "??????", "??????","??????", "??????", "???","\n",
                "???","??????", "??????",  "??????", "???", "??????","???"};

        for (String t : txts) {
            mBigBangLayout.addTextItem(t);
        }
        applyColor(lastPickedColor);

        isDefaultLocal.setChecked(defaultLocal);
        autoAddBlanks.setChecked(addBlanks);
        isBlankSymbol.setChecked(blankIsSymbol);
        bigbangBackgroungColors=getResources().getIntArray(BIGBANG_BACKGROUND_COLOR_ARRAY_RES);
        backgroundRV.setLayoutManager(new GridLayoutManager(this,4));
        backgroundRV.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.GRID_LIST));
        backgroundRV.setAdapter(backgroundColorAdapter);

        mBigBangLayoutWrap.onSwitchType(defaultLocal);
    }

    private RecyclerView.Adapter backgroundColorAdapter=new RecyclerView.Adapter() {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ColorVIewHolder(new TextView(SettingBigBangActivity.this));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextView view= (TextView) holder.itemView;
            view.setMinimumHeight((int) ViewUtil.dp2px(40));
            if (position==bigbangBackgroungColors.length){
                view.setBackgroundColor(getResources().getColor(R.color.white));
                view.setText(R.string.set_background_myself);
                view.setTextColor(getResources().getColor(R.color.black));
                view.setTextSize(14);
                view.setGravity(Gravity.CENTER);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showColorPickDialog();
                        UrlCountUtil.onEvent(UrlCountUtil.CLICK_SET_BB_BGCOLOR_DIY);
                    }
                });
            }else {
                view.setText("");
                view.setBackgroundColor(bigbangBackgroungColors[position]);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (bigbangBackgroungColors.length>position) {
                            applyColor(bigbangBackgroungColors[position]);
                            lastPickedColor=bigbangBackgroungColors[position];
                            SPHelper.save(ConstantUtil.BIGBANG_DIY_BG_COLOR,bigbangBackgroungColors[position] );
                            UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_BGCOLOR,lastPickedColor+"");
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return bigbangBackgroungColors.length+1;
        }

        class ColorVIewHolder extends RecyclerView.ViewHolder{
            public ColorVIewHolder(View itemView) {
                super(itemView);
            }
        }

    };

    private void applyColor(int color){
//        mBigBangLayoutWrap.setBackgroundColorWithAlpha(color,alpha);
        cardView.setCardBackgroundColor(Color.argb((int) ((alpha / 100.0f) * 255), Color.red(color), Color.green(color), Color.blue(color)));
    }


    private void applyColor(int color,int alpha){
//        mBigBangLayoutWrap.setBackgroundColorWithAlpha(color,alpha);
        cardView.setCardBackgroundColor(Color.argb((int) ((alpha / 100.0f) * 255), Color.red(color), Color.green(color), Color.blue(color)));
    }

    private void showColorPickDialog(){
        ColorPickerDialogBuilder
                .with(this)
                .setTitle(R.string.set_background_myself)
                .initialColor(lastPickedColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
                        applyColor(Color.rgb(Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor)),(int) (Color.alpha(selectedColor)*100.0/255));
                    }
                })
                .setPositiveButton(R.string.confirm, new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        lastPickedColor=Color.rgb(Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor));
                        alpha= (int) (Color.alpha(selectedColor)*100.0/255);
                        applyColor(Color.rgb(Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor)));
                        SPHelper.save(ConstantUtil.BIGBANG_DIY_BG_COLOR,lastPickedColor );
                        UrlCountUtil.onEvent(UrlCountUtil.STATUS_SET_BB_BGCOLOR,lastPickedColor+"");
                        mBigbangAlphaSeekBar.setProgress(alpha);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        applyColor(lastPickedColor);
                    }
                })
                .showColorEdit(true)
                .setColorEditTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_bright))
                .build()
                .show();
    }


    BigBangLayoutWrapper.ActionListener bigBangActionListener = new BigBangLayoutWrapper.ActionListener() {

        @Override
        public void onSelected(String text) {
        }

        @Override
        public void onSearch(String text) {
        }

        @Override
        public void onShare(String text) {
        }

        @Override
        public void onCopy(String text) {
        }

        @Override
        public void onTrans(String text) {
        }

        @Override
        public void onDrag() {
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_BIGBANG_DRAG);
        }

        @Override
        public void onSwitchType(boolean isLocal) {
            //
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_BIGBANG_SWITCH_TYPE);
            mBigBangLayout.reset();
            if (isLocal) {
                String[] txts = new String[]{"BigBang", "???","???", "???","???","???","???", "???","???", "???","\n",
                        "???","???","???", "???","???",  "???","???", "???", "???","???","???"};

                for (String t : txts) {
                    mBigBangLayout.addTextItem(t);
                }
            } else {
                String[] txts = new String[]{"BigBang", "??????", "??????","??????", "??????", "???","\n",
                        "???","??????", "??????",  "??????", "???", "??????","???"};

                for (String t : txts) {
                    mBigBangLayout.addTextItem(t);
                }
            }
        }

        @Override
        public void onSwitchSymbol(boolean isShow) {
            SPHelper.save(ConstantUtil.REMAIN_SYMBOL, isShow);
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_BIGBANG_REMAIN_SYMBOL);
        }

        @Override
        public void onSwitchSection(boolean isShow) {
            SPHelper.save(ConstantUtil.REMAIN_SECTION, isShow);
            UrlCountUtil.onEvent(UrlCountUtil.CLICK_BIGBANG_REMAIN_SECTION);
        }

        @Override
        public void onDragSelection() {

        }
    };

}
