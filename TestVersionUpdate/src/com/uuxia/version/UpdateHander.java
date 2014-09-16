package com.uuxia.version;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.uuxia.version.UpdateModel.Info;

public class UpdateHander {
	protected static Context mContext;
	protected static UpdateModel mUpdateModel;
	public final static int TO_SHOW_UPADTE_MSG = 0;
	public final static int TO_SHOW_DOWNLOAD_FINISH = 2;
	public final static int TO_SHOW_DOWNLOAD_ERROR = 3;
	/**
	 * 存储卡的路径.
	 */
	public static final String STORAGE_PATH = Environment
			.getExternalStorageDirectory().getPath();

	public static void checkVersion(Context c, final String addrPth) {
		mContext = c;
		if (isConnect(mContext)) {
			final int nCurVerCode = getVerCode(mContext);
			new Thread() {
				public void run() {
					if (addrPth == null)
						return;
					JSONObject json = getJsonContent(addrPth);// "http://192.168.1.142:8000/version.json"
					if (json != null) {
						UpdateModel mUpdateModel = parseJson(json);
						int nNewVerCode = mUpdateModel.getVerCode();
						if (nNewVerCode > nCurVerCode) {
							sendMsg(TO_SHOW_UPADTE_MSG, mUpdateModel);
						}
					} else {
						sendMsg(TO_SHOW_DOWNLOAD_ERROR, "获取配置文件失败");
					}
				}
			}.start();
		}
	}

