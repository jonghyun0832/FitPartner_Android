package com.example.fitpartner;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Activity_Chart extends AppCompatActivity {

    private LineChart chart_body;
    private Button btn_popup;
    private TextView tv_fitSteps, tv_fitCalories;

    long now = System.currentTimeMillis();
    Date mDate = new Date(now);
    SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
    String dateData = dateformat.format(mDate);

    private final String FileName = dateData;

    private final String mainData = "MainData";

    ArrayList<BodyData> bodyArrayList;

    private int dataSize = 7; //차트 기본 데이터 갯수

    int stepTotal; //스텝수
    double rounfoffCalories = 0.0 ;

    private boolean isActivate = true;

    CircleProgressBar circleProgressBar;

    private static final int REQUEST_OAUTH = 1;
    private final String FitData = "FitData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });

        //차트 등록
        chart_body = (LineChart)findViewById(R.id.linechart_body);
        btn_popup = (Button)findViewById(R.id.button_popupbody);
        tv_fitSteps = (TextView)findViewById(R.id.textView_fitSteps);
        tv_fitCalories = (TextView)findViewById(R.id.textView_fitCalories);
        circleProgressBar = (CircleProgressBar) findViewById(R.id.cpb_circlebar_step);



        // 구글 연결
        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(this, fitnessOptions);

        if (!GoogleSignIn.hasPermissions(googleSignInAccount, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    REQUEST_OAUTH, // e.g. 1
                    googleSignInAccount,
                    fitnessOptions);
        } else {
            if (isActivate == true){
                readDataSteps(googleSignInAccount); //걸음수 가져오기
                readDataCalories(); //칼로리가져오기
                isActivate = false;
            }


        }






        btn_popup.setText("최근 7개");
        btn_popup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(getApplicationContext(),view);
                getMenuInflater().inflate(R.menu.popup,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if(menuItem.getItemId() == R.id.action_menu1){
                            dataSize = 4;

                        } else if (menuItem.getItemId() == R.id.action_menu2){
                            dataSize = 7;

                        } else {
                            dataSize = 14;
                        }
                        setChart_body(dataSize);
                        chart_body.invalidate();
                        btn_popup.setText("최근 " + dataSize + "개");
                        return false;
                    }
                });
                popupMenu.show();
            }
        });


        //차트 설정
        chart_body.setBackgroundColor(ContextCompat.getColor(this,R.color.background));
        chart_body.setDrawGridBackground(false);
        chart_body.setDescription(null);

        //차트그리기
        setChart_body(dataSize);

    }

    private void setChart_body(int dataSize){

        ArrayList<Entry> entries_weight = new ArrayList<>();
        ArrayList<Entry> entries_fat = new ArrayList<>();
        ArrayList<Entry> entries_protein = new ArrayList<>();
        ArrayList<String> xlabel_title = new ArrayList<>();

        Log.d("111", "setChart_body: ");

        //데이터 받아오기 - 신체데이터
        bodyArrayList = new ArrayList<>();
        SharedPreferences sharedPreferences = this.getSharedPreferences(mainData, Context.MODE_PRIVATE);
        if (sharedPreferences.contains("Bodylog")) {
            Gson gson = new GsonBuilder().create();
            String json = sharedPreferences.getString("Bodylog", "");
            Type bodylogType = new TypeToken<ArrayList<BodyData>>() {
            }.getType();
            bodyArrayList = gson.fromJson(json, bodylogType);
        }

        //리스트에서 필요한 값들만 빼오기
        ArrayList<ArrayList<String>> arrangeList = new ArrayList<>(); //정리된 데이터 저장용 리스트
        for (int i = 0; i < bodyArrayList.size(); i++){
            String date = bodyArrayList.get(i).getCreateDate();
            String [] tmparray = bodyArrayList.get(i).getTv_totalWeight().split(" ");
            String weight = tmparray[2];//몸무게 가져오기
            tmparray = bodyArrayList.get(i).getTv_fatRate().split(" "); //ex) 체지방량 : 24kg
            String fat = tmparray[2]; //지방량 가져오기
            tmparray = bodyArrayList.get(i).getTv_proteinRate().split(" ");
            String protein = tmparray[2]; //근육량 가져오기
            ArrayList<String> tmpList  = new ArrayList<String>();

            tmpList.add(date);
            tmpList.add(weight);
            tmpList.add(fat);
            tmpList.add(protein);

            arrangeList.add(tmpList);
        }

        //받아온 리스트를 이용해서 같은날이면 평균내서 넣어주기
        ArrayList<ArrayList<String>> cleanarr = new ArrayList<>();
        ArrayList<ArrayList<String>> tmparr = new ArrayList<>();
        for (int i = 0; i < arrangeList.size(); i++){
            if (i != arrangeList.size()-1){
                if (arrangeList.get(i).get(0).equals(arrangeList.get(i+1).get(0))){
                    //같을떄니까 따로 저장했다가 다시저장
                    tmparr.add(arrangeList.get(i));
                }
                else {
                    tmparr.add(arrangeList.get(i));
                    int tmpWeight = 0;
                    int tmpFat = 0;
                    int tmpProtein = 0;
                    for (int j = 0; j < tmparr.size(); j++){
                        tmpWeight += Integer.parseInt(tmparr.get(j).get(1));
                        tmpFat += Integer.parseInt(tmparr.get(j).get(2));
                        tmpProtein += Integer.parseInt(tmparr.get(j).get(3));
                    }
                    String totalWeight = Integer.toString(tmpWeight/tmparr.size());
                    String totalFat = Integer.toString(tmpFat/tmparr.size());
                    String totalProtein = Integer.toString(tmpProtein/tmparr.size());
                    tmparr.get(0).set(1,totalWeight);
                    tmparr.get(0).set(2,totalFat);
                    tmparr.get(0).set(3,totalProtein);
                    cleanarr.add(tmparr.get(0));
                    tmparr.clear();
                }
            }
            else {
                //i가 마지막 값일떄
                tmparr.add(arrangeList.get(i));
                int tmpWeight = 0;
                int tmpFat = 0;
                int tmpProtein = 0;
                for (int j = 0; j < tmparr.size(); j++){
                    tmpWeight += Integer.parseInt(tmparr.get(j).get(1));
                    tmpFat += Integer.parseInt(tmparr.get(j).get(2));
                    tmpProtein += Integer.parseInt(tmparr.get(j).get(3));
                }
                String totalWeight = Integer.toString(tmpWeight/tmparr.size());
                String totalFat = Integer.toString(tmpFat/tmparr.size());
                String totalProtein = Integer.toString(tmpProtein/tmparr.size());
                tmparr.get(0).set(1,totalWeight);
                tmparr.get(0).set(2,totalFat);
                tmparr.get(0).set(3,totalProtein);
                cleanarr.add(tmparr.get(0));
                tmparr.clear();
            }
        }

        List<ArrayList<String>> cleanarr_cut;

        if (dataSize < cleanarr.size()){
            cleanarr_cut = cleanarr.subList(cleanarr.size()-dataSize-1,cleanarr.size()-1);
        } else {
            cleanarr_cut = cleanarr;
        }

        //순서 뒤집에서 넣어주기
        for (int i = cleanarr_cut.size() - 1; i >= 0; i--){
            String date = cleanarr.get(i).get(0);
            int weight = Integer.parseInt(cleanarr.get(i).get(1));//몸무게 가져오기
            int fat = Integer.parseInt(cleanarr.get(i).get(2)); //지방량 가져오기
            int protein = Integer.parseInt(cleanarr.get(i).get(3)); //근육량 가져오기

            //차트에 들어갈 데이터 설정
            entries_weight.add(new Entry(cleanarr.size()-i-1,weight));
            entries_fat.add(new Entry(cleanarr.size()-i-1,fat));
            entries_protein.add(new Entry(cleanarr.size()-i-1,protein));
            xlabel_title.add(" ");
        }

        //몸무게 엔트리
        LineDataSet lineDataSet_weight = new LineDataSet(entries_weight, "몸무게");
        lineDataSet_weight.setLineWidth(2); // 선 굵기
        lineDataSet_weight.setCircleRadius(6); // 곡률

        lineDataSet_weight.setCircleColor(ContextCompat.getColor(this, R.color.teal_200)); // LineChart에서 Line Circle Color 설정
        lineDataSet_weight.setCircleHoleColor(ContextCompat.getColor(this, R.color.teal_200)); // LineChart에서 Line Hole Circle Color 설정
        lineDataSet_weight.setColor(ContextCompat.getColor(this, R.color.teal_200)); // LineChart에서 Line Color 설정


        //체지방 엔트리
        LineDataSet lineDataSet_fat = new LineDataSet(entries_fat, "체지방량");
        lineDataSet_fat.setLineWidth(2); // 선 굵기
        lineDataSet_fat.setCircleRadius(6); // 곡률

        lineDataSet_fat.setCircleColor(ContextCompat.getColor(this, R.color.graphColor)); // LineChart에서 Line Circle Color 설정
        lineDataSet_fat.setCircleHoleColor(ContextCompat.getColor(this, R.color.graphColor)); // LineChart에서 Line Hole Circle Color 설정
        lineDataSet_fat.setColor(ContextCompat.getColor(this, R.color.graphColor)); // LineChart에서 Line Color 설정

        //단백질 엔트리
        LineDataSet lineDataSet_protein = new LineDataSet(entries_protein, "근골격량");
        lineDataSet_protein.setLineWidth(2); // 선 굵기
        lineDataSet_protein.setCircleRadius(6); // 곡률

        lineDataSet_protein.setCircleColor(ContextCompat.getColor(this, R.color.purple_500)); // LineChart에서 Line Circle Color 설정
        lineDataSet_protein.setCircleHoleColor(ContextCompat.getColor(this, R.color.purple_500)); // LineChart에서 Line Hole Circle Color 설정
        lineDataSet_protein.setColor(ContextCompat.getColor(this, R.color.purple_500)); // LineChart에서 Line Color 설정


        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet_weight);
        dataSets.add(lineDataSet_fat);
        dataSets.add(lineDataSet_protein);

        //x축 설정
        XAxis xAxis = chart_body.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //아래쪽으로
        xAxis.setTextColor(R.color.black); //x축 텍스트 컬러
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xlabel_title));
        //xAxis.setLabelCount(5, true);

        // y축 설정
        YAxis yLAxis = chart_body.getAxisLeft();
        yLAxis.setTextColor(R.color.black); // y축 텍스트 컬러

        // y축 오른쪽 비활성화
        YAxis yRAxis = chart_body.getAxisRight();
        yRAxis.setDrawLabels(false);
        yRAxis.setDrawAxisLine(false);
        yRAxis.setDrawGridLines(false);


        LineData lineData = new LineData(dataSets);
        chart_body.setData(lineData);

    }

    //걸음수 받아오기
    public void readDataSteps(GoogleSignInAccount googleSignInAccount) {
        Fitness.getHistoryClient(this, googleSignInAccount)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                    @Override
                    public void onSuccess(DataSet dataSet) {

                        if (dataSet.isEmpty()) {
                            stepTotal = 0;
                        } else {
                            stepTotal = dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            tv_fitSteps.setText(String.valueOf(stepTotal + "걸음"));

                            //걸음수 진행률 표시
                            circleProgressBar.setProgress((stepTotal*100)/10000); //목표 걸음수 추가 요망
                            try {
                                saveDatas();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Activity_Chart.this, "FailStep", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    //소모한 칼로리 받아오기
    private void readDataCalories() {
        // 칼로리 받아오기
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
                .addOnSuccessListener(new OnSuccessListener<DataSet>() { //스레드 돌고 완료됬을때
                    @Override
                    public void onSuccess(DataSet dataSet) {
                        double CaloriesBurn;
                        rounfoffCalories = 0.0;
                        if(dataSet.isEmpty()){
                            CaloriesBurn = 0.0;
                        }
                        else {
                            CaloriesBurn = dataSet.getDataPoints().get(0).getValue(Field.FIELD_CALORIES).asFloat();
                            rounfoffCalories = Math.round(CaloriesBurn * 100.0) / 100.0;
                        }
                        //Toast.makeText(Activity_Chart.this, "Calories  " + rounfoffCalories, Toast.LENGTH_SHORT).show();
                        tv_fitCalories.setText(rounfoffCalories+" Kcal");
                        try {
                            saveDatas();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Toast.makeText(Activity_Chart.this, "Unabel to Calculate c", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void saveDatas() throws JSONException {

        Log.d("111", "saveDatas: " + stepTotal + "  " + rounfoffCalories);
        SharedPreferences sharedPreferences = this.getSharedPreferences(FitData, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONObject wholedata = new JSONObject(); //최종
        JSONArray Jarray = new JSONArray();
        JSONObject Jdata = new JSONObject();
        Jdata.put("Step",stepTotal);
        Jdata.put("Calories",rounfoffCalories);
        Jarray.put(Jdata);
        wholedata.put(FileName,Jarray);
        editor.putString(FitData,wholedata.toString());
        editor.apply();

    }



}