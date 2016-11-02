package app.hitomila;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import app.hitomila.common.HitomiWebView;
import app.hitomila.common.WebViewLoadCompletedCallback;
import app.hitomila.common.exception.wrongHitomiDataException;
import app.hitomila.common.hitomi.IndexData;
import app.hitomila.common.hitomi.hitomiData;


/**
 * 리스트를 보여주는 액티비티, 메인액티비티이다.
 * 초기화면은 Recently Added, 추후 언어별, 태그(1개)별, 좋아요버튼별 등등 으로 꾸밀 수 있게끔 해둠
 */
public class IndexActivity extends AppCompatActivity {
    HitomiWebView view = HitomiWebView.getInstance();
    Drawer navigationDrawer;
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    ProgressBar loadingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //오픈소스인 네비게이션 툴바를 불러온다.
        navigationDrawer = new DrawerBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(false)
                .withActionBarDrawerToggle(false)
                .addDrawerItems(
                        //pass your items here
                )
                .build();
        initCustomActionbar();
        initRecyclerView();
        initView();

        //초기 접속은 이쪽으로. 다되면 리사이클러뷰를 소환
        //view.loadUrl("https://hitomi.la/reader/992458.html", webViewCallback);
        view.loadUrl("https://hitomi.la/index-all-1.html", webViewCallback);
    }

    @Override
    protected void onDestroy() {
        view.clear();
        super.onDestroy();
    }

    //여기서 뷰의 리스너나 할당을 하자
    private void initView(){
        loadingProgress = (ProgressBar)findViewById(R.id.loadingProgressBar);
    }

    //왜인지 모르지만 runOnUiThread가 안먹혀
    private void initRecyclerView(){
        recyclerView = (RecyclerView)findViewById(R.id.indexRecyclerView);
        adapter = new RecyclerViewAdapter(this);//DataSet 연결
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    private void initCustomActionbar(){
        ActionBar actionbar = getSupportActionBar();

        //Actionbar의 속성 설정
        actionbar.setDisplayShowCustomEnabled(true);
        actionbar.setDisplayHomeAsUpEnabled(false);
        actionbar.setDisplayShowTitleEnabled(false);

        //CustomActionbar Inflate
        View mCustomView = LayoutInflater.from(this).inflate(R.layout.activity_main_actionbar, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
//        actionbar.setCustomView(mCustomView);
//
//        Toolbar parent = (Toolbar) mCustomView.getParent();
//        parent.setContentInsetsAbsolute(0,0);


        //button Listener
        TextView title = (TextView) mCustomView.findViewById(R.id.titleTextView);
        ImageView toggleImage = (ImageView) mCustomView.findViewById(R.id.navigationDrawerToggleButton);

        title.setText(R.string.title_recently_added);
        toggleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!navigationDrawer.isDrawerOpen())
                    navigationDrawer.openDrawer();
                else navigationDrawer.closeDrawer();
            }
        });


        actionbar.setCustomView(mCustomView, params);
    }

    private WebViewLoadCompletedCallback webViewCallback = new WebViewLoadCompletedCallback() {

        //index 페이지 데이터 송수신 완료, 파싱완료 후 실행됨
        @Override
        public void onCompleted(final hitomiData data) {
            try {
            if(data instanceof IndexData){
                adapter.setData((IndexData) data);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingProgress.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                });
            } else
                throw new wrongHitomiDataException("IndexActivity", "index 페이지 송신 완료 -> indexData가 아님");
            } catch (wrongHitomiDataException e) {
                Toast.makeText(IndexActivity.this, "수신한 데이터 타입에 문제가 있습니다" , Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        @Override
        public void onStart() {
            loadingProgress.setVisibility(View.VISIBLE);
        }
    };
}