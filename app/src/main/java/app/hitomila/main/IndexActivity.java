package app.hitomila.main;

import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.List;
import java.util.regex.Matcher;

import app.hitomila.HitomiSearch;
import app.hitomila.R;
import app.hitomila.common.BackPressCloseHandler;
import app.hitomila.common.HitomiWebView;
import app.hitomila.common.WebViewLoadCompletedCallback;
import app.hitomila.common.exception.wrongHitomiDataException;
import app.hitomila.common.hitomiObjects.IndexData;
import app.hitomila.downloadService.DownloadServiceDataParser;
import cz.msebera.android.httpclient.Header;


/**
 * 리스트를 보여주는 액티비티, 메인액티비티이다.
 * 초기화면은 Recently Added, 추후 언어별, 태그(1개)별, 좋아요버튼별 등등 으로 꾸밀 수 있게끔 해둠
 */
/*
* note
*
* Tag page에서 태그 관련 String을 뽑아오는 Regex. 이 후 한번더 걸러야 한다
*
* */
public class IndexActivity extends AppCompatActivity {
    Context mContext;
    AsyncHttpClient httpClient;
    Drawer navigationDrawer;
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    ProgressBar loadingProgress;
    TextView actionBarTitle;
    TextView currPageTextView;
    LinearLayoutManager layoutManager;

    BackPressCloseHandler backButtonHandler;

    static int CONNECTURL_MAX = 5;
    static int CONNECTURL_COUNTOUT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        httpClient = new AsyncHttpClient();
        backButtonHandler = new BackPressCloseHandler(this);
        initNavigationDrawer();
        initCustomActionbar();
        initRecyclerView();
        initView();
        //deprecatedTagDataController.init(this);
        HitomiSearch.init(this);

