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
		String filename = "", downloadFileName = "result", downloadFilePath = uploadFilePath;
		String ID = "", TASK;
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1024*1024;
		File uploadFile;
		int start, end;
		if(register_or_login == true){
			start = 1;
			end = 10;
		}
		else
		{
			start = 14;
			end = 14;
		}

		final int totalFileIndex = uploadFileName.length * (end-start+1);

		try {
			main.runOnUiThread(new Runnable() {
				public void run() {
					main.mToast.setText(" Uploading...");
					main.mToast.show();
				}
			});
			for(int i = 0; i <= uploadFileName.length; i++) {
				for(int j = start; j <= end; j++) {
					String response_buf = "";
					final int currentFileIndex = i*(end-start+1) + (j-start+1);

					if (i == uploadFileName.length)
						filename = "end";
					else {
						ID = "S" + uploadFileName[i];
						TASK = "R" + String.format("%02d", j);
						filename = ID + "/" + ID + TASK + ".edf";
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
					conn.setRequestProperty("uploaded_file", filename);
					dos = new DataOutputStream(conn.getOutputStream());
					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + filename + "\"" + lineEnd);
					dos.writeBytes(lineEnd);

					if(!filename.equals("end")) {
						// create a buffer of  maximum size
						fileInputStream = new FileInputStream(uploadFile);
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
						fileInputStream.close();
					}


					Log.e("get response", conn.getResponseMessage());
					if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
						Log.e("upload success", "upload success = " + filename);
						String line;
						BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						while ((line = br.readLine()) != null) {
							response_buf += line;
						}
						if (i < uploadFileName.length) {
							main.runOnUiThread(new Runnable() {
								public void run() {
									main.mToast.setText(Integer.toString(currentFileIndex) +
											"/" + Integer.toString(totalFileIndex) + " Uploaded.");
									main.mToast.show();
								}
							});
						}
					}
					dos.flush();
					dos.close();
					conn.disconnect();

					final String response = response_buf;
					if (register_or_login) { // Register
						if (response.equals("0")) {
							main.runOnUiThread(new Runnable() {
								public void run() {
									main.mToast.setText("Training Completed.");
									main.mToast.show();
								}
							});
							for(int idx=0; idx<=uploadFileName.length; idx++) {
								main.registeredUser.add(Integer.parseInt(uploadFileName[idx]));
							}

							break;
						}
						else if (response.equals("success")) {
							continue;
						}
						else {
							main.runOnUiThread(new Runnable() {
								public void run() {
									main.mToast.setText("PHP execution failed: " + response);
									main.mToast.show();
								}
							});
							break;
						}
					}
					else { // Login
						if (response.equals("0")) {
							downloadFile(downloadFilePath, downloadFileName);
							break;
						}
						else if (response.equals("success")) {
							continue;
						}
						else {
							main.runOnUiThread(new Runnable() {
								public void run() {
									main.mToast.setText("PHP execution failed: " + response);
									main.mToast.show();
								}
							});
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			Log.e("connect error", "connect error" + e);
			main.runOnUiThread(new Runnable() {
				public void run() {
					main.mToast.setText("Upload Failed");
					main.mToast.show();
				}
			});
		}
	}

	private void downloadFile(String downloadFilePath, String downloadFileName) {
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
