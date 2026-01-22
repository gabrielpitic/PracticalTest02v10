package ro.pub.cs.systems.eim.practicaltest02v10;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

public class PracticalTest02v10MainActivity extends AppCompatActivity {

    private EditText pokemonName, port;
    private TextView serverResponse;
    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_practical_test02v10_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pokemonName = findViewById(R.id.pokemonName);
        port = findViewById(R.id.port);
        serverResponse = findViewById(R.id.serverRes);

        imageView = findViewById(R.id.image);

        Button sendReq = findViewById(R.id.sendReqBtn);
        sendReq.setOnClickListener(v -> {
            Server server = new Server(Integer.parseInt(port.getText().toString()));
            server.start();

            Client client = new Client(pokemonName.getText().toString(), Integer.parseInt(port.getText().toString()));
            client.start();
        });
    }

    class Server {

        private Integer port;

        public Server(Integer port) {
            this.port = port;
        }

        public void start() {
            new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    Log.d("[SERVER]", "Connected: " + serverSocket.getInetAddress() + " " +
                            serverSocket.getLocalPort());
                    while (true) {
                        Socket socket = serverSocket.accept();
                        new Thread(() -> handleClient(socket)).start();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        private void handleClient(Socket socket) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                String request = in.readLine();
                Log.d("[SERVER]", request);

                String response = callAPI(request);

                String jsonStrType = new JSONObject(response)
                        .getJSONArray("types")
                        .getJSONObject(0)
                        .getJSONObject("type")
                        .getString("name");

                String jsonAbilities = new JSONObject(response)
                        .getJSONArray("abilities")
                        .getJSONObject(0)
                        .getJSONObject("ability")
                        .getString("name");

                String sendRes = "Type is " + jsonStrType;
                String sendRes2 = "First ability is " + jsonAbilities;

                String imageUri = new JSONObject(response)
                        .getJSONObject("sprites")
                        .getString("front_default");

                out.println(sendRes);
                out.println(sendRes2);
                out.println(imageUri);

                Log.d("[Server]", sendRes + sendRes2 + imageUri);

                socket.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        private String callAPI(String request) {
            StringBuilder stringBuilder = new StringBuilder();
            try {
                URL url = new URL("https://pokeapi.co/api/v2/pokemon/" + request);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new
                        InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                reader.close();
                connection.disconnect();
                return stringBuilder.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    class Client {

        String pokemonName;
        Integer port;

        public Client(String name, Integer port) {
            this.pokemonName = name;
            this.port = port;
        }

        public void start() {
            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", port);
                    Log.d("[CLIENT]", "Connected to " + socket.getInetAddress() + " " +
                            socket.getLocalPort() + " " + socket.getPort());

                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(pokemonName);

                    BufferedReader reader = new BufferedReader(new
                            InputStreamReader(socket.getInputStream()));

                    String responseType = reader.readLine();
                    String firstAbility = reader.readLine();
                    String imageUri = reader.readLine();

                    String response = responseType + "\n" + firstAbility + "\n" + imageUri;

                    PracticalTest02v10MainActivity.this.runOnUiThread(() -> {
                        serverResponse.setText(response);

                        new DownloadImageTask((ImageView) findViewById(R.id.image))
                                .execute(imageUri);
                    });

                    socket.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}