        initIndexAndChangeTitle("Recently Added");
        connectUrl(new ConnectUrlBuilder().getCurrUrl());
    }

    /*
    * 2016-11-14 업데이트.
    * 잦은 에러 발생으로 인해 어플리케이션의 완전 종료시 노티피케이션 전부 삭제.
    * */
    @Override
    protected void onDestroy() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        HitomiWebView.getInstance().clear();
        super.onDestroy();
    }

    private void initIndexAndChangeTitle(String title) {
        actionBarTitle.setText(title);

        new ConnectUrlBuilder().setCurrIndex(1);
        currPageTextView.setText(Integer.toString(ConnectUrlBuilder.getCurrIndex()));
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        backButtonHandler.onBackPressed();
    }

    /**
     * 해당 url로 접속을 시도한다. ImageAddressPrefix(Global)이 없을 경우 초기화해본다.
     * 왜 여기에 넣었을까? 그냥 onCreated에 넣었어야 할것같은데.
     * 일단 잘 돌아가긴 하니까 두자.
     */
    private void connectUrl(String url) {
        Log.d("connectUrl", url + " -> connection..");
        final Toast loading = Toast.makeText(mContext, "페이지 로딩중", Toast.LENGTH_LONG);
        final Toast prefixLoading = Toast.makeText(mContext, "이미지서버 접두사 초기화 중..", Toast.LENGTH_LONG);
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        loading.show();

        httpClient.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                IndexData data = getIndexData(new String(responseBody));
                adapter.setData(data);
                CONNECTURL_COUNTOUT = 0;

                /*
                * 최초 실행 한번만 한다. prefix를 얻기 위해 index-all-1.html의 맨 앞 망가에 자동접속
                * 자바스크립트가 실행된 후의 response를 파싱하여 DownloadServiceDataParser.prefix에 넣는다.
                * */
                if (DownloadServiceDataParser.prefix.equals("")) {
                    String firstGalleryUrl = data.getDatas()[0].plainUrl;
                    String firstReaderUrl = DownloadServiceDataParser.galleryUrlToReaderUrl(firstGalleryUrl);

                    final HitomiWebView webview = HitomiWebView.getInstance();
                    webview.loadUrl(firstReaderUrl, new WebViewLoadCompletedCallback() {
                        @Override
                        public void onCompleted(final String prefix) {
                            if (prefix.equals("")) {
                                //TODO 접두사 초기화 실패시 어떻게 재실행 할 것인가?
                                Toast.makeText(mContext, "접두사 초기화 실패.", Toast.LENGTH_SHORT).show();
                                throw new wrongHitomiDataException("prefix초기화", "왜 안됐지?");
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    prefixLoading.cancel();
                                    Toast.makeText(mContext, "접두사 초기화 완료 : " + prefix, Toast.LENGTH_SHORT).show();
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.cancel();
                        layoutManager.scrollToPosition(0);
                        loadingProgress.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                //index를 띄우지 못했을 경우 무조건 초기상태로 돌아간다

                //어플 실행 후 인터넷을 강제로 끊어서 생긴 경우.
                if (((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()
                        == null) {
                    Toast.makeText(mContext, "인터넷을 연결해 주세요. 5초 후 재접속을 시도합니다.", Toast.LENGTH_LONG).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            connectUrl(ConnectUrlBuilder.getCurrUrl());
                        }
                    }, 5000);
                } else {
                    Toast.makeText(mContext, "인덱스 페이지 로딩 오류, 재시도 횟수 : " + ++CONNECTURL_COUNTOUT, Toast.LENGTH_SHORT).show();
                    if (CONNECTURL_COUNTOUT >= CONNECTURL_MAX) {
                        Toast.makeText(mContext, "어플리케이션에 문제가 있습니다. 프로그램을 종료합니다.", Toast.LENGTH_LONG).show();
                        finish();
                    } else
                        connectUrl(ConnectUrlBuilder.getCurrUrl());
                }

            }
        });
    }

    //30으로 내려봄 2016-11-05
    private IndexData getIndexData(String html) {
        IndexData result = new IndexData();

        String indexCrawlRegex =
                "<div class=\"([^\"]*)" +
                        "[^\\w]*a href=\"([^\"]*)" +
                        "\"[^\\r\\n]*[\\r\\n].*src=\"([^\"]*)" +
                        "(?:[^\\r\\n]*[\\r\\n]){2,4}.*html\">([^<]*)" +
                        "(?:[^\\r\\n]*[\\r\\n]){1,30}.*Language<\\/td><td>([^\\r\\n]*)";
        Matcher matcher = DownloadServiceDataParser.getMatcher(indexCrawlRegex, html);


        while (matcher.find()) {
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
    private void initView() {
        loadingProgress = (ProgressBar) findViewById(R.id.loadingProgressBar);

        currPageTextView = (TextView) findViewById(R.id.currPageTextView);
        currPageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(mContext)
                        .title("인덱스 직접입력")
                        .content("이동하려는 페이지 번호 직접 입력")
                        .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                        .input("숫자를 입력하세요", "", new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                try {
                                    int inputCustomIndex = Integer.parseInt(input.toString());
                                    if (inputCustomIndex >= 1) {
                                        new ConnectUrlBuilder().setCurrIndex(inputCustomIndex);
                                        currPageTextView.setText(ConnectUrlBuilder.getCurrIndex());
                                        connectUrl(ConnectUrlBuilder.getCurrUrl());
                                    } else
                                        Toast.makeText(mContext, "잘못된 숫자형식입니다", Toast.LENGTH_SHORT).show();
                                } catch (NumberFormatException e) {
                                    //딱히 뭘 안해도 되는 구간이다.
                                }
                            }
                        }).show();
            }
        });
        //아래 반투면 레이아웃(리모콘)쪽
        ImageView leftArrow = (ImageView) findViewById(R.id.leftArrowImageView);
        leftArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currIndex = ConnectUrlBuilder.getCurrIndex();
                if (currIndex > 1) {
                    connectUrl(new ConnectUrlBuilder().decreaseCurrIndex().build());
                    currPageTextView.setText(Integer.toString(currIndex-1));
                } else
                    Toast.makeText(mContext, "첫페이지 입니다", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView rightArrow = (ImageView) findViewById(R.id.rightArrowImageView);
        rightArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currIndex = ConnectUrlBuilder.getCurrIndex();
                currPageTextView.setText(Integer.toString(currIndex+1));
                connectUrl(new ConnectUrlBuilder().increaseCurrIndex().build());
            }
        });
    }

    //왜인지 모르지만 runOnUiThread가 안먹혀
    private void initRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.indexRecyclerView);
        adapter = new RecyclerViewAdapter(this);//DataSet 연결
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager((getApplicationContext()));
        recyclerView.setLayoutManager(layoutManager);
    }

    //메인 액티비티의 커스텀 액션바에 대한 설정
    private void initCustomActionbar() {
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
                if (!navigationDrawer.isDrawerOpen())
                    navigationDrawer.openDrawer();
                else navigationDrawer.closeDrawer();
            }
        });


        actionbar.setCustomView(mCustomView, params);
    }

    //네비게이션 드로어 세팅
    private void initNavigationDrawer() {
        //오픈소스인 네비게이션 툴바를 불러온다.
        PrimaryDrawerItem dummy = new PrimaryDrawerItem();
        SecondaryDrawerItem initSelect = new SecondaryDrawerItem().withIdentifier(0).withName("Home");
        SecondaryDrawerItem indexSelect = new SecondaryDrawerItem().withIdentifier(1).withName("Index");
        SecondaryDrawerItem koreanSelect = new SecondaryDrawerItem().withIdentifier(2).withName("Korean");
        SecondaryDrawerItem japaneseSelect = new SecondaryDrawerItem().withIdentifier(3).withName("Japanese");
        SecondaryDrawerItem chineseSelect = new SecondaryDrawerItem().withIdentifier(4).withName("Chinese");
        SecondaryDrawerItem englishSelect = new SecondaryDrawerItem().withIdentifier(5).withName("English");


        PrimaryDrawerItem tagSearch = new PrimaryDrawerItem().withIdentifier(1000).withName("태그로 검색");
        PrimaryDrawerItem artistsSearch = new PrimaryDrawerItem().withIdentifier(1001).withName("작가명으로 검색");
        PrimaryDrawerItem characterSearch = new PrimaryDrawerItem().withIdentifier(1002).withName("캐릭터명으로 검색");

        navigationDrawer = new DrawerBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(false)
                .withActionBarDrawerToggle(false)
                .addDrawerItems(
                        //TODO 네비게이션 드로어 메뉴 컨텐츠 이쪽에 삽입
                        dummy,
                        initSelect,
                        indexSelect,
                        koreanSelect,
                        japaneseSelect,
                        chineseSelect,
                        englishSelect,
                        new DividerDrawerItem(),
                        tagSearch,
                        artistsSearch,
                        characterSearch
                )
                .withMultiSelect(false)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        navigationDrawer.closeDrawer();
                        switch (position) {
                            /*
                            * 1~5 = Language Change
                            * 7, 8, 9 = specific index search
                            * */
                            case 1:
                                initIndexAndChangeTitle("Recently Added");
                                connectUrl(new ConnectUrlBuilder().setCurrLocation("index").setCurrLangauage("all").build());
                                break;
                            case 2:
                                initIndexAndChangeTitle("Index - " + ConnectUrlBuilder.getCurrLocation().replaceFirst("[^:]+:", ""));
                                connectUrl(new ConnectUrlBuilder().setCurrLangauage("all").build());
                                break;
                            case 3:
                                initIndexAndChangeTitle("Korean - " + ConnectUrlBuilder.getCurrLocation().replaceFirst("[^:]+:", ""));
                                connectUrl(new ConnectUrlBuilder().setCurrLangauage("korean").build());
                                break;
                            case 4:
                                initIndexAndChangeTitle("Japanese - " + ConnectUrlBuilder.getCurrLocation().replaceFirst("[^:]+:", ""));
                                connectUrl(new ConnectUrlBuilder().setCurrLangauage("japanese").build());
                                break;
                            case 5:
                                initIndexAndChangeTitle("Chinese - " + ConnectUrlBuilder.getCurrLocation().replaceFirst("[^:]+:", ""));
                                connectUrl(new ConnectUrlBuilder().setCurrLangauage("chinese").build());
                                break;
                            case 6:
                                initIndexAndChangeTitle("English - " + ConnectUrlBuilder.getCurrLocation().replaceFirst("[^:]+:", ""));
                                connectUrl(new ConnectUrlBuilder().setCurrLangauage("english").build());
                                break;
                            case 8://tagSearch
                                searchMode("tag");
                                return false;
                            case 9://artistSearch
                                searchMode("artist");
                                return false;
                            case 10://characterSearch
                                searchMode("character");
                                return false;
                            default:
                                return false;
                        }
                        return false;
                    }

                    private void searchMode(final String type){
                        if(HitomiSearch.isReady()){
                            final List<String> tagList = HitomiSearch.getTagList(type);
                            if(tagList == null){
                                Toast.makeText(mContext, "태그별 검색에 문제가있습니다. 재실행해 주세요.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            MaterialDialog searchDialog = new MaterialDialog.Builder(mContext)
                                    .title("키워드입력")
                                    .customView(R.layout.activity_main_searchdialog, true)
                                    .positiveText("확인")
                                    .negativeText("취소")
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            View view = dialog.getCustomView();
                                            AutoCompleteTextView searchText = (AutoCompleteTextView)view.findViewById(R.id.autoCompleteTextView);

                                            String listString = searchText.getText().toString();

                                            if(!tagList.contains(listString)){
                                                Toast.makeText(mContext, "잘못된 태그입니다.", Toast.LENGTH_SHORT).show();
                                            }else{
                                                listString = listString.replaceFirst(":[ ]+", ":");
                                                searchText.setText(listString);
                                                connectUrl(new ConnectUrlBuilder().setCurrLocation(type + "/" + listString).build());
                                                String title = ConnectUrlBuilder.getCurrLanguage().replaceFirst("all","Index") + " - " + ConnectUrlBuilder.getCurrLocation().replaceFirst("[^:]+:", "");
                                                initIndexAndChangeTitle(title.substring(0,1).toUpperCase() + title.substring(1,title.length()));
                                            }
                                        }
                                    })
                                    .build();
                            //setSoftInputMode는 화면이 잘려도 자동완성 드롭다운에 스크롤을 넣기 위함
                            searchDialog.show();
                            searchDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                            //Dialog 내 AutoCompleteTextView 세팅
                            View view = searchDialog.getCustomView();
                            AutoCompleteTextView searchText = (AutoCompleteTextView)view.findViewById(R.id.autoCompleteTextView);
                            searchText.setThreshold(1);
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), R.layout.support_simple_spinner_dropdown_item,tagList);
                            searchText.setAdapter(adapter);
                        } else{
                            Toast.makeText(mContext, "태그별 검색이 준비중입니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .build();
    }
}