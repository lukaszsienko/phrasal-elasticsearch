package phrasalelastic.text2jsonconverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Translator {

    private String translationSystemAddress;
    private int translationSystemPortNumber;

    private Socket socket;
    private PrintWriter outputToTranslate;
    private BufferedReader inputWithTranslation;

    public Translator(String translationSystemAddress, int translationSystemPortNumber) {
       this.translationSystemAddress = translationSystemAddress;
       this.translationSystemPortNumber = translationSystemPortNumber;
       initializeConnection();
    }

    private void initializeConnection() {
        try {
            socket = new Socket(translationSystemAddress, translationSystemPortNumber);
            socket.setKeepAlive(true);
            outputToTranslate = new PrintWriter(socket.getOutputStream(), true);
            inputWithTranslation = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> translate(List<String> input) {
        List<String> translation = new ArrayList<>();
        boolean retryTranslation = false;
        do {
            try {
                translation = tryToTranslate(input);
                retryTranslation = false;
            } catch(Exception e) {
                initializeConnection();
                retryTranslation = true;
            }

        } while(retryTranslation);

        return translation;
    }

    private List<String> tryToTranslate(List<String> input) throws IOException {
        List<String> translatedSentences = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            String nextInputSentence = input.get(i);
            outputToTranslate.println(nextInputSentence);
            String translation = inputWithTranslation.readLine();
            if (i == 0 && translation == null) {
                throw new IOException("Connection timeout.");
            }
            translatedSentences.add(translation.replace("\n", "").replace("\r", ""));
        }

        return translatedSentences;
    }
}
