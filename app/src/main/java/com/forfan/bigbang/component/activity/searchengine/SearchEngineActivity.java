package com.forfan.bigbang.component.activity.searchengine;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.forfan.bigbang.R;
import com.forfan.bigbang.component.activity.searchengine.listener.OnItemClickListener;
import com.forfan.bigbang.component.activity.searchengine.view.ListViewDecoration;
import com.forfan.bigbang.component.base.BaseActivity;
import com.forfan.bigbang.entity.SearchEngine;
import com.forfan.bigbang.util.ConstantUtil;
import com.forfan.bigbang.util.SearchEngineUtil;
import com.forfan.bigbang.util.SnackBarUtil;
import com.forfan.bigbang.util.UrlCountUtil;
import com.forfan.bigbang.view.Dialog;
import com.forfan.bigbang.view.DialogFragment;
import com.forfan.bigbang.view.SimpleDialog;
import com.shang.commonjar.contentProvider.SPHelper;
import com.shang.utils.StatusBarCompat;
import com.yanzhenjie.recyclerview.swipe.Closeable;
import com.yanzhenjie.recyclerview.swipe.OnSwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenu;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;
import com.yanzhenjie.recyclerview.swipe.touch.OnItemMoveListener;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by wangyan-pd on 2016/11/19.
 */

public class SearchEngineActivity extends BaseActivity {
    private Activity mContext;

    private ArrayList<SearchEngine> searchEngines;

