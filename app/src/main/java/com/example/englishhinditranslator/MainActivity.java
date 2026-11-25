package com.example.englishhinditranslator;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_AUDIO = 101;
    private EditText inputText;
    private TextView outputText, directionText, statusText;
    private Button speakInputButton, translateButton, playOutputButton, swapButton;
    private boolean englishToHindi = true;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private Translator translator;
    private boolean ttsReady = false;
    private boolean listening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        tts = new TextToSpeech(this, this);
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new SimpleRecognitionListener(
                text -> {
                    inputText.setText(text);
                    inputText.setSelection(inputText.length());
                    listening = false;
                    speakInputButton.setText("🎙  Speak");
                    statusText.setText("Voice captured. Tap Translate.");
                },
                error -> {
                    listening = false;
                    speakInputButton.setText("🎙  Speak");
                    statusText.setText("Voice input error. Please try again.");
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            ));
        } else {
            speakInputButton.setEnabled(false);
            statusText.setText("Speech recognition service is not available on this device.");
        }
        updateDirection();
    }

    private void buildScreen() {
        getWindow().setStatusBarColor(Color.rgb(19, 35, 69));
        getWindow().setNavigationBarColor(Color.rgb(19, 35, 69));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(24));
        page.setBackgroundColor(Color.rgb(246, 248, 252));
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("Voice Translator");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(25, 43, 77));
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("English  ⇄  हिन्दी");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(91, 105, 128));
        subtitle.setPadding(0, dp(5), 0, dp(18));
        page.addView(subtitle);

        directionText = new TextView(this);
        directionText.setTextSize(17);
        directionText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        directionText.setTextColor(Color.WHITE);
        directionText.setGravity(Gravity.CENTER);
        directionText.setPadding(dp(12), dp(14), dp(12), dp(14));
        directionText.setBackgroundColor(Color.rgb(36, 86, 180));
        page.addView(directionText, matchWrap());

        swapButton = makeButton("⇄  Swap languages");
        swapButton.setOnClickListener(v -> {
            englishToHindi = !englishToHindi;
            updateDirection();
            outputText.setText("Your translation will appear here.");
            statusText.setText("Language direction changed.");
        });
        addGap(page, 12);
        page.addView(swapButton, matchWrap());

        page.addView(sectionLabel("Speak or type"));
        inputText = new EditText(this);
        inputText.setTextSize(18);
        inputText.setGravity(Gravity.TOP | Gravity.START);
        inputText.setMinLines(4);
        inputText.setPadding(dp(14), dp(12), dp(14), dp(12));
        inputText.setHint("Tap Speak or type your text here...");
        inputText.setBackgroundColor(Color.WHITE);
        page.addView(inputText, matchWrap());

        speakInputButton = makeButton("🎙  Speak");
        speakInputButton.setOnClickListener(v -> startVoiceInput());
        addGap(page, 10);
        page.addView(speakInputButton, matchWrap());

        translateButton = makeButton("Translate");
        translateButton.setTextColor(Color.WHITE);
        translateButton.setBackgroundColor(Color.rgb(20, 145, 108));
        translateButton.setOnClickListener(v -> translateText());
        addGap(page, 12);
        page.addView(translateButton, matchWrap());

        page.addView(sectionLabel("Translation"));
        outputText = new TextView(this);
        outputText.setText("Your translation will appear here.");
        outputText.setTextSize(20);
        outputText.setTextColor(Color.rgb(30, 43, 65));
        outputText.setMinHeight(dp(110));
        outputText.setGravity(Gravity.TOP | Gravity.START);
        outputText.setPadding(dp(14), dp(16), dp(14), dp(16));
        outputText.setBackgroundColor(Color.WHITE);
        page.addView(outputText, matchWrap());

        playOutputButton = makeButton("🔊  Play translation");
        playOutputButton.setOnClickListener(v -> playTranslation());
        addGap(page, 10);
        page.addView(playOutputButton, matchWrap());

        statusText = new TextView(this);
        statusText.setText("Ready");
        statusText.setTextSize(13);
        statusText.setTextColor(Color.rgb(91, 105, 128));
        statusText.setPadding(2, dp(16), 2, dp(8));
        page.addView(statusText, matchWrap());

        TextView note = new TextView(this);
        note.setText("First translation may take a little longer while the language model downloads. Internet is needed for the initial model download.");
        note.setTextSize(12);
        note.setTextColor(Color.rgb(110, 119, 135));
        page.addView(note, matchWrap());

        setContentView(scroll);
    }

    private TextView sectionLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextColor(Color.rgb(42, 57, 82));
        label.setPadding(2, dp(18), 2, dp(8));
        return label;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setPadding(dp(12), dp(9), dp(12), dp(9));
        return b;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void addGap(LinearLayout parent, int heightDp) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void updateDirection() {
        directionText.setText(englishToHindi ? "English  →  हिन्दी" : "हिन्दी  →  English");
        inputText.setHint(englishToHindi ? "Speak or type English here..." : "यहाँ हिन्दी बोलें या लिखें...");
    }

    private void startVoiceInput() {
        if (speechRecognizer == null) {
            Toast.makeText(this, "Speech recognition is unavailable.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (listening) {
            speechRecognizer.stopListening();
            listening = false;
            speakInputButton.setText("🎙  Speak");
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, englishToHindi ? "en-IN" : "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, englishToHindi ? "en-IN" : "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, englishToHindi ? "Speak in English" : "हिन्दी में बोलें");
        listening = true;
        speakInputButton.setText("Listening… tap to stop");
        statusText.setText("Listening for " + (englishToHindi ? "English" : "Hindi") + " speech…");
        speechRecognizer.startListening(intent);
    }

    private void translateText() {
        String source = inputText.getText().toString().trim();
        if (source.isEmpty()) {
            inputText.setError("Please speak or enter text first.");
            return;
        }
        hideKeyboard();
        translateButton.setEnabled(false);
        statusText.setText("Preparing translation…");

        TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(englishToHindi ? TranslateLanguage.ENGLISH : TranslateLanguage.HINDI)
            .setTargetLanguage(englishToHindi ? TranslateLanguage.HINDI : TranslateLanguage.ENGLISH)
            .build();

        if (translator != null) translator.close();
        translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(unused -> translator.translate(source)
                .addOnSuccessListener(result -> {
                    outputText.setText(result);
                    statusText.setText("Translation complete.");
                    translateButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    statusText.setText("Translation failed: " + e.getLocalizedMessage());
                    translateButton.setEnabled(true);
                }))
            .addOnFailureListener(e -> {
                statusText.setText("Could not download language model. Check internet and try again.");
                Toast.makeText(this, "Language model download failed.", Toast.LENGTH_LONG).show();
                translateButton.setEnabled(true);
            });
    }

    private void playTranslation() {
        String text = outputText.getText().toString().trim();
        if (text.isEmpty() || text.equals("Your translation will appear here.")) {
            Toast.makeText(this, "Translate some text first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ttsReady) {
            Toast.makeText(this, "Speech engine is still starting.", Toast.LENGTH_SHORT).show();
            return;
        }
        Locale locale = englishToHindi ? new Locale("hi", "IN") : new Locale("en", "IN");
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "This voice is not installed. Install Hindi/English voice data in Android settings.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "translator_output");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
            inputText.clearFocus();
        } catch (Exception ignored) {}
    }

    @Override
    public void onInit(int status) {
        ttsReady = status == TextToSpeech.SUCCESS;
        if (!ttsReady) statusText.setText("Text-to-speech engine could not start.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice input.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (translator != null) {
            translator.close();
            translator = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private interface VoiceResult {
        void onResult(String text);
    }

    private interface VoiceError {
        void onError(String message);
    }

    private static class SimpleRecognitionListener implements android.speech.RecognitionListener {
        private final VoiceResult result;
        private final VoiceError error;
        SimpleRecognitionListener(VoiceResult result, VoiceError error) {
            this.result = result;
            this.error = error;
        }
        @Override public void onReadyForSpeech(Bundle params) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onError(int errorCode) {
            String message = "Speech recognition error (" + errorCode + ")";
            error.onError(message);
        }
        @Override public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) result.onResult(matches.get(0));
            else error.onError("No speech detected.");
        }
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    }
}
