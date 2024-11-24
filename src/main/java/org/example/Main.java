package org.example;

import javax.swing.SwingUtilities;

public class Main {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      TranslatorGUI translatorGUI = new TranslatorGUI();
      translatorGUI.setVisible(true);
    });
  }}