package phrasalelastic.text2jsonconverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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

    public String translateFromPolishToEnglish(String polishInput) {
        String translation = "";
        boolean retryTranslation = false;
        do {
            try {
                translation = tryToTranslate(polishInput);
                retryTranslation = false;
            } catch(Exception e) {
                initializeConnection();
                retryTranslation = true;
            }

        } while(retryTranslation);

        return translation;
    }

    private String tryToTranslate(String input) throws IOException {
        StringBuilder sb = new StringBuilder();
        outputToTranslate.write(input);
        String line;
        while ((line = inputWithTranslation.readLine()) != null) {
            sb.append(line+"\n");
        }
        return sb.toString();
    }
}
