package group7.project;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.CheckBox;
import android.os.Handler;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.Activity;
import android.widget.ListView;
import android.content.res.TypedArray;
import java.util.ArrayList;
import java.util.List;
import android.widget.AdapterView;
import android.os.SystemClock;
import android.util.Log;
import android.support.v4.app.ActivityCompat;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineDataSet;

public class MainActivity extends AppCompatActivity {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static final int REMOTE = 0;
    public static final int FOG = 1;
    public int ADAPTIVE = 2;

    public class Item {
        boolean checked;
        String ItemString;
        Item( String t, boolean b){
            ItemString = t;
            checked = b;
        }

        public boolean isChecked(){
            return checked;
        }
    }

    static class ViewHolder {
        CheckBox checkBox;
        TextView text;
    }

    public class ItemsListAdapter extends BaseAdapter {

        private Context context;
        private List<Item> list;

        ItemsListAdapter(Context c, List<Item> l) {
            context = c;
            list = l;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public boolean isChecked(int position) {
            return list.get(position).checked;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            // reuse views
            ViewHolder viewHolder = new ViewHolder();
            if (rowView == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                rowView = inflater.inflate(R.layout.row, null);

                viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.rowCheckBox);
                viewHolder.text = (TextView) rowView.findViewById(R.id.rowTextView);
                rowView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) rowView.getTag();
            }

            viewHolder.checkBox.setChecked(list.get(position).checked);

            final String itemStr = list.get(position).ItemString;
            viewHolder.text.setText(itemStr);

