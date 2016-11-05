package app.hitomila;

import android.content.Context;
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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;

import java.util.regex.Matcher;

import app.hitomila.common.HitomiWebView;
import app.hitomila.common.WebViewLoadCompletedCallback;
import app.hitomila.common.exception.wrongHitomiDataException;
import app.hitomila.common.hitomi.IndexData;
import app.hitomila.services.DownloadServiceDataParser;
import cz.msebera.android.httpclient.Header;


/**
 * 리스트를 보여주는 액티비티, 메인액티비티이다.
 * 초기화면은 Recently Added, 추후 언어별, 태그(1개)별, 좋아요버튼별 등등 으로 꾸밀 수 있게끔 해둠
 */
public class IndexActivity extends AppCompatActivity {
    Context mContext;
    AsyncHttpClient httpClient;
    Drawer navigationDrawer;
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    ProgressBar loadingProgress;
    TextView actionBarTitle;
    int currIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
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

        httpClient = new AsyncHttpClient();
        //초기 접속은 이쪽으로. 다되면 리사이클러뷰를 소환
        //view.loadUrl("https://hitomi.la/reader/992458.html", webViewCallback);
        connectUrl("https://hitomi.la/index-all-1.html");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void connectUrl(String url){
        final Toast loading = Toast.makeText(mContext, "페이지 로딩중", Toast.LENGTH_LONG);
        final Toast prefixLoading = Toast.makeText(mContext, "이미지서버 접두사 초기화 중..", Toast.LENGTH_LONG);
//        final Toast completed = Toast.makeText(mContext, "초기화 완료", Toast.LENGTH_SHORT);
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        loading.show();
        httpClient.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                IndexData data = getIndexData(new String(responseBody));
                adapter.setData(data);

                //최초 실행 한번만 한다. prefix를 얻기 위해 index-all-1.html의 맨 앞 망가에 자동접속
                //자바스크립트가 실행된 후의 response를 파싱하여 DownloadServiceDataParser.prefix에 넣는다.
                if(DownloadServiceDataParser.prefix.equals("")){
                    String firstGalleryUrl = data.getDatas()[0].plainUrl;
                    String firstReaderUrl = DownloadServiceDataParser.galleryUrlToReaderUrl(firstGalleryUrl);
                    HitomiWebView.getInstance().loadUrl(firstReaderUrl, new WebViewLoadCompletedCallback() {
                        @Override
                        public void onCompleted(final String prefix) {
                            if(prefix.equals(""))
                                throw new wrongHitomiDataException("prefix초기화", "왜 안됐지?");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    prefixLoading.cancel();
                                    Toast.makeText(mContext, "접두사 초기화 완료 : " + prefix, Toast.LENGTH_SHORT).show();
                                    loadingProgress.setVisibility(View.GONE);
                                    adapter.notifyDataSetChanged();
                                    recyclerView.setVisibility(View.VISIBLE);
                                }
                            });
                        }

                        @Override
                        public void onStart() {
                            loading.cancel();
                            prefixLoading.show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                //index를 띄우지 못했을 경우 무조건 초기상태로 돌아간다
                Toast.makeText(mContext, "인덱스 페이지 로딩 오류", Toast.LENGTH_SHORT).show();
                connectUrl("https://hitomi.la/index-all-1.html");
            }
        });
    }

    //TODO 문제가 있어서 몇개는 잘리기도 한다. 나중에 다시 수정하는것으로. {2,4} {1,50} 문제인듯
    //30으로 내려봄 2016-11-05
    private IndexData getIndexData(String html){
        IndexData result = new IndexData();

        String indexCrawlRegex =
                "<div class=\"([^\"]*)" +
                        "[^\\w]*a href=\"([^\"]*)" +
                        "\"[^\\r\\n]*[\\r\\n].*src=\"([^\"]*)" +
                        "(?:[^\\r\\n]*[\\r\\n]){2,4}.*html\">([^<]*)" +
                        "(?:[^\\r\\n]*[\\r\\n]){1,30}.*Language<\\/td><td>([^\\r\\n]*)";
        Matcher matcher = DownloadServiceDataParser.getMatcher(indexCrawlRegex, html);


        while(matcher.find()){
            String type = matcher.group(1);
            String plainUrl = matcher.group(2);
            String thumbnailUrl = matcher.group(3);
            String title = matcher.group(4);
            String lang = matcher.group(5);

            result.add(title, type, lang, plainUrl, thumbnailUrl);
        }

        return result;
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

    //메인 액티비티의 커스텀 액션바에 대한 설정
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
        //액션바타이틀의 경우는 밖에서도 변경이 필요할 수 있기 때문에 외부로 뺐음음
        actionBarTitle = (TextView) mCustomView.findViewById(R.id.titleTextView);
        ImageView toggleImage = (ImageView) mCustomView.findViewById(R.id.navigationDrawerToggleButton);

        actionBarTitle.setText(R.string.title_recently_added);
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
}