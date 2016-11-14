package app.hitomila.common;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.WindowManager;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

/**
 * Created by admin on 2016-11-01.
 */

public class HitomiApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new TedPermission(this)
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        return;
                    }

                    @Override
                    public void onPermissionDenied(ArrayList<String> arrayList) {
                        Toast.makeText(getApplicationContext(), "권한거부됨\n" + arrayList.toString(), Toast.LENGTH_LONG).show();
                    }
                })
                .setRationaleMessage("인터넷, 파일접근권한이 필요함. 닥치고 오케이를 해")
                .setDeniedMessage("거부시 [설정] > [앱 권한] 에서 따로 허용이 가능.")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE)
                .check();
        NetworkInfo activeNetwork = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        //WIFI = 1, DATA NETWORK = 2 , NOT_CONNECTED = 0;
        if(activeNetwork == null){
            AlertDialog dialog = new AlertDialog.Builder(getBaseContext())
                    .setTitle("인터넷 연결 안됨")
                    .setMessage("인터넷을 연결한 후 재실행 해주세요.")
                    .setNeutralButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    }).create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
            //Toast.makeText(getBaseContext(),"인터넷 연결 후 재실행 해주세요", Toast.LENGTH_LONG).show();
            //System.exit(0);
        }

        Fabric.with(this, new Crashlytics());
        HitomiWebView.init(this);
    }
}
