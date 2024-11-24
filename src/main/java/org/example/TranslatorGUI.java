package org.example;

import java.io.IOException;
import java.util.Objects;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import javax.swing.*;
import java.awt.*;

public class TranslatorGUI extends JFrame {

  private static final String OPENAI_API_KEY = "REPLACE-API-KEY";
  private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;

  private JTextArea inputTextArea;
  private JTextArea outputTextArea;
  private final Timer typingTimer;

  public TranslatorGUI() {
    client = new OkHttpClient();
    objectMapper = new ObjectMapper();

    setTitle("Korean to Spanish Translator");
    setSize(600, 500);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    JPanel mainPanel = createMainPanel();
    add(mainPanel, BorderLayout.CENTER);

    typingTimer = new Timer(1000, e -> handleTranslation());
    typingTimer.setRepeats(false);

    inputTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override
      public void insertUpdate(javax.swing.event.DocumentEvent e) {
        restartTimer();
      }

      @Override
      public void removeUpdate(javax.swing.event.DocumentEvent e) {
        restartTimer();
      }

      @Override
      public void changedUpdate(javax.swing.event.DocumentEvent e) {
        restartTimer();
      }
    });
  }

  private JPanel createMainPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    mainPanel.setBackground(Color.DARK_GRAY);

    inputTextArea = createTextArea("Input Text (in Korean)");
    mainPanel.add(new JScrollPane(inputTextArea));

    outputTextArea = createTextArea("Translated Text");
    outputTextArea.setEditable(false);
    mainPanel.add(new JScrollPane(outputTextArea));

    return mainPanel;
  }

  private JTextArea createTextArea(String title) {
    JTextArea textArea = new JTextArea(8, 40);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setFont(new Font("Arial", Font.PLAIN, 14));
    textArea.setBackground(Color.DARK_GRAY);
    textArea.setForeground(Color.WHITE);
    textArea.setCaretColor(Color.WHITE);
    textArea.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(Color.GRAY), title,
        0, 0, new Font("Arial", Font.PLAIN, 12), Color.WHITE));
    return textArea;
  }

  private void restartTimer() {
    typingTimer.restart();
  }

  private void handleTranslation() {
    String textToTranslate = inputTextArea.getText().trim();
    if (!textToTranslate.isEmpty()) {
      try {
        String improvedTranslation = translateAndImprove(textToTranslate);
        outputTextArea.setText(improvedTranslation);
        FileWriterUtil.saveTranslation(improvedTranslation);
      } catch (IOException e) {
        outputTextArea.setText("Error translating text.");
      }
    }
  }

  public String translateAndImprove(String text) throws IOException {
    String translatedText = requestTranslation(text, "Translate the following text from Korean to Spanish: %s");
    return requestTranslation(translatedText, "Improve the following translation to sound more natural in Spanish: %s");
  }

  private String requestTranslation(String text, String promptFormat) throws IOException {
    String prompt = String.format(promptFormat, text);

    RequestBody body = createRequestBody(prompt);
    Request request = new Request.Builder()
        .url(OPENAI_API_URL)
        .post(body)
        .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
      return parseResponse(Objects.requireNonNull(response.body()).string());
    }
  }

  private RequestBody createRequestBody(String prompt) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("model", "gpt-4");

    JSONArray messages = new JSONArray();
    messages.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant for translating and improving translations between Korean and Spanish."));
    messages.put(new JSONObject().put("role", "user").put("content", prompt));

    jsonObject.put("messages", messages);
    jsonObject.put("temperature", 0.3);

    String json = jsonObject.toString();
    return RequestBody.create(json, MediaType.parse("application/json"));
  }

  private String parseResponse(String jsonResponse) throws IOException {
    JsonNode rootNode = objectMapper.readTree(jsonResponse);
    return rootNode.path("choices").get(0).path("message").path("content").asText().trim();
  }

}