            viewHolder.checkBox.setTag(position);
            viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean newState = !list.get(position).isChecked();
                    list.get(position).checked = newState;
                }
            });

            viewHolder.checkBox.setChecked(isChecked(position));

            return rowView;
        }
    }
    List<Item> items;
    ListView listView;
    ItemsListAdapter myItemsListAdapter;


    public Toast mToast;
    private Button registerbutton, loginbutton, statisticsbutton;
    private RadioGroup radioGroup;
    private String serverchoose;
    public int serverType;
    public ArrayList<Integer> registeredUser, TP, FP, FN, TN;
    public long startTime, stopTime;
    public AlertDialog.Builder msgBox;
    public ArrayList<Float> remoteTrainP, fogTrainP, remoteTestP, fogTestP;
    public Double remoteTrainT, fogTrainT, remoteTestT, fogTestT;
    BatteryManager bm;
    Intent batteryIntent;

    String db_path = Environment.getExternalStorageDirectory() + "/Android/data/PROJECT_DATA";
    String remote_serverURL = "https://www.lioujheyu.com";
    String fog_serverURL = "http://en4109601l.cidse.dhcp.asu.edu";
    String serverURL = remote_serverURL;

    String train_serverPHPfile = "train_UploadToServer.php";
    String test_serverPHPfile = "test_UploadToServer.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        msgBox = new AlertDialog.Builder(this);
        mToast = Toast.makeText(MainActivity.this,"",Toast.LENGTH_SHORT);
        registeredUser = new ArrayList<Integer>();
        TP = new ArrayList<Integer>();
        FP = new ArrayList<Integer>();
        FN = new ArrayList<Integer>();
        TN = new ArrayList<Integer>();
        verifyStoragePermissions(this);
        remoteTrainP = new ArrayList<>();
        fogTrainP = new ArrayList<>();
        remoteTestP = new ArrayList<>();
        fogTestP = new ArrayList<>();
        remoteTrainT = 0.0;
        remoteTestT = 0.0;
        fogTrainT = 0.0;
        fogTestT = 0.0;
        bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        jump_to_page_1();
    }

    @Override
    public void onBackPressed() {
        jump_to_page_1();
    }

    private void initItems(ArrayList<Integer> tp, ArrayList<Integer> fp, ArrayList<Integer> fn, ArrayList<Integer> tn){
        items = new ArrayList<Item>();
        // Get item list from arrays.xml
        TypedArray arrayText = getResources().obtainTypedArray(R.array.restext);

        for(int i = 0; i < arrayText.length(); i++){
            String s = arrayText.getString(i);
            if (tp.contains(Integer.parseInt(s)))
                s = s + ": Succeed login";
            else if(fp.contains(Integer.parseInt(s)))
                s = s + ": Failed denied";
            else if(fn.contains(Integer.parseInt(s)))
                s = s + ": Failed login";
            else if(tn.contains(Integer.parseInt(s)))
                s = s + ": Succeed denied";
            boolean b = false;
            Item item = new Item(s, b);
            items.add(item);
        }
        tp.clear(); fp.clear(); fn.clear(); tn.clear();
        arrayText.recycle();
    }


    public void jump_to_register_server(final int serverType){
        setContentView(R.layout.upload_layout);
        if (serverType == REMOTE)
            setTitle("Register - Remote Server");
        else if (serverType == FOG)
            setTitle("Register - Fog Server");

        listView = (ListView)findViewById(R.id.listview);

        initItems(TP,FP,FN,TN);
        myItemsListAdapter = new ItemsListAdapter(this, items);
        listView.setAdapter(myItemsListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(MainActivity.this,
                        ((Item)(parent.getItemAtPosition(position))).ItemString,
                        Toast.LENGTH_SHORT).show();
            }});

        Button uploadbtn= (Button)findViewById(R.id.button3);
        Button jumpbackbtn= (Button)findViewById(R.id.button4);

        uploadbtn.setOnClickListener(new Button.OnClickListener() {
            List<String> CheckBoxlist = new ArrayList<>();
            String[] CheckBoxarray;
            @Override
            public void onClick(View arg0) {
                startTime = System.currentTimeMillis();
                for (int i=0; i<items.size(); i++){
                    if (items.get(i).isChecked()){
                        // Add leading zeros to the string
                        CheckBoxlist.add(String.format("%03d", i+1));
                    }
                }

                CheckBoxarray = CheckBoxlist.toArray(new String[CheckBoxlist.size()]);

                new Thread(new Runnable() {
                    public void run() {
                        serverConnection conn = new serverConnection(serverURL, train_serverPHPfile, MainActivity.this);
                        conn.uploadFile(db_path, CheckBoxarray, true, serverType);  // true => register
                    }
                }).start();
                CheckBoxlist.clear();
            }
        });
        jumpbackbtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                jump_to_page_1();
            }
        });

    }

    public void jump_to_login_server(final int serverType){
        setContentView(R.layout.test_layout);
        listView = (ListView)findViewById(R.id.listview);
        if (serverType == REMOTE)
            setTitle("Login - Remote Server");
        else if (serverType == FOG)
            setTitle("Login - Fog Server");

        initItems(TP,FP,FN,TN);
        myItemsListAdapter = new ItemsListAdapter(this, items);
        listView.setAdapter(myItemsListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(MainActivity.this,
                        ((Item)(parent.getItemAtPosition(position))).ItemString,
                        Toast.LENGTH_SHORT).show();
            }});

        Button uploadbtn= (Button)findViewById(R.id.button3);
        Button jumpbackbtn= (Button)findViewById(R.id.button4);

        uploadbtn.setOnClickListener(new Button.OnClickListener() {
            List<String> CheckBoxlist = new ArrayList<>();
            String[] CheckBoxarray;
            @Override
            public void onClick(View arg0) {
                for (int i=0; i<items.size(); i++){
                    startTime = System.currentTimeMillis();
                    if (items.get(i).isChecked()){
                        // Add leading zeros to the string
                        CheckBoxlist.add(String.format("%03d", i+1));
                    }
                }

                CheckBoxarray = CheckBoxlist.toArray(new String[CheckBoxlist.size()]);

                new Thread(new Runnable() {
                    public void run() {
                        serverConnection conn = new serverConnection(serverURL, test_serverPHPfile, MainActivity.this);
                        conn.uploadFile(db_path, CheckBoxarray, false, serverType); // false => login
                    }
                }).start();
                CheckBoxlist.clear();
            }
        });
        jumpbackbtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                jump_to_page_1();
            }
        });

    }

    public void jump_to_page_1() {
        setContentView(R.layout.activity_main);
        setTitle(getString(R.string.app_name));

        registerbutton = (Button) findViewById(R.id.button);
        loginbutton = (Button) findViewById(R.id.button2);
        statisticsbutton = (Button) findViewById(R.id.button5);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.check(R.id.radioButton2);
        serverchoose = ((RadioButton) findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString();

        registerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverchoose = ((RadioButton) findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString();
                if(serverchoose.equals("Remote server")) {
                    serverURL = remote_serverURL;
                    serverType = REMOTE;
                }
                else if(serverchoose.equals("Fog server")) {
                    serverURL = fog_serverURL;
                    serverType = FOG;
                }
                else if(serverchoose.equals("Adaptive")) {
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            serverConnection conn = new serverConnection(serverURL, test_serverPHPfile, MainActivity.this);
                            if (conn.testFile(db_path+"/S001/", "S001R14.edf") == 0) {
                                serverURL = remote_serverURL;
                                serverType = REMOTE;
                                ADAPTIVE = 0;
                            }
                            else {
                                serverURL = fog_serverURL;
                                serverType = FOG;
                                ADAPTIVE = 1;
                            }
                        }
                    });
                    t.start();
                    try {t.join();} catch(InterruptedException e){}
                }
                jump_to_register_server(serverType);
            }
        });

        loginbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverchoose = ((RadioButton) findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString();
                if(serverchoose.equals("Remote server")) {
                    serverURL = remote_serverURL;
                    serverType = REMOTE;
                    jump_to_login_server(serverType);

                }
                else if(serverchoose.equals("Fog server")) {
                    serverURL = fog_serverURL;
                    serverType = FOG;
                    jump_to_login_server(serverType);
                }
                else if(serverchoose.equals("Adaptive")) {
                    serverConnection conn = new serverConnection(serverURL, test_serverPHPfile, MainActivity.this);
                    if (ADAPTIVE == 0) {
                        serverURL = remote_serverURL;
                        serverType = REMOTE;
                    }
                    else if (ADAPTIVE == 1) {
                        serverURL = fog_serverURL;
                        serverType = FOG;
                    }

                }
                jump_to_login_server(serverType);
            }
        });

        statisticsbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.sta_layout);
                TextView text1, text2, text3, text4;
                TextView sta1, sta2, sta3, sta4;

                text1 = new TextView(getBaseContext());
                text3 = new TextView(getBaseContext());
                text4 = new TextView(getBaseContext());
                sta1 = (TextView) findViewById(R.id.textSta1);
                sta2 = (TextView) findViewById(R.id.textSta2);
                sta3 = (TextView) findViewById(R.id.textSta3);
                sta4 = (TextView) findViewById(R.id.textSta4);
                LineChart chart1 = (LineChart) findViewById(R.id.chart1);
                LineChart chart2 = (LineChart) findViewById(R.id.chart2);
                LineChart chart3 = (LineChart) findViewById(R.id.chart3);
                LineChart chart4 = (LineChart) findViewById(R.id.chart4);

                if (remoteTrainP.isEmpty()) {
                    sta1.setText("Remote Training: You haven't done a remote server training.");
                }
                else {
                    float sum = 0;
                    for (int i = 0; i < remoteTrainP.size(); i++)
                        sum += remoteTrainP.get(i);
                    sta1.setText("Remote Training: " + Double.toString(remoteTrainT) + "(sec), Energy: " + Float.toString(sum) + "(J)");

                    List<Entry> entries = new ArrayList<Entry>();
                    for (int i = 0; i < remoteTrainP.size(); i++) {
                        entries.add(new Entry(i,remoteTrainP.get(i)));
                    }
                    LineDataSet dataSet = new LineDataSet(entries, "Energy Consumption(J)");
                    LineData lineData = new LineData(dataSet);
                    Description description = new Description();
                    description.setText("Remote Training Energy");
                    chart1.setDescription(description);
                    chart1.setData(lineData);
                    chart1.invalidate();

                }
                if (fogTrainP.isEmpty()) {
                    sta2.setText("Fog Training: You haven't done a fog server training.");
                }
                else {
                    float sum = 0;
                    for (int i = 0; i < fogTrainP.size(); i++)
                        sum += fogTrainP.get(i);
                    sta2.setText("Fog Training: " + Double.toString(fogTrainT) + "(sec), Energy: " + Float.toString(sum) + "(J)");
                    List<Entry> entries = new ArrayList<Entry>();
                    for (int i = 0; i < fogTrainP.size(); i++) {
                        entries.add(new Entry(i,fogTrainP.get(i)));
                    }
                    LineDataSet dataSet = new LineDataSet(entries, "Energy Consumption(J)");
                    LineData lineData = new LineData(dataSet);
                    Description description = new Description();
                    description.setText("Fog Training Energy");
                    chart2.setDescription(description);
                    chart2.setData(lineData);
                    chart2.invalidate();

                }
                if (remoteTestP.isEmpty()) {
                    sta3.setText("Remote Testing: You haven't done a remote server testing.");
                }
                else {
                    float sum = 0;
                    for (int i = 0; i < remoteTestP.size(); i++)
                        sum += remoteTestP.get(i);
                    sta3.setText("Remote Testing: " + Double.toString(remoteTestT) + "(sec), Energy: " + Float.toString(sum) + "(J)");
                    List<Entry> entries = new ArrayList<Entry>();
                    for (int i = 0; i < remoteTestP.size(); i++) {
                        entries.add(new Entry(i,remoteTestP.get(i)));
                    }
                    LineDataSet dataSet = new LineDataSet(entries, "Energy Consumption(J)");
                    LineData lineData = new LineData(dataSet);
                    Description description = new Description();
                    description.setText("Remote Testing Energy");
                    chart3.setDescription(description);
                    chart3.setData(lineData);
                    chart3.invalidate();

                }
                if (fogTestP.isEmpty()) {
                    sta4.setText("Fog Testing: You haven't done a fog server testing.");
                }
                else {
                    float sum = 0;
                    for (int i = 0; i < fogTestP.size(); i++)
                        sum += fogTestP.get(i);
                    sta4.setText("Fog Testing: " + Double.toString(fogTestT) + "(sec), Energy: " + Float.toString(sum) + "(J)");
                    List<Entry> entries = new ArrayList<Entry>();
                    for (int i = 0; i < fogTestP.size(); i++) {
                        entries.add(new Entry(i,fogTestP.get(i)));
                    }
                    LineDataSet dataSet = new LineDataSet(entries, "Energy Consumption(J)");
                    LineData lineData = new LineData(dataSet);
                    Description description = new Description();
                    description.setText("Fog Testing Energy");
                    chart4.setDescription(description);
                    chart4.setData(lineData);
                    chart4.invalidate();

                }

            }
        });
    }
}
