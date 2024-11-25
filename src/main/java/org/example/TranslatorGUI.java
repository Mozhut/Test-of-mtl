package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

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
    textArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
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
      } catch (IOException e) {
        outputTextArea.setText("Error translating text.");
      }
    }
  }

  public String translateAndImprove(String text) throws IOException {
    String[] paragraphs = text.split("\n\n");
    StringBuilder fullTranslation = new StringBuilder();

    for (String paragraph : paragraphs) {
      System.out.println("Texto: " + paragraph);
      String translatedParagraph = requestTranslation(paragraph, "Translate the following text from Korean to Spanish: %s", true);
      String improvedParagraph = requestTranslation(translatedParagraph, "Improve the following translation to sound more natural in Spanish: %s", false);
      fullTranslation.append(improvedParagraph).append("\n\n");
      FileWriterUtil.saveTranslation(improvedParagraph);
    }

    return fullTranslation.toString().trim();
  }

  private String requestTranslation(String text, String promptFormat, boolean isTranslation) throws IOException {
    String prompt = String.format(promptFormat, text);

    RequestBody body = createRequestBody(prompt, isTranslation);
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

  private RequestBody createRequestBody(String text, boolean isTranslation) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("model", "gpt-3.5-turbo");

    JSONArray messages = new JSONArray();
    messages.put(new JSONObject().put("role", "system").put("content", "You are a helpful assistant for translating and improving translations between Korean and Spanish."));

    String prompt;
    if (isTranslation) {
      prompt = String.format("Translate the following text from Korean to Spanish: %s", text);
    } else {
      prompt = String.format("Improve the following translation to sound more natural in Spanish: %s", text);
    }

    messages.put(new JSONObject().put("role", "user").put("content", prompt));

    jsonObject.put("messages", messages);
    jsonObject.put("temperature", 0.3);
    jsonObject.put("max_tokens", 4096);

    String json = jsonObject.toString();
    return RequestBody.create(json, MediaType.parse("application/json"));
  }


  private String parseResponse(String jsonResponse) throws IOException {
    JsonNode rootNode = objectMapper.readTree(jsonResponse);
    return rootNode.path("choices").get(0).path("message").path("content").asText().trim();
  }
}
