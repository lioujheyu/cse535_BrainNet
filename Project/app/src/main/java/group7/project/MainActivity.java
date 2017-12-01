package group7.project;

import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {

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
    private Button registerbutton, loginbutton;
    private RadioGroup radioGroup;
    private String serverchoose;
    public int serverType;
    public ArrayList<Integer> registeredUser, TP, FP, FN, TN;
    public long startTime, stopTime;
    public AlertDialog.Builder msgBox;

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
                            if (conn.testFile(db_path+"/S001/", "S001R01.edf") == 0) {
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
    }

}
