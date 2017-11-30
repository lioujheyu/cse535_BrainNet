package group7.project;

import android.content.DialogInterface;
import android.os.Handler;
import android.preference.DialogPreference;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.DoubleBuffer;
import java.net.URLConnection;
import java.nio.channels.FileLock;
import java.io.*;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

// This class is referred to http://tinyurl.com/or8wql2
class serverConnection {
	public static final int REMOTE = 0;
	public static final int FOG = 1;
	public static final int ADAPTIVE = 2;

	private String serverURL; // should not include any filename
    private String serverPHPfile; // Upload procedure will use this server-side program
    private MainActivity main;


	serverConnection(String serverURL, String serverPHPfile, MainActivity mainActivity) {
		this.serverURL = serverURL;
        this.serverPHPfile = serverPHPfile;
		this.main = mainActivity;
	}

	void uploadFile(String uploadFilePath, String[] uploadFileName, boolean register_or_login, int serverType) {
        URLConnection conn;
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
						Log.d("file error", "file not exist = " + filename);
					else
						Log.d("file success", "file exist = " + filename);

					FileInputStream fileInputStream;
					URL url = new URL(serverURL + "/" + serverPHPfile);

					// Open a HTTP  connection to  the URL
					if (serverType == REMOTE)
						conn = (HttpsURLConnection) url.openConnection();
					else
						conn = (HttpURLConnection)url.openConnection();

					conn.setDoInput(true); // Allow Inputs
					conn.setDoOutput(true); // Allow Outputs
					conn.setUseCaches(false); // Don't use a Cached Copy
					if (serverType == REMOTE)
						((HttpsURLConnection)conn).setRequestMethod("POST");
					else
						((HttpURLConnection)conn).setRequestMethod("POST");
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

					int responseCode;
					if (serverType == REMOTE) {
						responseCode = ((HttpsURLConnection) conn).getResponseCode();
						Log.d("get response", ((HttpsURLConnection) conn).getResponseMessage());
					}
					else {
						responseCode = ((HttpURLConnection) conn).getResponseCode();
						Log.d("get response", ((HttpURLConnection) conn).getResponseMessage());
					}
					if (responseCode == HttpURLConnection.HTTP_OK) {
						Log.d("upload success", "upload success = " + filename);
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
					if (serverType == REMOTE)
						((HttpsURLConnection)conn).disconnect();
					else
						((HttpURLConnection)conn).disconnect();

					final String response = response_buf;
					if (register_or_login) { // Register
						if (response.equals("0")) {
							main.runOnUiThread(new Runnable() {
								public void run() {
									main.stopTime = System.currentTimeMillis();
									Double elapsedTime = (double)(main.stopTime - main.startTime);
									elapsedTime = elapsedTime / 1000;
									main.msgBox.setMessage("Training Completed.\n execution time: " + Double.toString(elapsedTime) + " seconds")
									.setTitle("Training Status").setCancelable(false)
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialogInterface, int i) {

										}
									});
									main.msgBox.create().setCanceledOnTouchOutside(false);
									main.msgBox.show();
								}
							});
							main.registeredUser.clear();
							for(int idx=0; idx<uploadFileName.length; idx++) {
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
									Log.e("PHP execution failed: ", response);
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
							File file = new File(main.db_path + "/result");
							BufferedReader bfr = new BufferedReader(new FileReader(file));
							String line;
							Double tp = 0.0; Double fp = 0.0; Double tn = 0.0; Double fn = 0.0; int testUser = 0;
							bfr.readLine();
							while ((line = bfr.readLine()) != null)
							{
								testUser += 1;
								String[] num = line.split(",");
								if (main.registeredUser.contains(Integer.parseInt(num[1])))
								{
									if (num[0].equals(num[1]))
										tp += 1;
									else
										fn += 1;
								}
								else
								{
									if (num[0].equals("-1"))
										tn += 1;
									else
										fp += 1;
								}
							}
							Double accuracy = (tp+tn) / testUser;
							Double precision = tp / (tp+fp);
							Double recall = tp / (tp+fn);
							Double F1Score = 2*precision*recall / (precision+recall);
							main.stopTime = System.currentTimeMillis();
							Double elapsedTime = (double)(main.stopTime - main.startTime);
							elapsedTime = elapsedTime / 1000;
							main.msgBox.setMessage("Testing Completed.\n execution time: " + Double.toString(elapsedTime) + " seconds\n\n"
							+ "Total test users: " + Integer.toString(testUser) + "\nAccuracy: " + Double.toString(accuracy) + "\nPrecision: "
							+ Double.toString(precision) + "\nRecall: " + Double.toString(recall) + "\nF1 Score: " + Double.toString(F1Score))
									.setTitle("Testing Status").setCancelable(false)
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialogInterface, int i) {

										}
									});
							main.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									main.msgBox.create().setCanceledOnTouchOutside(false);
									main.msgBox.show();
								}
							});

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
