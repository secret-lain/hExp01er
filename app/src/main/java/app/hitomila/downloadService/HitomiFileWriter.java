package app.hitomila.downloadService;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.hitomila.common.hitomiObjects.ReaderData;

/**
 * Created by admin on 2016-10-12.
 */

public class HitomiFileWriter {
    private ReaderDownloadClient downloader;
    private Context parentContext;
    private String filePath;

    //저장될 디렉토리의 경로를 반환
    public String getFilePath(){
        return filePath;
    }

    public HitomiFileWriter(Context mContext, ReaderData data){
        parentContext = mContext;
        downloader = new ReaderDownloadClient(mContext,  data.getImages());

        filePath = Environment.getExternalStorageDirectory().getPath() + "/hitomi/" + data.title + "/";
        directoryWrite(data.title);
    }

    //저장될 디렉토리의 이름
    private void directoryWrite(String directoryName){
        File directory = new File(filePath);
        if(!directory.exists()){
            boolean created = directory.mkdirs();
            if(created == false){
                Toast.makeText(parentContext, "파일권한이 없습니다. 앱설정을 해주세요", Toast.LENGTH_SHORT).show();
                throw new RuntimeException("directory Creation Failed - directory.mkdirs()");
            }
        }
    }

    public void downloadAll(final DownloadNotifyCallback callback){
        downloader.downloadAll(new DownloadFileWriteCallback() {
            @Override
            public void notifyPageDownloaded(String imageFileName, byte[] binaryImageData) {
                //이미지파일의 다운로드가 된 경우.
                writeImage(imageFileName, binaryImageData);
                callback.notifyPageDownloaded();
            }

            @Override
            public void notifyDownloadCompleted() {
                downloader.interrupt();
                callback.notifyDownloadCompleted();
            }

            @Override
            public void notifyDownloadFailed() {
                downloader.interrupt();
                callback.notifyDownloadFailed();
            }
        });
    }

    //저장될 파일명, 이미지데이터
    public boolean writeImage(String imageName, byte[] binary){
        imageName = parseFileNameToTwoDigits(imageName);
        File image = new File(filePath, imageName);
        FileOutputStream oStream = null;

        try {
            oStream = new FileOutputStream(image);
            oStream.write(binary);
            oStream.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //정렬순서를 맞추기 위해 한자리수의 경우 2자리수로 바꾼다.
    //만약 3자리수 이상일 경우? 다시 해결하도록 한다
    private static String parseFileNameToTwoDigits(String filename){
        if(isOneDigit(filename))
            return "0" + filename;
        return filename;
    }
    private static boolean isOneDigit(String filename){
        String extractImageNameRegex = "[^\\.]*";
        Pattern pattern = Pattern.compile(extractImageNameRegex);
        Matcher match = pattern.matcher(filename);

        if(match.find())
            if(match.group(0).length() == 1) return true;
        return false;
    }

}
