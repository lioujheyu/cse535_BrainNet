package group7.project;

import android.os.Handler;
import android.util.Log;
import android.view.ViewDebug;
import android.widget.Toast;
import android.text.TextUtils;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileLock;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

// This class is referred to http://tinyurl.com/or8wql2
class serverConnection {
	private String serverURL; // should not include any filename
    private String serverPHPfile; // Upload procedure will use this server-side program
    private MainActivity main;

	serverConnection(String serverURL, String serverPHPfile, MainActivity mainActivity) {
		this.serverURL = serverURL;
        this.serverPHPfile = serverPHPfile;
		this.main = mainActivity;
	}

	void uploadFile(String uploadFilePath, String[] uploadFileName, boolean register_or_login) {
        HttpsURLConnection conn;
		DataOutputStream dos;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		String filename = "", downloadFileName = "ttttest", downloadFilePath = uploadFilePath;
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1024*1024;
		File uploadFile;
		int start, end;
		if(register_or_login == true){
			start = 1;
			end = 13;
		}
		else
		{
			start = 14;
			end = 14;
		}

		try {
			main.runOnUiThread(new Runnable() {
				public void run() {
					final Toast mToast = Toast.makeText(main, " Uploading...", Toast.LENGTH_SHORT);
					mToast.show();
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mToast.cancel();
						}
					}, 500);
				}
			});
			for(int i = 0; i <= uploadFileName.length; i++) {
				for(int j = start; j <= end; j++) {
					String response = "";
					final int x = i*(end-start+1)+(j-start+1), l = uploadFileName.length * (end-start+1);

					if (i == uploadFileName.length)
						filename = "end";
					else {
						filename = "S"
								+ new String(new char[3 - uploadFileName[i].length()]).replace("\0", "0")
								+ uploadFileName[i] + "R"
								+ new String(new char[2 - Integer.toString(j).length()]).replace("\0", "0")
								+ Integer.toString(j) + ".edf";
					}

					uploadFile = new File(uploadFilePath + "/" + filename);

					if (!uploadFile.exists())
						Log.e("file error", "file not exist = " + filename);
					else
						Log.e("file success", "file exist = " + filename);

					FileInputStream fileInputStream;
					URL url = new URL(serverURL + "/" + serverPHPfile);

					// Open a HTTP  connection to  the URL
					conn = (HttpsURLConnection) url.openConnection();
					conn.setDoInput(true); // Allow Inputs
					conn.setDoOutput(true); // Allow Outputs
					conn.setUseCaches(false); // Don't use a Cached Copy
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Connection", "Keep-Alive");
					conn.setRequestProperty("ENCTYPE", "multipart/form-data");
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

					// open a URL connection to the Server
					fileInputStream = new FileInputStream(uploadFile);
					conn.setRequestProperty("uploaded_file", filename);
					dos = new DataOutputStream(conn.getOutputStream());
					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + filename + "\"" + lineEnd);
					dos.writeBytes(lineEnd);

					// create a buffer of  maximum size
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];

					// read file and write it into form...
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);

					while (bytesRead > 0) {
						dos.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					}

					// send multipart form data necessary after file data...
					dos.writeBytes(lineEnd);
					dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


					Log.e("get response", conn.getResponseMessage());
					if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
						Log.e("upload success", "upload success = " + filename);
						String line;
						BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						while ((line = br.readLine()) != null) {
							response += line;
						}
						if (i < uploadFileName.length) {
							main.runOnUiThread(new Runnable() {
								public void run() {
									final Toast mToast = Toast.makeText(main, Integer.toString(x) + "/" + Integer.toString(l) + " Uploaded.", Toast.LENGTH_SHORT);
									mToast.show();
									Handler handler = new Handler();
									handler.postDelayed(new Runnable() {
										@Override
										public void run() {
											mToast.cancel();
										}
									}, 500);
								}
							});
						}
					}

					fileInputStream.close();
					dos.flush();
					dos.close();
					conn.disconnect();
					if (response.equals("end") && register_or_login == true) {
						main.runOnUiThread(new Runnable() {
							public void run() {
								final Toast mToast = Toast.makeText(main, "Training Completed.", Toast.LENGTH_SHORT);
								mToast.show();
							}
						});
						break;
					}
					if(response.equals("end") && register_or_login == false)
					{
						try {
							url = new URL(serverURL + "/" + downloadFileName);
							conn = (HttpsURLConnection) url.openConnection();
							//int responseCode = conn.getResponseCode();

							File localPath = new File(downloadFilePath);
							if (!localPath.exists())
								localPath.mkdirs();

							FileOutputStream outputStream =
									new FileOutputStream(downloadFilePath + "/" + downloadFileName);
							// Lock the file in case it is also used by the database
							FileLock filelock = outputStream.getChannel().lock();

							InputStream is = conn.getInputStream();
							buffer = new byte[4096];
							int current;
							while ((current = is.read(buffer)) != -1) {
								outputStream.write(buffer, 0, current);
							}

							filelock.release();
							outputStream.close();
							conn.disconnect();
							main.runOnUiThread(new Runnable() {
								public void run() {
									final Toast mToast = Toast.makeText(main, "Download Completed", Toast.LENGTH_SHORT);
									mToast.show();
								}
							});
						}
						catch (Exception e) {
							final String errMsg = e.toString();
							main.runOnUiThread(new Runnable() {
								public void run() {
									final Toast mToast = Toast.makeText(main, "Download Failed", Toast.LENGTH_SHORT);
									mToast.show();
								}
							});
						}
						break;
					}
				}
			}

		} catch (Exception e) {
			Log.e("connect error", "connect error" + e);
			main.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(main, "Upload Failed.", Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	void downloadFile(String downloadFilePath, String downloadFileName) {
		HttpURLConnection conn;
		try {
			URL url = new URL(serverURL + "/" + downloadFileName);
			conn = (HttpURLConnection) url.openConnection();
			int responseCode = conn.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				final String responseMessage = conn.getResponseMessage();
				main.runOnUiThread(new Runnable() {
					public void run() {
						main.mToast.setText(responseMessage);
						main.mToast.show();
					}
				});
				return;
			}

			File localPath = new File(downloadFilePath);
			if (!localPath.exists())
				localPath.mkdirs();

			FileOutputStream outputStream =
					new FileOutputStream(downloadFilePath + "/" + downloadFileName);
			// Lock the file in case it is also used by the database
			FileLock filelock = outputStream.getChannel().lock();

			InputStream is = conn.getInputStream();
			byte[] buffer = new byte[4096];
			int current;
			while ((current = is.read(buffer)) != -1) {
				outputStream.write(buffer, 0, current);
			}

			filelock.release();
			outputStream.close();
			conn.disconnect();
			main.runOnUiThread(new Runnable() {
				public void run() {
					main.mToast.setText("Download completed.");
					main.mToast.show();
				}
			});
		}
		catch (Exception e) {
			final String errMsg = e.toString();
			main.runOnUiThread(new Runnable() {
				public void run() {
					main.mToast.setText(errMsg);
					main.mToast.show();
				}
			});
		}
	}
}