	private static Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (!Thread.currentThread().isInterrupted()) {
				switch (msg.what) {
				case TO_SHOW_DOWNLOAD_FINISH:
					installApk();
					break;
				case TO_SHOW_DOWNLOAD_ERROR:
					String error = msg.obj.toString();
					tips(error);
					break;
				case TO_SHOW_UPADTE_MSG:
					if (msg.obj != null && msg.obj instanceof UpdateModel) {
						mUpdateModel = (UpdateModel) msg.obj;
					}
					showDialogMsg();
					break;
				}
			}
			super.handleMessage(msg);
		}

	};

	protected static void tips(CharSequence msg) {
		Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
	}

	protected static void installApk() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		String strDirName = STORAGE_PATH + File.separator
				+ mContext.getPackageName() + File.separator;
		intent.setDataAndType(Uri.fromFile(new File(strDirName, getFileName(mUpdateModel
				.getDownloadUrl()))), "application/vnd.android.package-archive");
		mContext.startActivity(intent);
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}
	/**
	 * 创建目录，若目录已存则不做操作.
	 * 
	 * @param strDir
	 *            目录的路径
	 */
	public static void createDir(final String strDir) {
		if (strDir == null) {
			return;
		}
		File file = new File(strDir);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	protected static void showDialogMsg() {
		String msg = "有新版本更新哦";
		String title = "新版本更新内容";
		String ok = "马上更新";
		String no = "以后再说";
		if (mUpdateModel != null) {
			if (mUpdateModel.getContent() != null
					&& mUpdateModel.getContent().size() > 0) {
				msg = "";
				for (Info item : mUpdateModel.getContent()) {
					msg += item.getId() + " . " + item.getText() + "\n";
				}
			}
			if (mUpdateModel.getForce() == 1) {
				msg += "\n注意" + " : " + "本次有重大修改，不更新无法使用";
				ok = "立即更新";
				no = null;
			}
		}
		Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setTitle(title);
		dialog.setInverseBackgroundForced(true);
		dialog.setMessage(msg);
		dialog.setCancelable(false);
		dialog.setPositiveButton(ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				downLoadApk();
				dialog.dismiss();
			}
		});
		if (no != null) {
			dialog.setNegativeButton(no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			});
		}
		dialog.create();
		dialog.show();

	}

	/*
	 * 从服务器中下载APK
	 */
	protected static void downLoadApk() {
		final ProgressDialog nDownProgressDialog; // 进度条对话框
		nDownProgressDialog = new ProgressDialog(mContext);
		nDownProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		nDownProgressDialog.setMessage("正在下载更新");
		nDownProgressDialog.setCancelable(false);
		nDownProgressDialog.setCanceledOnTouchOutside(false);
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			sendMsg(TO_SHOW_DOWNLOAD_ERROR, "需要SD卡才能升级哦~");
		} else {
			nDownProgressDialog.show();
			new Thread() {
				public void run() {
					loadFile(mUpdateModel.getDownloadUrl(), nDownProgressDialog);
				}
			}.start();
		}
	}

	/*
	 * 从服务器中下载APK
	 */
	public static void loadFile(String url, ProgressDialog dialog) {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse response;
		try {
			response = client.execute(get);
			HttpEntity entity = response.getEntity();
			float length = entity.getContentLength();
			dialog.setMax((int) length);
			InputStream is = entity.getContent();
			FileOutputStream fileOutputStream = null;
			if (is != null) {
				String fileName = getFileName(url);
				String strDirName = STORAGE_PATH + File.separator
						+ mContext.getPackageName() + File.separator;
				createDir(strDirName);
				File file = new File(strDirName,fileName);
				fileOutputStream = new FileOutputStream(file);
				byte[] buf = new byte[1024];
				int ch = -1;
				int count = 0;
				while ((ch = is.read(buf)) != -1) {
					fileOutputStream.write(buf, 0, ch);
					count += ch;
					dialog.setProgress(count);
				}
			}
			dialog.dismiss();
			sendMsg(TO_SHOW_DOWNLOAD_FINISH, null);
			if (fileOutputStream != null) {
				fileOutputStream.flush();
				fileOutputStream.close();
			}
			if (is != null) {
				is.close();
			}
		} catch (Exception e) {
			sendMsg(TO_SHOW_DOWNLOAD_ERROR, "下载异常");
		}
	}

	// 解析多个数据的Json
	protected static UpdateModel parseJson(JSONObject json) {
		UpdateModel updateInfo = new UpdateModel();
		try {
			JSONArray jsonArray = json.getJSONArray("content");
			int force = json.getInt("force");
			int verCode = json.getInt("verCode");
			String verName = json.getString("verName");
			String downLoadUrl = json.getString("downloadUrl");
			updateInfo.setForce(force);
			updateInfo.setVerCode(verCode);
			updateInfo.setVerName(verName);
			updateInfo.setDownloadUrl(downLoadUrl);
			List<Info> content = new ArrayList<UpdateModel.Info>();
			for (int i = 0; i < jsonArray.length(); i++) {
				Info item = new Info();
				JSONObject jsonObj = jsonArray.getJSONObject(i);
				int id = jsonObj.getInt("id");
				String text = jsonObj.getString("text");
				item.setId(id);
				item.setText(text);
				content.add(item);
			}
			updateInfo.setContent(content);
		} catch (JSONException e) {
			sendMsg(TO_SHOW_DOWNLOAD_ERROR, "Jsons parse error !");
			e.printStackTrace();
		}
		return updateInfo;
	}

	protected static void sendMsg(int id, Object obj) {
		Message msg = mHandler.obtainMessage();
		msg.what = id;
		msg.obj = obj;
		mHandler.sendMessage(msg);
	}

	protected static boolean isConnect(Context context) {
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (info != null && info.isConnected()) {
				if (info.getState() == NetworkInfo.State.CONNECTED) {
					return true;
				}
			}
		}
		return false;
	}

	protected static String getFileName(String path) {
		return path.substring(path.lastIndexOf("/") + 1);
	}

	protected static JSONObject getJsonContent(String path) {
		JSONObject jsonObj = null;
		try {
			URL url = new URL(path);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setConnectTimeout(3000);
			connection.setRequestMethod("GET");
			connection.setDoInput(true); // 从服务器获得数据
			int responseCode = connection.getResponseCode();
			if (200 == responseCode) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				InputStream inputStream = connection.getInputStream();
				byte[] data = new byte[1024];
				int len = 0;
				while ((len = inputStream.read(data)) != -1) {
					outputStream.write(data, 0, len);
				}
				String jsonString = new String(outputStream.toByteArray(),
						"GB2312");
				inputStream.close();
				outputStream.close();
				jsonObj = new JSONObject(jsonString);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObj;
	}

	/*
	 * 获取当前程序的版本号
	 */
	protected String getVersionName(Context context)
			throws NameNotFoundException {
		// 获取packagemanager的实例
		PackageManager packageManager = context.getPackageManager();
		// getPackageName()是你当前类的包名，0代表是获取版本信息
		PackageInfo packInfo = packageManager.getPackageInfo(
				context.getPackageName(), 0);
		return packInfo.versionName;
	}

	public static int getVerCode(Context context) {
		int verCode = -1;
		try {
			verCode = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
		}
		return verCode;
	}
}