    private MenuAdapter mMenuAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private SwipeMenuRecyclerView swipeMenuRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarCompat.setupStatusBarView(this, (ViewGroup) getWindow().getDecorView(), true, R.color.colorPrimary);
        setContentView(R.layout.activity_search_engine);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.setting_search_engine);

        mContext = this;
        searchEngines = SearchEngineUtil.getInstance().getSearchEngines();
        swipeMenuRecyclerView = (SwipeMenuRecyclerView) findViewById(R.id.recyclerview);
        mLinearLayoutManager = new LinearLayoutManager(this);
        swipeMenuRecyclerView.setLayoutManager(mLinearLayoutManager);// ??????????????????
        swipeMenuRecyclerView.setHasFixedSize(true);// ??????Item???????????????????????????????????????FixSize??????????????????
        swipeMenuRecyclerView.setItemAnimator(new DefaultItemAnimator());// ??????Item??????????????????????????????????????????
        swipeMenuRecyclerView.addItemDecoration(new ListViewDecoration());// ??????????????????

        // ???SwipeRecyclerView???Item??????????????????????????????????????????????????????
        // ????????????????????????
        swipeMenuRecyclerView.setSwipeMenuCreator(swipeMenuCreator);
        // ????????????Item???????????????
        swipeMenuRecyclerView.setSwipeMenuItemClickListener(menuItemClickListener);

        mMenuAdapter = new MenuAdapter(searchEngines);
        mMenuAdapter.setOnItemClickListener(onItemClickListener);
        swipeMenuRecyclerView.setAdapter(mMenuAdapter);

        swipeMenuRecyclerView.setLongPressDragEnabled(true);// ??????????????????????????????????????????
        swipeMenuRecyclerView.setOnItemMoveListener(onItemMoveListener);// ?????????????????????UI???
        findViewById(R.id.add_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UrlCountUtil.onEvent(UrlCountUtil.CLICK_SEARCH_ENGINE_ADD);
                showAddSearchEngineDialog();
            }
        });
    }

    private void showAddSearchEngineDialog() {
        SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight) {
            public EditText editName;
            public EditText editUrl;

            @Override
            protected void onBuildDone(Dialog dialog) {
                super.onBuildDone(dialog);
                dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                editName = (EditText) dialog.findViewById(R.id.search_name);
                editUrl = (EditText) dialog.findViewById(R.id.search_url);

            }

            @Override
            public void onPositiveActionClicked(DialogFragment fragment) {
                // ?????????????????????
                if (TextUtils.isEmpty(editName.getText().toString()) || TextUtils.isEmpty(editUrl.getText().toString())) {
                    SnackBarUtil.show(swipeMenuRecyclerView,getResources().getString(R.string.save_search_engine_fail));
                    return;
                }
                SearchEngineUtil.getInstance().addSearchEngine(new SearchEngine(editName.getText().toString(), editUrl.getText().toString()));
                mMenuAdapter.notifyDataSetChanged();
                SearchEngineUtil.getInstance().save(searchEngines);
                super.onPositiveActionClicked(fragment);
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onCancel(dialog);
            }
        };
        builder.contentView(R.layout.search_engine_edit);
        builder.title(getResources().getString(R.string.add));
        builder.positiveAction(this.getString(R.string.confirm));
        builder.negativeAction(getString(R.string.cancel));
        DialogFragment fragment = DialogFragment.newInstance(builder);
        fragment.show(getSupportFragmentManager(), null);
    }

    /**
     * ???Item??????????????????
     */
    private OnItemMoveListener onItemMoveListener = new OnItemMoveListener() {
        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            // ???Item?????????????????????
            Collections.swap(searchEngines, fromPosition, toPosition);
            mMenuAdapter.notifyItemMoved(fromPosition, toPosition);
            SearchEngineUtil.getInstance().save(searchEngines);
            return true;// ??????true????????????????????????false????????????????????????
        }

        @Override
        public void onItemDismiss(int position) {
            // ???Item?????????????????????????????????????????????????????????????????????????????????????????????
            // ??????Menu???????????????????????????????????????????????????????????????
        }
    };


    /**
     * ??????????????????
     */
    private SwipeMenuCreator swipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
            int width = getResources().getDimensionPixelSize(R.dimen.item_height);

            // MATCH_PARENT ?????????????????????????????????????????????????????????????????????????????????????????????WRAP_CONTENT???
            int height = ViewGroup.LayoutParams.MATCH_PARENT;

            // ??????????????????????????????????????????????????????????????????
            {
                SwipeMenuItem deleteItem = new SwipeMenuItem(mContext)
//                        .setBackgroundDrawable(R.drawable.selector_red)
//                        .setImage(R.mipmap.ic_action_delete)
                        .setBackgroundColor(Color.parseColor("#ff6e40"))
                        .setText(getString(R.string.delete)) // ??????????????????????????????????????????????????????
                        .setTextColor(Color.WHITE)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(deleteItem);// ???????????????????????????????????????

                SwipeMenuItem addItem = new SwipeMenuItem(mContext)
//                        .setBackgroundDrawable(R.drawable.selector_green)
                        .setBackgroundColor(Color.parseColor("#5af158"))
                        .setText(getString(R.string.edit))
                        .setTextColor(Color.WHITE)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(addItem); // ????????????????????????????????????
            }
        }
    };

    private OnItemClickListener onItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(int position) {
            SPHelper.save(ConstantUtil.BROWSER_SELECTION, position);
            SnackBarUtil.show(swipeMenuRecyclerView,getString(R.string.set_search_pre)+ searchEngines.get(position).title+getString(R.string.set_search_end));
            mMenuAdapter.notifyDataSetChanged();
        }
    };

    /**
     * ?????????????????????
     */
    private OnSwipeMenuItemClickListener menuItemClickListener = new OnSwipeMenuItemClickListener() {
        @Override
        public void onItemClick(Closeable closeable, int adapterPosition, int menuPosition, int direction) {
            closeable.smoothCloseMenu();// ???????????????????????????
            switch (menuPosition) {
                case 0:
                    if (searchEngines.size() == 1) {
                        SnackBarUtil.show(swipeMenuRecyclerView, getString(R.string.can_not_delete));
                        return;
                    }
                    UrlCountUtil.onEvent(UrlCountUtil.CLICK_SEARCH_ENGINE_DEL);
                    mMenuAdapter.notifyItemRemoved(adapterPosition);
                    searchEngines.remove(adapterPosition);
                    if (SPHelper.getInt(ConstantUtil.BROWSER_SELECTION, 0) == adapterPosition) {
                        SPHelper.save(ConstantUtil.BROWSER_SELECTION, 0);
                    }
                    SearchEngineUtil.getInstance().save(searchEngines);
                    break;
                case 1:
                    UrlCountUtil.onEvent(UrlCountUtil.CLICK_SEARCH_ENGINE_EDIT);
                    showEidtSearchEngineDialog(adapterPosition);
                    break;
            }
//
//            if (direction == SwipeMenuRecyclerView.RIGHT_DIRECTION) {
//                Toast.makeText(mContext, "list???" + adapterPosition + "; ???????????????" + menuPosition, Toast.LENGTH_SHORT).show();
//            } else if (direction == SwipeMenuRecyclerView.LEFT_DIRECTION) {
//                Toast.makeText(mContext, "list???" + adapterPosition + "; ???????????????" + menuPosition, Toast.LENGTH_SHORT).show();
//            }
        }


    };

    private void showEidtSearchEngineDialog(int adapterPosition) {
        SearchEngine searchEngine = searchEngines.get(adapterPosition);
        SimpleDialog.Builder builder = new SimpleDialog.Builder(R.style.SimpleDialogLight) {
            public EditText editName;
            public EditText editUrl;

            @Override
            protected void onBuildDone(Dialog dialog) {
                super.onBuildDone(dialog);
                dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                editName = (EditText) dialog.findViewById(R.id.search_name);
                editUrl = (EditText) dialog.findViewById(R.id.search_url);
                editName.setText(searchEngine.title);
                editName.setSelection(searchEngine.title.length());
                editUrl.setText(searchEngine.url);

            }

            @Override
            public void onPositiveActionClicked(DialogFragment fragment) {
                // ?????????????????????
                if (TextUtils.isEmpty(editName.getText().toString()) || TextUtils.isEmpty(editUrl.getText().toString())) {
                    SnackBarUtil.show(swipeMenuRecyclerView, getString(R.string.save_search_engine_fail));
                    return;
                }
                searchEngine.url = editUrl.getText().toString();
                searchEngine.title = editName.getText().toString();
                mMenuAdapter.notifyDataSetChanged();
                SearchEngineUtil.getInstance().save(searchEngines);
                super.onPositiveActionClicked(fragment);
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onCancel(dialog);
            }
        };
        builder.contentView(R.layout.search_engine_edit);
        builder.title(getString(R.string.xiugai));
        builder.positiveAction(this.getString(R.string.confirm));
        builder.negativeAction(getString(R.string.cancel));
        DialogFragment fragment = DialogFragment.newInstance(builder);
        fragment.show(getSupportFragmentManager(), null);
    }

}
