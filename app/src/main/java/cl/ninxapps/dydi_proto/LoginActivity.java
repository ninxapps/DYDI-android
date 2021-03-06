package cl.ninxapps.dydi_proto;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Handler;
import com.github.kittinunf.fuel.core.Manager;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.Pair;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    private ProgressDialog progressDialog;

    @BindView(R.id.input_email) EditText _emailText;
    @BindView(R.id.input_password) EditText _passwordText;
    @BindView(R.id.btn_login) Button _loginButton;
    @BindView(R.id.link_signup)
    TextView _signupLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed(null);
            return;
        }

        _loginButton.setEnabled(false);

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        // TODO: Implement your own authentication logic here
        doLogin(email, password, getApplicationContext());
    }

    public void doLogin(final String email, final String password, final Context context){

        final MainApp app = (MainApp)getApplication();
        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>() {{
            add(new Pair<>("email", email));
            add(new Pair<>("password", password));
        }};

        boolean success = false;
        Fuel.post(GlobalConstants.API+"/auth/sign_in", params).responseString(new Handler<String>() {
            @Override
            public void failure(Request request, Response response, FuelError error) {
                onLoginFailed(error.toString());
            }

            @Override
            public void success(Request request, Response response, String data) {
                Log.e("FUEL", data.toString());
                Map<String, List<String>> allHeaders = response.getHttpResponseHeaders();


                try {
                    String user_email = email;
                    String access_token = allHeaders.get("access-token").get(0);
                    String client = allHeaders.get("client").get(0);

                    //Parse JSON
                    JSONObject jObject = new JSONObject(data);
                    jObject = jObject.getJSONObject("data");

                    //Get data
                    String name = jObject.getString("name");
                    String uid = jObject.getString("uid");
                    String provider = jObject.getString("provider");

                    app.onUserDataLoaded(provider, uid, name, email, access_token, client);
                }
                catch(JSONException e){
                    Log.e("FUEL", e.toString());
                    onLoginFailed("Error al contactar al servidor");
                    return;
                }


                //Make the headers for future http requests
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("uid",  app.getUID());
                headers.put("client", app.getClient());
                headers.put("access-token", app.getAccessToken());
                Manager.Companion.getInstance().setBaseHeaders(headers);
                onLoginSuccess();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                // By default we just finish the Activity and log them in automatically
                if(this.progressDialog != null)
                    this.progressDialog.dismiss();
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);
        if(this.progressDialog != null)
            this.progressDialog.dismiss();
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }

    public void onLoginFailed(String error) {
        if(this.progressDialog != null)
            this.progressDialog.dismiss();

        if(error != null)
            Toast.makeText(getBaseContext(), error, Toast.LENGTH_LONG).show();
        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }
}