package com.labs.robots.ttkbot;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.labs.robots.ttkbot.api.WeatherService;
import com.labs.robots.ttkbot.model.Model;
import com.labs.robots.ttkbot.service.ApiFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Button btn;
    private static final int VR_REQUEST=999;
    private LinearLayout linearLayout;
    private LinearLayout.LayoutParams lparams;
    private WeatherService weatherService;
    private ProgressBar progressBar;
    private TextToSpeech TTS;
    private boolean ttsEnabled;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);

        btn = (Button) findViewById(R.id.button);
        linearLayout = (LinearLayout) findViewById(R.id.linear);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        editText = (EditText) findViewById(R.id.editText);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PackageManager packManager= getPackageManager();
                List<ResolveInfo> intActivities= packManager.queryIntentActivities(new
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),0);
                if(intActivities.size()!=0){
                    listen();
                }
                else
                {
                    Toast.makeText(MainActivity.this,"Не поддерживается на вашем устройстве", Toast.LENGTH_LONG).show();
                }

            }
        });

        TTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override public void onInit(int initStatus) {
                if (initStatus == TextToSpeech.SUCCESS) {
                    if (TTS.isLanguageAvailable(new Locale(Locale.getDefault().getLanguage()))
                            == TextToSpeech.LANG_AVAILABLE) {
                        TTS.setLanguage(new Locale(Locale.getDefault().getLanguage()));
                    } else {
                        TTS.setLanguage(Locale.US);
                    }
                    TTS.setPitch(1.6f);
                    TTS.setSpeechRate(0.9f);
                    ttsEnabled = true;
                } else if (initStatus == TextToSpeech.ERROR) {
                    Toast.makeText(MainActivity.this, "Erroe", Toast.LENGTH_LONG).show();
                    ttsEnabled = false;
                }
            }
        });
    }


    private void listen() {
        Intent listenIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        listenIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getClass().getPackage().getName());
        listenIntent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say a word!");
        listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        startActivityForResult(listenIntent, VR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode== VR_REQUEST && resultCode== RESULT_OK)
        {

            String url = editText.getText().toString();
            ApiFactory.setApiBaseUrl("http://" + url + ".ngrok.io/");
            List<String> suggestedWords=
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            weatherService = ApiFactory.getRetrofitInstance().create(WeatherService.class);

            TextView textView = new TextView(MainActivity.this);
            textView.setLayoutParams(lparams);
            textView.setText("вы - \n" + suggestedWords.get(0));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            linearLayout.addView(textView);

            getAnswer(suggestedWords.get(0));


        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public String convertDate(Long dateInMilliseconds) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date (dateInMilliseconds*1000));
    }

    public void speak(String text) {
        if (!ttsEnabled) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsGreater21(text);
        } else {
            ttsUnder20(text);
        }
    }

    @SuppressWarnings("deprecation") private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        TTS.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void ttsGreater21(String text) {
        String utteranceId = this.hashCode() + "";
        TTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    private void getAnswer(final String q) {
        progressBar.setVisibility(View.VISIBLE);
        Call<Model> call = weatherService.getAnswer(q);

        call.enqueue(new Callback<Model>() {
            @Override
            public void onResponse(Call<Model> call, Response<Model> response) {
                progressBar.setVisibility(View.INVISIBLE);
                if (response.isSuccessful()) {
                    TextView textView = new TextView(MainActivity.this);
                    textView.setLayoutParams(lparams);
                    textView.setText("bot: \n" + response.body().getAnswer());
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    linearLayout.addView(textView);
                    speak(response.body().getAnswer());

                } else {
                    Toast.makeText(MainActivity.this, String.valueOf(response.code()), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Model> call, Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, "error" + t.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
