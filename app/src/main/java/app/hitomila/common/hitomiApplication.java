package app.hitomila.common;

import android.Manifest;
import android.app.Application;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

/**
 * Created by admin on 2016-11-01.
 */

public class HitomiApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HitomiWebView.init(this);
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
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET)
                .check();
    }
}
